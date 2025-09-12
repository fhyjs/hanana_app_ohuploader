package org.eu.hanana.reimu.app.webui.ohuploader.handlers;

import org.eu.hanana.reimu.app.webui.ohuploader.Main;
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

public class GetSystemConfigHandler extends AbstractEasyPathHandler {
    @Override
    protected String getPath() {
        return "/data/ohupd/system_config.json";
    }

    @Override
    public Publisher<Void> process(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        return httpServerResponse.header("content-type","application/json; charset=utf-8").sendString(Mono.create(stringMonoSink -> {
            CfgCoreBase cfgCore = ModLoader.getLoader().getMod(Main.MOD_ID).cfgCore;
            File cfgFile = new File(cfgCore.cfgDir, ConfigCore.Config.class.getName() + ".cfg");
            try {
                stringMonoSink.success(Files.readString(cfgFile.toPath()));
            } catch (IOException e) {
                stringMonoSink.error(e);
            }
        }));
    }
}
