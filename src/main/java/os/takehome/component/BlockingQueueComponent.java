package os.takehome.component;

import os.takehome.Operation;

import java.util.concurrent.*;

public class BlockingQueueComponent implements ComputationComponent {
    private final int groupIndex;
    private final int componentIndex;
    private final Operation operation;
    private final BlockingQueue<Integer> inputQueue;
    private final BlockingQueue<Double> outputQueue;
    private volatile boolean finished = false;
    private Thread processingThread;

    public BlockingQueueComponent(int groupIndex, int componentIndex, Operation operation) {
        this.groupIndex = groupIndex;
        this.componentIndex = componentIndex;
        this.operation = operation;
        this.inputQueue = new LinkedBlockingQueue<>();
        this.outputQueue = new LinkedBlockingQueue<>();
        startProcessing();
    }

    private void startProcessing() {
        processingThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    int input = inputQueue.take();
                    double result = operation.getFunction().apply(input);
                    outputQueue.put(result);
                    System.out.println("component " + componentIndex + " finished");
                    finished = true;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        processingThread.start();
    }

    @Override
    public void compute(int input) {
        try {
            inputQueue.put(input);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
        return finished;
    }

    @Override
    public void shutdown() {
        if (processingThread != null) {
            processingThread.interrupt();
        }
    }
}