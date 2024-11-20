package os.takehome.taskmanager;

import os.takehome.component.Component;
import os.takehome.component.ComponentGroup;

import java.util.*;
import java.util.concurrent.*;

public class ComponentGroupManager {
    private final Map<Integer, ComponentGroup> groups;
    private ComponentGroup currentGroup;
    private final ExecutorService executorService;

    public ComponentGroupManager() {
        this.groups = new HashMap<>();
        this.executorService = Executors.newCachedThreadPool();
    }

    public void createOrSwitchGroup(int index) {
        groups.putIfAbsent(index, new ComponentGroup(index));
        currentGroup = groups.get(index);
        System.out.println("Switched to group " + index);
    }

    public ComponentGroup getCurrentGroup() {
        return currentGroup;
    }

    public void addComponent(Component component) {
        if (currentGroup == null) {
            throw new IllegalStateException("No group selected");
        }
        currentGroup.getComponents().put(component.getIndex(), component);
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void shutdown() {
        executorService.shutdownNow();
    }
}