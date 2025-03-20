package org.eu.hanana.reimu.app.mod.webui.handler;

import org.eu.hanana.reimu.webui.core.Util;
import org.eu.hanana.reimu.webui.handler.AbstractModelJsEntryHandler;
import org.reactivestreams.Publisher;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public class AppJsMainHandler extends AbstractModelJsEntryHandler {
    @Override
    public Publisher<Void> handle(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        return Util.sendRedirect(httpServerResponse, httpServerRequest.uri() + "../../../../static/cp/webui_app/assets/index.js").send();
    }

    @Override
    public String getName() {
        return "app";
    }
}
