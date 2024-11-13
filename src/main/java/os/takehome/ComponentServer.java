package os.takehome;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ComponentServer {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final int PORT_START = 8000;
    private static final int MAX_PORTS = 10;
    private static final ServerSocket[] serverSockets = new ServerSocket[MAX_PORTS];

    public static void main(String[] args) {
        try {
            // Створюємо серверні сокети для кожного можливого компонента
            for (int i = 0; i < MAX_PORTS; i++) {
                final int port = PORT_START + i;
                final int componentIndex = i;
                serverSockets[i] = new ServerSocket(port);

                // Запускаємо обробку підключень для кожного порту в окремому потоці
                executorService.submit(() -> {
                    try {
                        System.out.println("Started listening on port " + port + " for component " + componentIndex);
                        while (true) {
                            Socket clientSocket = serverSockets[componentIndex].accept();
                            handleClient(clientSocket, componentIndex);
                        }
                    } catch (IOException e) {
                        System.err.println("Error on port " + port + ": " + e.getMessage());
                    }
                });
            }

            System.out.println("Component server started and listening on ports " + PORT_START + " to " + (PORT_START + MAX_PORTS - 1));

            // Тримаємо сервер запущеним
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private static void handleClient(Socket socket, int componentIndex) {
        executorService.submit(() -> {
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

                System.out.println("Client connected to component " + componentIndex);

                while (!socket.isClosed()) {
                    try {
                        // Читаємо вхідні дані
                        int input = in.readInt();
                        System.out.println("Received input " + input + " for component " + componentIndex);

                        // Обробляємо дані та надсилаємо результат
                        double result = processInput(input);
                        out.writeDouble(result);
                        out.flush();

                        System.out.println("Sent result " + result + " for component " + componentIndex);
                    } catch (EOFException e) {
                        // Клієнт відключився
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Error handling client for component " + componentIndex + ": " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
            }
        });
    }

    private static double processInput(int input) {
        try {
            // Симулюємо обробку
            Thread.sleep(2000);
            return Math.pow(input, 2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    private static void shutdown() {
        executorService.shutdown();
        for (ServerSocket serverSocket : serverSockets) {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
    }
}