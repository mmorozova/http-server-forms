package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;


public class Main {
    public static void main(String[] args) {
        Server server = new Server(9999);

        List<String> validPaths = List.of(
                "/index.html", "/spring.svg", "/spring.png",
                "/resources.html", "/styles.css", "/app.js",
                "/links.html", "/forms.html", "/classic.html",
                "/events.html", "/events.js"
        );

        for (String path : validPaths) {

            server.addHandler("GET", path, new Handler() {
                        public void handle(Request request, BufferedOutputStream responseStream) {
                            try {
                                Path filePath = Path.of(".", "public", path);
                                if (!Files.exists(filePath)) {
                                    responseStream.write((
                                            "HTTP/1.1 404 Not Found\r\n" +
                                                    "Content-Length: 0\r\n" +
                                                    "Connection: close\r\n" +
                                                    "\r\n"
                                    ).getBytes());
                                    responseStream.flush();
                                    return;
                                }
                                String mimeType = Files.probeContentType(filePath);
                                long length = Files.size(filePath);
                                responseStream.write((
                                        "HTTP/1.1 200 OK\r\n" +
                                                "Content-Type: " + mimeType + "\r\n" +
                                                "Content-Length: " + length + "\r\n" +
                                                "Connection: close\r\n" +
                                                "\r\n"
                                ).getBytes());
                                Files.copy(filePath, responseStream);
                                responseStream.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
            });
        }

        server.start();
    }
}