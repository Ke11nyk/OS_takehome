package os.takehome.taskmanager;

import os.takehome.component.CalculationComponent;
import os.takehome.component.ComponentFactory;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ServerManager {
    private final ServerSocket[] serverSockets;
    private final ExecutorService serverExecutor;
    private static final int PORT_START = 8000;
    private static final int MAX_PORTS = 10;
    private volatile boolean isServerRunning;

    public ServerManager() {
        this.serverSockets = new ServerSocket[MAX_PORTS];
        this.serverExecutor = Executors.newCachedThreadPool();
        this.isServerRunning = false;
        startServer();
    }

    private void startServer() {
        try {
            for (int i = 0; i < MAX_PORTS; i++) {
                serverSockets[i] = new ServerSocket(PORT_START + i);
                final int componentIndex = i;

                serverExecutor.submit(() -> handleServerPort(componentIndex));
            }
            isServerRunning = true;
            System.out.println("Component server started on ports " + PORT_START + "-" + (PORT_START + MAX_PORTS - 1));
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }

    private void handleServerPort(int componentIndex) {
        try {
            System.out.println("Server listening on port " + (PORT_START + componentIndex));
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSockets[componentIndex].accept();
                handleServerClient(clientSocket);
            }
        } catch (IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                System.err.println("Server error on port " + (PORT_START + componentIndex) + ": " + e.getMessage());
            }
        }
    }

    private void handleServerClient(Socket socket) {
        serverExecutor.submit(() -> {
            try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                    try {
                        int input = in.readInt();
                        char componentSymbol = in.readChar();

                        CalculationComponent calculator = ComponentFactory.getComponent(componentSymbol);
                        double result = calculator.calculate(input);

                        out.writeDouble(result);
                        out.flush();
                    } catch (EOFException e) {
                        break;
                    }
                }
            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    System.err.println("Client handling error: " + e.getMessage());
                }
            }
        });
    }

    public void shutdown() {
        for (ServerSocket serverSocket : serverSockets) {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
        serverExecutor.shutdownNow();
    }

    public static int getPortStart() {
        return PORT_START;
    }
}