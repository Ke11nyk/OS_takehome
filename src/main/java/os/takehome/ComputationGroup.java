package os.takehome;

import os.takehome.component.*;

import java.util.*;

class ComputationGroup {
    private final int groupIndex;
    private final List<ComputationComponent> components;
    private int nextComponentIndex;
    private static int componentType = 0; // 0 - Future, 1 - Socket, 2 - BlockingQueue

    public ComputationGroup(int groupIndex) {
        this.groupIndex = groupIndex;
        this.components = new ArrayList<>();
        this.nextComponentIndex = 1;
    }

    public int addComponent(Operation operation) {
        int componentIndex = nextComponentIndex++;
        ComputationComponent component;

        // Циклічно змінюємо тип компоненти для демонстрації різних підходів
        switch (componentType) {
            case 0:
                component = new FutureComponent(groupIndex, componentIndex, operation);
                break;
            case 1:
                component = new SocketComponent(groupIndex, componentIndex, operation);
                break;
            case 2:
                component = new BlockingQueueComponent(groupIndex, componentIndex, operation);
                break;
            default:
                component = new FutureComponent(groupIndex, componentIndex, operation);
        }

        componentType = (componentType + 1) % 3;
        components.add(component);
        return componentIndex;
    }

    public void runAll() {
        Random random = new Random();
        for (ComputationComponent component : components) {
            component.compute(random.nextInt(100) + 1);
        }
    }

    public List<ComputationComponent> getComponents() {
        return components;
    }

    public ComputationComponent getComponentByIndex(int index) {
        return components.get(index);
    }

    public void shutdown() {
        components.forEach(ComputationComponent::shutdown);
    }
}