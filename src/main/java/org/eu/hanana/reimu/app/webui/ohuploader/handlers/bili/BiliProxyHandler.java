package org.eu.hanana.reimu.app.webui.ohuploader.handlers.bili;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eu.hanana.reimu.app.webui.ohuploader.Util;
import org.eu.hanana.reimu.webui.handler.AbstractEasyPathHandler;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BiliProxyHandler extends AbstractEasyPathHandler {
    @Override
    protected String getPath() {
        return "/data/ohupd/bili/get_proxy.json";
    }

    @Override
    public Publisher<Void> process(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        return Util.autoContentType(httpServerResponse).sendString(Mono.create(stringMonoSink -> {
            try {
                // 解析请求 body
                JsonObject data = JsonParser.parseString(
                        Objects.requireNonNull(
                                httpServerRequest.receive().aggregate().asString(StandardCharsets.UTF_8).block()
                        )
                ).getAsJsonObject();

                var url = data.get("url").getAsString();
                var referer = data.get("referer").getAsString();

                if (!url.contains("bilibili.com/")) throw new IllegalArgumentException("URL Not Allow");

                try (HttpClient httpClient = HttpClient.newHttpClient()) {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Referer", referer)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
                            .GET()
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    // 构建返回 JSON
                    JsonObject result = new JsonObject();
                    result.addProperty("body", response.body());

                    JsonObject head = new JsonObject();
                    for (Map.Entry<String, List<String>> entry : response.headers().map().entrySet()) {
                        JsonArray arr = new JsonArray();
                        for (String v : entry.getValue()) arr.add(v);
                        head.add(entry.getKey(), arr);
                    }
                    result.add("head", head);

                    stringMonoSink.success(result.toString());
                }
            } catch (Exception e) {
                stringMonoSink.error(e);
            }
        }));
    }
}
