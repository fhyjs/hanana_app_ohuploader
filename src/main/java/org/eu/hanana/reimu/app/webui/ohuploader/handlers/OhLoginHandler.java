package org.eu.hanana.reimu.app.webui.ohuploader.handlers;

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

public class OhLoginHandler extends AbstractEasyPathHandler {
    @Override
    protected String getPath() {
        return "/data/ohupd/login.json";
    }

    @Override
    public Publisher<Void> process(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        return Util.autoContentType(httpServerResponse).sendString(Mono.create(stringMonoSink -> {
            var act = Util.getQueryParam(httpServerRequest.uri(),"act");
            if (act.equals("login")) {
                try (var httpClient = HttpClient.newHttpClient()) {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.ottohub.cn/?module=auth&action=login&uid_email="+Util.getQueryParam(httpServerResponse.uri(),"un")+"&pw="+Util.getQueryParam(httpServerResponse.uri(),"pw")))
                            .method("GET", HttpRequest.BodyPublishers.noBody())
                            .build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    JsonObject jsonObject = new JsonObject();
                    if (response.body().contains("error")) {
                        jsonObject.addProperty("status", "error");
                        jsonObject.addProperty("msg", response.body());
                    } else {
                        jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
                        //jsonObject.addProperty("PHPSESSID",Util.parseCookies(response).get("PHPSESSID"));
                        request = HttpRequest.newBuilder()
                                .uri(URI.create("https://api.ottohub.cn/?module=profile&action=user_profile&token="+jsonObject.get("token").getAsString()))
                                .method("GET", HttpRequest.BodyPublishers.noBody())
                                .build();
                        response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                        jsonObject.add("profile",JsonParser.parseString(response.body()));
                        webUi.getSessionManage().getUser(httpServerRequest).data.add("ottohub_account", jsonObject);
                        webUi.getSessionManage().getUser(httpServerRequest).markDirty();
                    }
                    stringMonoSink.success(jsonObject.toString());
                } catch (Exception e) {
                    stringMonoSink.error(e);
                    e.printStackTrace();
                }
            } else if (act.equals("logout")) {
                webUi.getSessionManage().getUser(httpServerRequest).data.remove("ottohub_account");
                webUi.getSessionManage().getUser(httpServerRequest).markDirty();
                stringMonoSink.success("{}");
            }
        }));
    }
}
