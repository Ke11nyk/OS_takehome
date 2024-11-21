package os.takehome.taskmanager;

import os.takehome.component.Component;
import os.takehome.component.ComponentFactory;
import os.takehome.component.ComponentGroup;
import os.takehome.component.ComponentStatus;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class CommandProcessor {
    private final ComponentGroupManager groupManager;
    private final ServerManager serverManager;
    private final NotificationManager notificationManager;

    public CommandProcessor(ComponentGroupManager groupManager, ServerManager serverManager, NotificationManager notificationManager) {
        this.groupManager = groupManager;
        this.serverManager = serverManager;
        this.notificationManager = notificationManager;
    }

    public void processCommand(String command) {
        String[] parts = command.trim().split("\\s+");
        try {
            switch (parts[0].toLowerCase()) {
                case "group":
                    if (parts.length < 2) throw new IllegalArgumentException("Usage: group <index> [limit <time in seconds>]");
                    int groupIndex = Integer.parseInt(parts[1]);
                    Integer timeLimit = (parts.length == 4 && parts[2].equalsIgnoreCase("limit")) ? Integer.parseInt(parts[3]) : null;
                    handleGroupCommand(groupIndex, timeLimit);
                    break;
                case "new":
                    if (parts.length != 2) throw new IllegalArgumentException("Usage: new <component symbol>");
                    handleNewCommand(parts[1].charAt(0));
                    break;
                case "run":
                    int argument = parts.length > 1 ? Integer.parseInt(parts[1]) : 5;
                    handleRunCommand(argument);
                    break;
                case "status":
                    if (parts.length != 2) throw new IllegalArgumentException("Usage: status <component index>");
                    handleStatusCommand(Integer.parseInt(parts[1]));
                    break;
                case "summary":
                    handleSummaryCommand();
                    break;
                case "interactive":
                    handleInteractiveCommand();
                    break;
                default:
                    System.out.println("Unknown command: " + parts[0]);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void handleGroupCommand(int index, Integer timeLimit) {
        groupManager.createOrSwitchGroup(index);

        if (timeLimit != null) {
            ComponentGroup currentGroup = groupManager.getCurrentGroup();
            currentGroup.setTimeLimit(timeLimit);
            System.out.println("Set time limit of " + timeLimit + " seconds for group " + currentGroup.getIndex());
        }
    }

    private void handleNewCommand(char symbol) throws IOException {
        ComponentGroup currentGroup = groupManager.getCurrentGroup();
        if (!ComponentFactory.isValidSymbol(symbol)) {
            throw new IllegalArgumentException("Invalid component symbol: " + symbol);
        }

        int componentIndex = currentGroup.getComponents().size();
        Socket socket = createComponentSocket(componentIndex);

        Component component = new Component(componentIndex, symbol, socket);
        groupManager.addComponent(component);

        System.out.println("Created component " + componentIndex + " with symbol " + symbol);
    }

    private void handleSetLimitCommand(String target, int timeLimit) {
        if (target.equalsIgnoreCase("group")) {
            ComponentGroup currentGroup = groupManager.getCurrentGroup();
            currentGroup.setTimeLimit(timeLimit);
            System.out.println("Set time limit of " + timeLimit + " seconds for group " + currentGroup.getIndex());
        } else {
            int componentIndex = Integer.parseInt(target);
            Component component = groupManager.getCurrentGroup().getComponents().get(componentIndex);
            if (component == null) throw new IllegalArgumentException("Component not found: " + componentIndex);
            component.setTimeLimit(timeLimit);
            System.out.println("Set time limit of " + timeLimit + " seconds for component " + componentIndex);
        }
    }


    private Socket createComponentSocket(int componentIndex) throws IOException {
        int maxRetries = 3;
        int retryCount = 0;
        int retryDelayMs = 1000;

        while (retryCount < maxRetries) {
            try {
                return new Socket("localhost", ServerManager.getPortStart() + componentIndex);
            } catch (ConnectException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw new IOException("Failed to connect after " + maxRetries + " attempts");
                }
                System.out.println("Connection attempt " + retryCount + " failed, retrying in " + retryDelayMs + "ms...");
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Connection interrupted");
                }
            }
        }
        throw new IOException("Failed to create component socket");
    }

    private void handleRunCommand(int argument) {
        ComponentGroup currentGroup = groupManager.getCurrentGroup();

        if (currentGroup.getTimeLimit() != null) {
            System.out.println("Execution time limit: " + currentGroup.getTimeLimit() + " seconds");
        }

        currentGroup.setRunning(true);
        Map<Integer, Component> newComponents = new HashMap<>();
        List<CompletableFuture<Void>> componentFutures = new ArrayList<>();

        for (Component oldComponent : currentGroup.getComponents().values()) {
            try {
                if (!oldComponent.getSocket().isClosed()) {
                    oldComponent.getSocket().close();
                }

                Socket newSocket = createComponentSocket(oldComponent.getIndex());
                Component newComponent = new Component(
                        oldComponent.getIndex(),
                        oldComponent.getSymbol(),
                        newSocket
                );

                newComponent.setStatus(ComponentStatus.RUNNING);

                CompletableFuture<Double> resultFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return executeComponent(newComponent, argument);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, groupManager.getExecutorService());

                if (currentGroup.getTimeLimit() != null) {
                    resultFuture = resultFuture.orTimeout(currentGroup.getTimeLimit(), TimeUnit.SECONDS)
                            .exceptionally(e -> {
                                newComponent.setStatus(ComponentStatus.FAILED);
                                notificationManager.sendNotification("Component " + newComponent.getIndex() +
                                        " failed due to time limit: " + e.getMessage());
                                return null;
                            });
                }

                newComponent.setResult(resultFuture);

                CompletableFuture<Void> componentFuture = resultFuture
                        .thenAccept(result -> {
                            newComponent.setStatus(ComponentStatus.COMPLETED);
                            notificationManager.sendNotification("Component " + newComponent.getIndex() +
                                    " completed with result: " + result);
                        })
                        .exceptionally(e -> {
                            newComponent.setStatus(ComponentStatus.FAILED);
                            notificationManager.sendNotification("Component " + newComponent.getIndex() +
                                    " failed: " + e.getMessage());
                            return null;
                        });

                componentFutures.add(componentFuture);
                newComponents.put(oldComponent.getIndex(), newComponent);

            } catch (IOException e) {
                System.err.println("Failed to restart component " + oldComponent.getIndex() + ": " + e.getMessage());
            }
        }

        currentGroup.getComponents().clear();
        currentGroup.getComponents().putAll(newComponents);

        CompletableFuture.allOf(componentFutures.toArray(new CompletableFuture[0]))
                .whenComplete((v, e) -> {
                    currentGroup.setRunning(false);
                    notificationManager.sendNotification("Group " + currentGroup.getIndex() + " completed");
                });
    }

    private double executeComponent(Component component, int argument) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(component.getSocket().getOutputStream());
             ObjectInputStream in = new ObjectInputStream(component.getSocket().getInputStream())) {

            out.writeInt(argument);
            out.writeChar(component.getSymbol());
            out.flush();

            return in.readDouble();
        }
    }

    private void handleStatusCommand(int componentIndex) {
        if (!notificationManager.isInteractiveMode()) {
            System.out.println("The status command is only available in interactive mode.");
            return;
        }

        ComponentGroup currentGroup = groupManager.getCurrentGroup();
        Component component = currentGroup.getComponents().get(componentIndex);
        if (component == null) {
            throw new IllegalArgumentException("Component not found: " + componentIndex);
        }

        System.out.println("Component " + componentIndex + " status: " + component.getStatus());
    }


    private void handleSummaryCommand() {
        ComponentGroup currentGroup = groupManager.getCurrentGroup();
        System.out.println("Group " + currentGroup.getIndex() + " summary:");
        for (Component component : currentGroup.getComponents().values()) {
            String result = "N/A";
            if (component.getResult() != null && component.getResult().isDone()) {
                try {
                    result = String.valueOf(component.getResult().get());
                } catch (Exception e) {
                    result = "Error: " + e.getMessage();
                }
            }
            System.out.println("Component " + component.getIndex() +
                    " (Symbol: " + component.getSymbol() +
                    "): Status=" + component.getStatus() +
                    ", Result=" + result);
        }
    }

    private void handleInteractiveCommand() {
        notificationManager.toggleInteractiveMode();
    }
}