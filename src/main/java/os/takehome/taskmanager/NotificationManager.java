package os.takehome.taskmanager;

import java.util.*;

public class NotificationManager {
    private boolean isInteractiveMode;
    private final Queue<String> pendingNotifications;

    public NotificationManager() {
        this.isInteractiveMode = false;
        this.pendingNotifications = new LinkedList<>();
    }

    public void sendNotification(String message) {
        if (isInteractiveMode) {
            pendingNotifications.offer(message);
        } else {
            System.out.println(message);
        }
    }

    public void toggleInteractiveMode() {
        isInteractiveMode = !isInteractiveMode;
        if (!isInteractiveMode) {
            while (!pendingNotifications.isEmpty()) {
                System.out.println(pendingNotifications.poll());
            }
        }
        System.out.println("Interactive mode: " + (isInteractiveMode ? "ON" : "OFF"));
    }

    public boolean isInteractiveMode() {
        return isInteractiveMode;
    }
}