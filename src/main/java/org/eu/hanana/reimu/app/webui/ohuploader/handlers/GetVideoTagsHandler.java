package org.eu.hanana.reimu.app.webui.ohuploader.handlers;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.eu.hanana.reimu.app.webui.ohuploader.Util;
import org.eu.hanana.reimu.webui.handler.AbstractEasyPathHandler;
import org.eu.hanana.reimu.webui.handler.AbstractPathHandler;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GetVideoTagsHandler extends AbstractEasyPathHandler {

    @Override
    protected String getPath() {
        return "/data/ohupd/vidtags.json";
    }

    @Override
    public Publisher<Void> process(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        return Util.autoContentType(httpServerResponse).sendString(Mono.create(stringMonoSink -> {
            try (HttpClient httpClient = HttpClient.newHttpClient()){
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.bilibili.com/x/tag/archive/tags?aid="+Util.getQueryParam(httpServerRequest.uri(),"aid")))
                        //.header("Referer", "https://bilibili.com/")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
                        .method("GET", HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                stringMonoSink.success(response.body());
            } catch (Exception e) {
                stringMonoSink.error(e);
            }
        }));
    }
}
