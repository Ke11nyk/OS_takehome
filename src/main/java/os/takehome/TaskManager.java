package os.takehome;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class TaskManager {
    private final Map<Integer, ComponentGroup> groups;
    private ComponentGroup currentGroup;
    private final ExecutorService executorService;
    private final ExecutorService serverExecutor;
    private boolean isInteractiveMode;
    private final Queue<String> pendingNotifications;
    private final ServerSocket[] serverSockets;
    private static final int PORT_START = 8000;
    private static final int MAX_PORTS = 10;
    private volatile boolean isServerRunning;

    public TaskManager() {
        this.groups = new HashMap<>();
        this.executorService = Executors.newCachedThreadPool();
        this.serverExecutor = Executors.newCachedThreadPool();
        this.isInteractiveMode = false;
        this.pendingNotifications = new LinkedList<>();
        this.serverSockets = new ServerSocket[MAX_PORTS];
        this.isServerRunning = false;
        startServer();
    }

    private void startServer() {
        try {
            for (int i = 0; i < MAX_PORTS; i++) {
                serverSockets[i] = new ServerSocket(PORT_START + i);
                final int componentIndex = i;

                serverExecutor.submit(() -> {
                    try {
                        System.out.println("Server listening on port " + (PORT_START + componentIndex));
                        while (!Thread.currentThread().isInterrupted()) {
                            Socket clientSocket = serverSockets[componentIndex].accept();
                            handleServerClient(clientSocket, componentIndex);
                        }
                    } catch (IOException e) {
                        if (!Thread.currentThread().isInterrupted()) {
                            System.err.println("Server error on port " + (PORT_START + componentIndex) + ": " + e.getMessage());
                        }
                    }
                });
            }
            isServerRunning = true;
            System.out.println("Component server started on ports " + PORT_START + "-" + (PORT_START + MAX_PORTS - 1));
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }

    private void handleServerClient(Socket socket, int componentIndex) {
        serverExecutor.submit(() -> {
            try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                    try {
                        int input = in.readInt();
                        char componentSymbol = in.readChar();

                        CalculationComponent calculator = ComponentFactory.getComponent(componentSymbol);
                        double result = calculator.calculate(input);

                        out.writeDouble(result);
                        out.flush();
                    } catch (EOFException e) {
                        break;
                    }
                }
            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    System.err.println("Client handling error: " + e.getMessage());
                }
            }
        });
    }

    private void processCommand(String command) {
        String[] parts = command.trim().split("\\s+");
        try {
            switch (parts[0].toLowerCase()) {
                case "group":
                    if (parts.length != 2) throw new IllegalArgumentException("Usage: group <index>");
                    handleGroupCommand(Integer.parseInt(parts[1]));
                    break;

                case "new":
                    if (parts.length != 2) throw new IllegalArgumentException("Usage: new <component symbol>");
                    handleNewCommand(parts[1].charAt(0));
                    break;

                case "run":
                    int argument = parts.length > 1 ? Integer.parseInt(parts[1]) : 5; // Default value is 5
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

    private void handleGroupCommand(int index) {
        groups.putIfAbsent(index, new ComponentGroup(index));
        currentGroup = groups.get(index);
        System.out.println("Switched to group " + index);
    }

    private void handleNewCommand(char symbol) throws IOException {
        if (currentGroup == null) {
            throw new IllegalStateException("No group selected");
        }

        // Перевіряємо, чи є такий символ компонента
        if (!ComponentFactory.isValidSymbol(symbol)) {
            throw new IllegalArgumentException("Invalid component symbol: " + symbol);
        }

        int componentIndex = currentGroup.getComponents().size();

        // Створюємо сокет з повторними спробами
        Socket socket = null;
        int maxRetries = 3;
        int retryCount = 0;

        while (socket == null && retryCount < maxRetries) {
            try {
                socket = new Socket("localhost", PORT_START + componentIndex);
                break;
            } catch (ConnectException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw new IOException("Failed to connect to component server after " + maxRetries + " attempts");
                }
                System.out.println("Connection attempt " + retryCount + " failed, retrying...");
                try {
                    Thread.sleep(1000); // Чекаємо секунду перед повторною спробою
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Connection interrupted");
                }
            }
        }

        Component component = new Component(componentIndex, symbol, socket);
        currentGroup.getComponents().put(componentIndex, component);

        System.out.println("Created component " + componentIndex + " with symbol " + symbol);
    }

    private void sendNotification(String message) {
        if (isInteractiveMode) {
            pendingNotifications.offer(message);
        } else {
            // В неінтерактивному режимі виводимо повідомлення одразу
            System.out.println(message);
        }
    }

    private void handleRunCommand(int argument) {
        if (currentGroup == null) {
            throw new IllegalStateException("No group selected");
        }

        currentGroup.setRunning(true);

        // Створюємо нові компоненти з новими сокетами
        Map<Integer, Component> newComponents = new HashMap<>();
        List<CompletableFuture<Void>> componentFutures = new ArrayList<>();

        for (Component oldComponent : currentGroup.getComponents().values()) {
            try {
                // Закриваємо старий сокет
                if (!oldComponent.getSocket().isClosed()) {
                    oldComponent.getSocket().close();
                }

                // Створюємо новий сокет і новий компонент
                Socket newSocket = createComponentSocket(oldComponent.getIndex());
                Component newComponent = new Component(
                        oldComponent.getIndex(),
                        oldComponent.getSymbol(),
                        newSocket
                );

                newComponent.setStatus(ComponentStatus.RUNNING);

                // Створюємо Future для кожного компонента
                CompletableFuture<Double> resultFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        return executeComponent(newComponent, argument);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, executorService);

                newComponent.setResult(resultFuture);

                // Створюємо обробник завершення для кожного компонента
                CompletableFuture<Void> componentFuture = resultFuture
                        .thenAccept(result -> {
                            newComponent.setStatus(ComponentStatus.COMPLETED);
                            sendNotification("Component " + newComponent.getIndex() +
                                    " completed with result: " + result);
                        })
                        .exceptionally(e -> {
                            newComponent.setStatus(ComponentStatus.FAILED);
                            sendNotification("Component " + newComponent.getIndex() +
                                    " failed: " + e.getMessage());
                            return null;
                        })
                        .whenComplete((v, e) -> {
                            try {
                                if (!newComponent.getSocket().isClosed()) {
                                    newComponent.getSocket().close();
                                }
                            } catch (IOException ioE) {
                                System.err.println("Error closing socket for component " +
                                        newComponent.getIndex() + ": " + ioE.getMessage());
                            }
                        });

                componentFutures.add(componentFuture);
                newComponents.put(oldComponent.getIndex(), newComponent);

            } catch (IOException e) {
                System.err.println("Failed to restart component " +
                        oldComponent.getIndex() + ": " + e.getMessage());
            }
        }

        // Оновлюємо мапу компонент в групі
        currentGroup.getComponents().clear();
        currentGroup.getComponents().putAll(newComponents);

        // Очікуємо завершення всіх компонент для фінального повідомлення
        CompletableFuture.allOf(componentFutures.toArray(new CompletableFuture[0]))
                .whenComplete((v, e) -> {
                    currentGroup.setRunning(false);
                    sendNotification("Group " + currentGroup.getIndex() + " completed");
                });
    }

    private Socket createComponentSocket(int componentIndex) throws IOException {
        int maxRetries = 3;
        int retryCount = 0;
        int retryDelayMs = 1000;

        while (retryCount < maxRetries) {
            try {
                return new Socket("localhost", PORT_START + componentIndex);
            } catch (ConnectException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw new IOException("Failed to connect after " +
                            maxRetries + " attempts");
                }
                System.out.println("Connection attempt " + retryCount +
                        " failed, retrying in " + retryDelayMs + "ms...");
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

    private double executeComponent(Component component, int argument) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(component.getSocket().getOutputStream());
             ObjectInputStream in = new ObjectInputStream(component.getSocket().getInputStream())) {

            out.writeInt(argument);
            out.writeChar(component.getSymbol());
            out.flush();

            return in.readDouble();
        }
    }

    public void shutdown() {
        System.out.println("Shutting down TaskManager...");
        for (ServerSocket serverSocket : serverSockets) {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
        executorService.shutdownNow();
        serverExecutor.shutdownNow();
    }

    private double performCalculation(char symbol, int input) {
        try {
            CalculationComponent component = ComponentFactory.getComponent(symbol);
            return component.calculate(input);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid component symbol: " + symbol);
        }
    }

    private void handleStatusCommand(int componentIndex) {
        if (currentGroup == null) {
            throw new IllegalStateException("No group selected");
        }

        Component component = currentGroup.getComponents().get(componentIndex);
        if (component == null) {
            throw new IllegalArgumentException("Component not found: " + componentIndex);
        }

        System.out.println("Component " + componentIndex + " status: " + component.getStatus());
    }

    private void handleSummaryCommand() {
        if (currentGroup == null) {
            throw new IllegalStateException("No group selected");
        }

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
        isInteractiveMode = !isInteractiveMode;
        if (!isInteractiveMode) {
            // Print all pending notifications
            while (!pendingNotifications.isEmpty()) {
                System.out.println(pendingNotifications.poll());
            }
        }
        System.out.println("Interactive mode: " + (isInteractiveMode ? "ON" : "OFF"));
    }

    public static void main(String[] args) {
        TaskManager manager = new TaskManager();
        Runtime.getRuntime().addShutdownHook(new Thread(manager::shutdown));

        Scanner scanner = new Scanner(System.in);
        System.out.println("Task Manager started. Enter commands:");

        while (true) {
            System.out.print("> ");
            String command = scanner.nextLine();
            if (command.equals("exit")) break;
            manager.processCommand(command);
        }

        scanner.close();
        manager.shutdown();
    }
}