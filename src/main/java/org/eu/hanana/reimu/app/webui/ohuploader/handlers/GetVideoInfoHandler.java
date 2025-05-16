package org.eu.hanana.reimu.app.webui.ohuploader.handlers;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.eu.hanana.reimu.app.mod.webui.ModMain;
import org.eu.hanana.reimu.app.webui.ohuploader.Util;
import org.eu.hanana.reimu.webui.handler.AbstractPathHandler;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GetVideoInfoHandler extends AbstractPathHandler {

    @Override
    protected String getPath() {
        return "/data/ohupd/vidinfo.json";
    }

    @Override
    public Publisher<Void> handle(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        return Util.autoContentType(httpServerResponse).sendString(Mono.create(stringMonoSink -> {
            try (HttpClient httpClient = HttpClient.newHttpClient()){
                var type = Util.getQueryParam(httpServerRequest.uri(),"type").equals("av")?"aid":"bvid";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.bilibili.com/x/web-interface/view?"+type+"="+Util.getQueryParam(httpServerRequest.uri(),"vid")))
                        .header("Referer", "https://bilibili.com/")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
                        .method("GET", HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonElement jsonElement = JsonParser.parseString(response.body());
                if (jsonElement.getAsJsonObject().has("data")) {
                    jsonElement.getAsJsonObject().get("data").getAsJsonObject().addProperty("pic", Util.downloadFileToBase64(jsonElement.getAsJsonObject().get("data").getAsJsonObject().get("pic").getAsString()));
                }

                stringMonoSink.success(jsonElement.toString());
            } catch (Exception e) {
                stringMonoSink.error(e);
            }
        }));
    }
}
