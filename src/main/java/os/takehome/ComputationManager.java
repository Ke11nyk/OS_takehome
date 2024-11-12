package os.takehome;

import os.takehome.component.ComputationComponent;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

public class ComputationManager {
    private final Map<Integer, ComputationGroup> groups = new HashMap<>();
    private final Scanner scanner = new Scanner(System.in);
    private volatile boolean isInteracting = false;

    public void start() {
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            if (input.equals("exit")) {
                shutdown();
                break;
            } else if (input.equals("interactive")) {
                startInteractiveMode();
            } else {
                processCommand(input);
            }
        }
    }

    private void startInteractiveMode() {
        isInteracting = true;
        System.out.println("Entering interactive mode. Press Enter to exit.");

        while (isInteracting) {
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    isInteracting = false;
                    System.out.println("Exiting interactive mode.");
                } else {
                    processCommand(line);
                }
            }
        }
    }

    private void processCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        if (parts.length < 1) return;

        try {
            switch (parts[0]) {
                case "group":
                    if (parts.length < 2) {
                        System.out.println("Usage: group <index>");
                        return;
                    }
                    createGroup(Integer.parseInt(parts[1]));
                    break;

                case "new":
                    if (parts.length < 2) {
                        System.out.println("Usage: new <component symbol>,<group index>");
                        return;
                    }
                    String[] newParts = parts[1].split(",");
                    if (newParts.length != 2) {
                        System.out.println("Invalid format. Use: new <component symbol>,<group index>");
                        return;
                    }
                    createComponent(newParts[0].trim().charAt(0), Integer.parseInt(newParts[1].trim()));
                    break;

                case "run":
                    if (parts.length < 2) {
                        System.out.println("Usage: run <group index>");
                        return;
                    }
                    runGroup(Integer.parseInt(parts[1]));
                    break;

                case "status":
                    if (parts.length < 2) {
                        System.out.println("Usage: status <group index> <component index>");
                        return;
                    }
                    String[] statusParts = parts[1].split(",");
                    if (statusParts.length != 2) {
                        System.out.println("Invalid format. Use: status <group index>,<component index>");
                        return;
                    }
                    showComponentStatus(Integer.parseInt(statusParts[0].trim()), Integer.parseInt(statusParts[1].trim()));
                    break;

                case "summary":
                    if (parts.length < 2) {
                        System.out.println("Usage: summary <group index>");
                        return;
                    }
                    showGroupSummary(Integer.parseInt(parts[1]));
                    break;

                default:
                    System.out.println("Unknown command");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private void createGroup(int index) {
        if (index <= 0) {
            System.out.println("Group index must be positive");
            return;
        }
        if (groups.containsKey(index)) {
            System.out.println("Group already exists");
            return;
        }
        groups.put(index, new ComputationGroup(index));
        System.out.println("new group with index " + index + " was created");
    }

    private void createComponent(char operationSymbol, int groupIndex) {
        ComputationGroup group = groups.get(groupIndex);
        if (group == null) {
            System.out.println("Group not found");
            return;
        }
        try {
            Operation operation = Operation.fromSymbol(operationSymbol);
            int componentIndex = group.addComponent(operation);
            System.out.println("new component " + operationSymbol + " with index " + componentIndex +
                    " was added to group " + groupIndex);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid operation symbol");
        }
    }

    private void runGroup(int groupIndex) {
        ComputationGroup group = groups.get(groupIndex);
        if (group == null) {
            System.out.println("Group not found");
            return;
        }
        group.runAll();
    }

    private void showSummary(int groupIndex) {
        ComputationGroup group = groups.get(groupIndex);
        if (group == null) {
            System.out.println("Group not found");
            return;
        }

        System.out.println("Group " + groupIndex + " summary:");
        for (ComputationComponent component : group.getComponents()) {
            System.out.printf("Component %d (%c): %s%n",
                    component.getComponentIndex(),
                    component.getOperation().symbol,
                    component.isFinished() ? "finished" : "running");
        }
    }

    private void showComponentStatus(int groupIndex, int componentIndex) {
        ComputationGroup group = groups.get(groupIndex);
        if (group == null) {
            System.out.println("Group not found");
            return;
        }

        ComputationComponent component = group.getComponentByIndex(componentIndex);
        if (component == null) {
            System.out.println("Component not found");
            return;
        }

        String status = component.isFinished() ? "finished" : "running";
        System.out.printf("Component %d (%c): %s%n",
                component.getComponentIndex(),
                component.getOperation().symbol,
                status);
    }

    private void showGroupSummary(int groupIndex) {
        ComputationGroup group = groups.get(groupIndex);
        if (group == null) {
            System.out.println("Group not found");
            return;
        }

        System.out.println("Group " + groupIndex + " summary:");
        for (ComputationComponent component : group.getComponents()) {
            showComponentStatus(groupIndex, component.getComponentIndex());
        }
    }

    private void shutdown() {
        groups.values().forEach(ComputationGroup::shutdown);
        System.out.println("Shutting down Computation Manager");
    }

    public static void main(String[] args) {
        new ComputationManager().start();
    }
}