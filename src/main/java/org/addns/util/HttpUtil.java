package org.addns.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/**
 * @author YahocenMiniPC
 */
public class HttpUtil {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    /**
     * 发送 GET 请求并返回响应内容。
     *
     * @param url URL 地址
     * @return 响应内容
     */
    public static String get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 发送 POST 请求并返回响应内容。
     *
     * @param url      URL 地址
     * @param payload  请求体内容
     * @param headers  请求头
     * @return 响应内容
     */
    public static String post(String url, String payload, HttpHeaders headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url)).POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(builder::header);
        }
        try {
            return HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString()).body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static class HttpHeaders extends LinkedHashMap<String, String> {

        public HttpHeaders() {
            super();
        }

        public HttpHeaders add(String key, String value) {
            put(key, value);
            return this;
        }

    }

}