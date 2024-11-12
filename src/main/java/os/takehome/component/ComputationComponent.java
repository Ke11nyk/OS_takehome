package os.takehome.component;

import os.takehome.Operation;

public interface ComputationComponent {
    void compute(int input);
    int getComponentIndex();
    Operation getOperation();
    boolean isFinished();
    void shutdown();
}