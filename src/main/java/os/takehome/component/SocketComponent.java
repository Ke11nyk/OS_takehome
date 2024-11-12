package os.takehome.component;

import os.takehome.Operation;

import java.io.*;
import java.net.*;

public class SocketComponent implements ComputationComponent {
    private final int groupIndex;
    private final int componentIndex;
    private final Operation operation;
    private Socket socket;
    private final int port;
    private volatile boolean finished = false;
    private Thread serverThread;

    public SocketComponent(int groupIndex, int componentIndex, Operation operation) {
        this.groupIndex = groupIndex;
        this.componentIndex = componentIndex;
        this.operation = operation;
        this.port = 8000 + componentIndex; // Унікальний порт для кожної компоненти
        startServer();
    }

    private void startServer() {
        serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (!Thread.currentThread().isInterrupted()) {
                    try (Socket clientSocket = serverSocket.accept();
                         DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                         DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

                        int input = in.readInt();
                        double result = operation.getFunction().apply(input);
                        out.writeDouble(result);
                        System.out.println("component " + componentIndex + " finished");
                        finished = true;
                    }
                }
            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    e.printStackTrace();
                }
            }
        });
        serverThread.start();
    }

    @Override
    public void compute(int input) {
        new Thread(() -> {
            try (Socket socket = new Socket("localhost", port);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {

                out.writeInt(input);
                double result = in.readDouble();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
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
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }
}