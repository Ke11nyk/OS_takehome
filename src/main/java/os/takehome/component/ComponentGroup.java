package os.takehome.component;

import java.util.*;

public class ComponentGroup {
    private final int index;
    private final Map<Integer, Component> components;
    private boolean isRunning;

    public ComponentGroup(int index) {
        this.index = index;
        this.components = new HashMap<>();
        this.isRunning = false;
    }

    public int getIndex() { return index; }
    public Map<Integer, Component> getComponents() { return components; }
    public boolean isRunning() { return isRunning; }
    public void setRunning(boolean running) { isRunning = running; }
}