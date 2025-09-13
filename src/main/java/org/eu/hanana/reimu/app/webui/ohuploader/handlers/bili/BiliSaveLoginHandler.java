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

public class BiliSaveLoginHandler extends AbstractEasyPathHandler {
    @Override
    protected String getPath() {
        return "/data/ohupd/bili/save_login.php";
    }

    @Override
    public Publisher<Void> process(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        return Util.autoContentType(httpServerResponse).sendString(Mono.create(stringMonoSink -> {
            var data = httpServerRequest.receive().aggregate().asString(StandardCharsets.UTF_8).block();
            var user = webUi.getSessionManage().getUser(httpServerRequest);
            user.data.addProperty("bilibili_sess",data);
            if (user.data.get("bilibili_sess").isJsonNull()) {
                user.data.remove("bilibili_sess");
            }
            user.markDirty();
            stringMonoSink.success();
        }));
    }
}
