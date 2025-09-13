package org.eu.hanana.reimu.app.webui.ohuploader;

import org.eu.hanana.reimu.app.mod.webui.event.WebUiCreatedEvent;
import org.eu.hanana.reimu.app.webui.ohuploader.config.ConfigCore;
import org.eu.hanana.reimu.app.webui.ohuploader.handlers.*;
import org.eu.hanana.reimu.app.webui.ohuploader.handlers.bili.BiliPasswordLoginHandler;
import org.eu.hanana.reimu.app.webui.ohuploader.handlers.bili.BiliProxyHandler;
import org.eu.hanana.reimu.app.webui.ohuploader.handlers.bili.BiliSaveLoginHandler;
import org.eu.hanana.reimu.hnnapp.ModLoader;
import org.eu.hanana.reimu.hnnapp.mods.Event;
import org.eu.hanana.reimu.hnnapp.mods.ModEntry;
import org.eu.hanana.reimu.hnnapp.mods.events.PostInitModsEvent;

import static org.eu.hanana.reimu.app.webui.ohuploader.Main.MOD_ID;

@ModEntry(name = MOD_ID,id = MOD_ID)
public class Main {
    public static final String MOD_ID = "ohuploader";
    public Main(){
        ModLoader.getLoader().regEventBuses(this);
    }
    @Event
    public void onInit(PostInitModsEvent event){
        ModLoader.getLoader().regCfgCore(MOD_ID,new ConfigCore());
    }
    @Event
    public void onWebUiCreated(WebUiCreatedEvent event){
        var wui = event.getWebui();
        wui.handlers.add(new OhuJsMainHandler());
        wui.handlers.add(new GetVideoInfoHandler());
        wui.handlers.add(new DownloadVideoHandler());
        wui.handlers.add(new OhLoginHandler());
        wui.handlers.add(new GetVideoTagsHandler());
        wui.handlers.add(new PlayUrlInfoHandler());
        wui.handlers.add(new QuicLoginHandler());
        wui.handlers.add(new GetSystemConfigHandler());
        wui.handlers.add(new GetBatchStatusHandler());
        wui.handlers.add(new CreateBatchHandler());
        //bili
        wui.handlers.add(new BiliProxyHandler());
        wui.handlers.add(new BiliSaveLoginHandler());
        wui.handlers.add(new BiliPasswordLoginHandler());
    }
}