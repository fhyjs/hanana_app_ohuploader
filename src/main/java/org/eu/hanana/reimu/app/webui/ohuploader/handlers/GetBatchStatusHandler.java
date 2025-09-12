package org.eu.hanana.reimu.app.webui.ohuploader.handlers;

import com.google.gson.Gson;
import org.eu.hanana.reimu.app.webui.ohuploader.Main;
import org.eu.hanana.reimu.app.webui.ohuploader.batch.BatchManager;
import org.eu.hanana.reimu.app.webui.ohuploader.batch.BatchWrapper;
import org.eu.hanana.reimu.app.webui.ohuploader.config.ConfigCore;
import org.eu.hanana.reimu.hnnapp.ModLoader;
import org.eu.hanana.reimu.hnnapp.mods.CfgCoreBase;
import org.eu.hanana.reimu.webui.handler.AbstractEasyPathHandler;
import org.eu.hanana.reimu.webui.handler.AbstractPathHandler;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class GetBatchStatusHandler extends AbstractEasyPathHandler {
    @Override
    protected String getPath() {
        return "/data/ohupd/batch_status.json";
    }

    @Override
    public Publisher<Void> process(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        return httpServerResponse.header("content-type","application/json; charset=utf-8").sendString(Mono.create(stringMonoSink -> {
            var uid = webUi.getSessionManage().getUser(httpServerRequest).data.get("ottohub_account").getAsJsonObject().get("uid").getAsInt();
            if (!BatchManager.getInstance().batchWorkFlow.containsKey(uid)){
                BatchManager.getInstance().batchWorkFlow.put(uid,new ArrayList<>());
            }
            List<BatchWrapper> batchWrappers = BatchManager.getInstance().batchWorkFlow.get(uid);
            stringMonoSink.success(new Gson().toJson(batchWrappers));
        }));
    }
}
