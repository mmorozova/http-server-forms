package ru.netology;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.net.URLEncodedUtils;
//package org.apache.hc.core5.http;

public class Request {
    private final Optional<String> method;
    private final String path;
    private final Map<String, List<String>> queryParams = new HashMap<>();
    private final Map<String, String> headers = new HashMap<>();
    private final InputStream body;

    public Request(Optional<String> method, String path, Map<String, List<String>> queryParams, Map<String, String> headers, InputStream body) {
        this.method = method;
        this.path = path;
        this.queryParams.putAll(queryParams);
        this.headers.putAll(headers);
        this.body = body;
    }

    public static Request from(BufferedReader reader) throws IOException, URISyntaxException {
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            return new Request(
                    Optional.empty(),
                    "",
                    new HashMap<>(),
                    new HashMap<>(),
                    null
            );
        }

        String[] parts = requestLine.split(" ");
        if (parts.length != 3) {
            return new Request(
                    Optional.empty(),
                    "",
                    new HashMap<>(),
                    new HashMap<>(),
                    null
            );
        }

        String methodStr = parts[0];
        String uriStr = parts[1];


        URIBuilder uriBuilder = new URIBuilder(uriStr);
        URI uri = uriBuilder.build();
        String path = uri.getPath();
        List<NameValuePair> paramsList = URLEncodedUtils.parse(uri.getQuery(), StandardCharsets.UTF_8);

        Map<String, List<String>> queryParams = new HashMap<>();
        for (NameValuePair param : paramsList) {
            queryParams.computeIfAbsent(param.getName(), k -> new ArrayList<>()).add(param.getValue());
        }

        // Чтение заголовков
        Map<String, String> headers = new HashMap<>();
        String line;
        while (!(line = reader.readLine()).isEmpty()) {
            String[] headerParts = line.split(": ", 2);
            if (headerParts.length == 2) {
                headers.put(headerParts[0], headerParts[1]);
            }
        }

        int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
        InputStream body = new InputStream() {
            private int read = 0;

            @Override
            public int read() throws IOException {
                if (read >= contentLength) return -1;
                read++;
                return reader.read();
            }
        };

        return new Request(
                Optional.of(methodStr),
                path,
                queryParams,
                headers,
                body
        );
    }

    public Optional<String> getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Optional<String> getQueryParam(String name) {
        List<String> values = queryParams.get(name);
        return values != null && !values.isEmpty() ? Optional.of(values.get(0)) : Optional.empty();
    }

    public Map<String, List<String>> getQueryParams() {
        return Collections.unmodifiableMap(queryParams);
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    public InputStream getBody() {
        return body;
    }
}
