package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final Map<String, Map<String, Handler>> handlers = new HashMap<>();
    private final int port;
    private ExecutorService executor;

    public Server(int port) {
        this.port = port;
    }

    public void addHandler(String method, String path, Handler handler) {
        handlers.computeIfAbsent(method, k -> new HashMap<>()).put(path, handler);
    }

    public void start() {
        executor = Executors.newFixedThreadPool(64);
        try (final var serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            while (true) {
                final var socket = serverSocket.accept();
                executor.submit(() -> processConnection(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    private void processConnection(Socket socket) {
        try (
                var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            Request request = Request.from(in);

            if (request.getMethod().isEmpty() || request.getPath().isEmpty()) {
                writeError(out, 400, "Bad Request");
                return;
            }

            String method = request.getMethod().get();
            String path = request.getPath();

            Map<String, Handler> methodHandlers = handlers.get(method);
            if (methodHandlers == null || !methodHandlers.containsKey(path)) {
                writeError(out, 404, "Not Found");
                return;
            }

            methodHandlers.get(path).handle(request, out);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeError(BufferedOutputStream out, int code, String message) throws IOException {
        byte[] response = ("HTTP/1.1 " + code + " " + message + "\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n" +
                "\r\n").getBytes();
        out.write(response);
        out.flush();
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}