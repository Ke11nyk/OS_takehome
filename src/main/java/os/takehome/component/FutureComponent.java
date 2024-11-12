package os.takehome.component;

import os.takehome.Operation;

import java.util.concurrent.*;

public class FutureComponent implements ComputationComponent {
    private final int groupIndex;
    private final int componentIndex;
    private final Operation operation;
    private Future<Double> future;
    private final ExecutorService executor;

    public FutureComponent(int groupIndex, int componentIndex, Operation operation) {
        this.groupIndex = groupIndex;
        this.componentIndex = componentIndex;
        this.operation = operation;
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void compute(int input) {
        future = executor.submit(() -> {
            double result = operation.getFunction().apply(input);
            System.out.println("component " + componentIndex + " finished");
            return result;
        });
    }

    @Override
    public int getComponentIndex() {
        return componentIndex;
    }

    @Override
    public Operation getOperation() {
        return operation;
    }

    @Override
    public boolean isFinished() {
        return future != null && future.isDone();
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }
}