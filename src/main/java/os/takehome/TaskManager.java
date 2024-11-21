package os.takehome;

import os.takehome.taskmanager.CommandProcessor;
import os.takehome.taskmanager.ComponentGroupManager;
import os.takehome.taskmanager.NotificationManager;
import os.takehome.taskmanager.ServerManager;

import java.util.*;

public class TaskManager {
    private final ComponentGroupManager groupManager;
    private final ServerManager serverManager;
    private final NotificationManager notificationManager;
    private final CommandProcessor commandProcessor;

    public TaskManager() {
        this.serverManager = new ServerManager();
        this.notificationManager = new NotificationManager();
        this.groupManager = new ComponentGroupManager();
        this.commandProcessor = new CommandProcessor(groupManager, serverManager, notificationManager);
    }

    public void processCommand(String command) {
        commandProcessor.processCommand(command);
    }

    public void shutdown() {
        System.out.println("Shutting down TaskManager...");
        serverManager.shutdown();
    }

    public static void main(String[] args) {
        TaskManager manager = new TaskManager();
        Runtime.getRuntime().addShutdownHook(new Thread(manager::shutdown));

        Scanner scanner = new Scanner(System.in);
        System.out.println("Task Manager started. Enter commands:");

        while (true) {
            System.out.print("> ");
            String command = scanner.nextLine();
            if (command.trim().isEmpty()) {
                manager.processCommand("interactive");
            } else if (command.equalsIgnoreCase("exit")) {
                break;
            } else {
                manager.processCommand(command);
            }
        }


        scanner.close();
        manager.shutdown();
    }
}