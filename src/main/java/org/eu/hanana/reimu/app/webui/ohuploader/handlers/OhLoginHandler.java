package org.eu.hanana.reimu.app.webui.ohuploader.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

public class OhLoginHandler extends AbstractPathHandler {
    @Override
    protected String getPath() {
        return "/data/ohupd/login.json";
    }

    @Override
    public Publisher<Void> handle(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        return Util.autoContentType(httpServerResponse).sendString(Mono.create(stringMonoSink -> {
            var act = Util.getQueryParam(httpServerRequest.uri(),"act");
            if (act.equals("login")) {
                try (var httpClient = HttpClient.newHttpClient()) {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://ottohub.cn/api/user/user_select.php"))
                            .header("content-type", "multipart/form-data; boundary=---011000010111000001101001")
                            .method("POST", HttpRequest.BodyPublishers.ofString("-----011000010111000001101001\r\nContent-Disposition: form-data; name=\"action\"\r\n\r\nlogin\r\n-----011000010111000001101001\r\nContent-Disposition: form-data; name=\"uid_email\"\r\n\r\n" + Util.getQueryParam(httpServerRequest.uri(), "un") + "\r\n-----011000010111000001101001\r\nContent-Disposition: form-data; name=\"pw\"\r\n\r\n" + Util.getQueryParam(httpServerRequest.uri(), "pw") + "\r\n-----011000010111000001101001--\r\n\r\n"))
                            .build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    JsonObject jsonObject = new JsonObject();
                    if (response.body().contains("error")) {
                        jsonObject.addProperty("status", "error");
                        jsonObject.addProperty("msg", response.body());
                    } else {
                        jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
                        jsonObject.addProperty("PHPSESSID",Util.parseCookies(response).get("PHPSESSID"));
                        webUi.getSessionManage().getUser(httpServerRequest).data.add("ottohub_account", jsonObject);
                        webUi.getSessionManage().getUser(httpServerRequest).markDirty();
                    }
                    stringMonoSink.success(jsonObject.toString());
                } catch (Exception e) {
                    stringMonoSink.error(e);
                }
            } else if (act.equals("logout")) {
                webUi.getSessionManage().getUser(httpServerRequest).data.remove("ottohub_account");
                webUi.getSessionManage().getUser(httpServerRequest).markDirty();
                stringMonoSink.success("{}");
            }
        }));
    }
}
