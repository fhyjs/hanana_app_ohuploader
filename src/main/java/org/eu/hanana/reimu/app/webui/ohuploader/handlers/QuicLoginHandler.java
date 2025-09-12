package org.eu.hanana.reimu.app.webui.ohuploader.handlers;

import org.eu.hanana.reimu.app.webui.ohuploader.Util;
import org.eu.hanana.reimu.webui.handler.AbstractEasyPathHandler;
import org.eu.hanana.reimu.webui.handler.AbstractPathHandler;
import org.eu.hanana.reimu.webui.session.User;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class QuicLoginHandler extends AbstractEasyPathHandler {

    @Override
    protected String getPath() {
        return "/dynamic/ohupd/quic_login.html";
    }

    @Override
    public Publisher<Void> process(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        return Util.autoContentType(httpServerResponse).sendString(Mono.create(stringMonoSink -> {
            String login = Util.getQueryParam(httpServerRequest.uri(), "login");
            User user = webUi.getSessionManage().getUser(httpServerRequest);
            user.data.addProperty("ottohub_password",login);
            stringMonoSink.success("<h1>登录完成:"+login.split(":")[0]+"</h1><h2>您可以返回上一页了<h2/><script>window.close();</script>");
        }));
    }
}
