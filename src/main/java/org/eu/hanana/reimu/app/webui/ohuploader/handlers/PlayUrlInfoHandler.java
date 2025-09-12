package org.eu.hanana.reimu.app.webui.ohuploader.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

public class PlayUrlInfoHandler extends AbstractEasyPathHandler {

    @Override
    protected String getPath() {
        return "/data/ohupd/urlinfo.json";
    }

    @Override
    public Publisher<Void> process(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        return Util.autoContentType(httpServerResponse).sendString(Mono.create(stringMonoSink -> {
            try (HttpClient httpClient = HttpClient.newHttpClient()){
                var type = Util.getQueryParam(httpServerRequest.uri(),"type").equals("av")?"avid":"bvid";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.bilibili.com/x/player/playurl?cid="+Util.getQueryParam(httpServerRequest.uri(),"cid")+"&"+type+"="+Util.getQueryParam(httpServerRequest.uri(),"vid")+"&fnval=1&qn="+Util.getQueryParam(httpServerRequest.uri(),"qn")))
                        .method("GET", HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonObject asJsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray accept_quality = asJsonObject.get("data").getAsJsonObject().get("accept_quality").getAsJsonArray();
                JsonObject sizes = new JsonObject();
                for (JsonElement jsonElement : accept_quality) {
                    request = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.bilibili.com/x/player/playurl?cid="+Util.getQueryParam(httpServerRequest.uri(),"cid")+"&"+type+"="+Util.getQueryParam(httpServerRequest.uri(),"vid")+"&fnval=1&qn="+jsonElement.getAsInt()))
                            .method("GET", HttpRequest.BodyPublishers.noBody())
                            .build();
                    HttpResponse<String> response1 = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    JsonObject asJsonObject1 = JsonParser.parseString(response1.body()).getAsJsonObject().get("data").getAsJsonObject();
                    sizes.addProperty(String.valueOf(asJsonObject1.get("quality").getAsInt()),asJsonObject1.get("durl").getAsJsonArray().get(0).getAsJsonObject().get("size").getAsLong());
                }
                asJsonObject.add("sizes",sizes);
                stringMonoSink.success(asJsonObject.toString());
            } catch (Exception e) {
                stringMonoSink.error(e);
                e.printStackTrace();
            }
        }));
    }
}
