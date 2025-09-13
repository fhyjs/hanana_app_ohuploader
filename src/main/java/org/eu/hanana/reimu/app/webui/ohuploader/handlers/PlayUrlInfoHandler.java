package org.eu.hanana.reimu.app.webui.ohuploader.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eu.hanana.reimu.app.webui.ohuploader.Util;
import org.eu.hanana.reimu.app.webui.ohuploader.util.BiliPlaybackData;
import org.eu.hanana.reimu.app.webui.ohuploader.util.BiliPlaybackUtil;
import org.eu.hanana.reimu.app.webui.ohuploader.util.WbiUtil;
import org.eu.hanana.reimu.webui.handler.AbstractEasyPathHandler;
import org.eu.hanana.reimu.webui.handler.AbstractPathHandler;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.function.Tuple2;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                var sessData = "";
                if (webUi.getSessionManage().getUser(httpServerRequest).data.has("bilibili_sess")){
                    sessData=webUi.getSessionManage().getUser(httpServerRequest).data.get("bilibili_sess").getAsString();
                }
                var login=!sessData.isEmpty();
                var cid = Util.getQueryParam(httpServerRequest.uri(),"cid");
                var vid = Util.getQueryParam(httpServerRequest.uri(),"vid");
                Tuple2<String, List<BiliPlaybackData>> data;
                if (login){
                    data=BiliPlaybackUtil.getDashPlaybackData(httpClient,sessData,type,vid,cid);
                }else {
                    data=BiliPlaybackUtil.getMp4PlaybackData(httpClient,type,vid,cid);
                }
                stringMonoSink.success(data.getT1());
            } catch (Exception e) {
                stringMonoSink.error(e);
                e.printStackTrace();
            }
        }));
    }
}
