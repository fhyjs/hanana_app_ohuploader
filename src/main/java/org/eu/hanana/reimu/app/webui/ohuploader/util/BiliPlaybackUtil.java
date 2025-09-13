package org.eu.hanana.reimu.app.webui.ohuploader.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eu.hanana.reimu.app.webui.ohuploader.Util;
import org.jetbrains.annotations.Nullable;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BiliPlaybackUtil {
    /**
     * 获取Dash播放数据
     * @param sessdata 登录令牌
     * @param type avid/bvid
     * @param vid 视频变号
     * @param cid 分集编号
     *
     */
    public static Tuple2<String,List<BiliPlaybackData>> getDashPlaybackData(HttpClient httpClient, @Nullable String sessdata, String type, String vid, String cid) throws IOException, InterruptedException {
        var sessData = "";
        if (sessdata!=null){
            sessData=sessdata;
        }
        var login=!sessData.isEmpty();
        var imgKey = "";
        var subKey = "";

        {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.bilibili.com/x/web-interface/nav"))
                    .header("Cookie", "SESSDATA="+sessData) // 添加多个 Cookie 用分号分隔
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject asJsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject wbi_img = asJsonObject.get("data").getAsJsonObject().get("wbi_img").getAsJsonObject();
            imgKey= wbi_img.get("img_url").getAsString();
            subKey= wbi_img.get("sub_url").getAsString();
            Pattern pattern = Pattern.compile("/([a-f0-9]{32})\\.png$");
            Matcher matcherImgKey = pattern.matcher(imgKey);
            Matcher matcherSubKey = pattern.matcher(subKey);
            if (matcherImgKey.find()) {
                imgKey = matcherImgKey.group(1);
            }
            if (matcherSubKey.find()) {
                subKey = matcherSubKey.group(1);
            }
        }
        var param = WbiUtil.getParam(Map.of(
                "cid",cid,
                type,vid,
                "fnval",4048,
                "qn","127"
        ),imgKey,subKey);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.bilibili.com/x/player/wbi/playurl?"+param))
                .header("Cookie", "SESSDATA="+sessData) // 添加多个 Cookie 用分号分隔
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject asJsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray accept_quality = asJsonObject.get("data").getAsJsonObject().get("accept_quality").getAsJsonArray();
        JsonObject sizes = new JsonObject();
        var videos = asJsonObject.get("data").getAsJsonObject().get("dash").getAsJsonObject().get("video").getAsJsonArray();
        var audios = asJsonObject.get("data").getAsJsonObject().get("dash").getAsJsonObject().get("audio").getAsJsonArray();
        var duration = asJsonObject.get("data").getAsJsonObject().get("dash").getAsJsonObject().get("duration").getAsLong();
        var audio = ((JsonObject) null);
        for (JsonElement jsonElement : audios) {
            var ao = jsonElement.getAsJsonObject();
            var id = ao.get("id").getAsInt();
            var oid=0;
            if (audio != null) {
                oid = audio.get("id").getAsInt();
            }
            if (id>oid){
                audio=ao;
            }
        }
        for (JsonElement jsonElement : accept_quality) {
            var qn = jsonElement.getAsInt();
            var vn = (JsonObject)null;
            for (JsonElement video : videos) {
                var vo =video.getAsJsonObject();
                if (vo.get("id").getAsInt()==qn){
                    vn=vo;
                    break;
                }
            }
            if (vn==null){
                sizes.addProperty(String.valueOf(qn),-1);
                continue;
            }
            var bandwidth = vn.get("bandwidth").getAsLong();
            sizes.addProperty(String.valueOf(qn),duration*bandwidth/8f+audio.get("bandwidth").getAsLong()*duration/8f);
        }
        //
        asJsonObject.add("sizes",sizes);

        var result = new ArrayList<BiliPlaybackData>();
        for (JsonElement video : videos) {
            var vo = video.getAsJsonObject();
            BiliPlaybackData biliPlaybackData = new BiliPlaybackData();
            biliPlaybackData.isDash=true;
            biliPlaybackData.videoUrl=vo.get("baseUrl").getAsString();
            biliPlaybackData.audioUrl=audio.get("baseUrl").getAsString();
            var qn = vo.get("id").getAsInt();
            biliPlaybackData.size=sizes.get(String.valueOf(qn)).getAsLong();
            biliPlaybackData.qn=qn;
            biliPlaybackData.sizeA= (long) (audio.get("bandwidth").getAsLong()*duration/8f);
            result.add(biliPlaybackData);
        }
        return Tuples.of(asJsonObject.toString(),result);
    }
    public static Tuple2<String,List<BiliPlaybackData>> getMp4PlaybackData(HttpClient httpClient, String type, String vid, String cid) throws IOException, InterruptedException {
        var imgKey = "";
        var subKey = "";

        {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.bilibili.com/x/web-interface/nav"))
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject asJsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject wbi_img = asJsonObject.get("data").getAsJsonObject().get("wbi_img").getAsJsonObject();
            imgKey= wbi_img.get("img_url").getAsString();
            subKey= wbi_img.get("sub_url").getAsString();
            Pattern pattern = Pattern.compile("/([a-f0-9]{32})\\.png$");
            Matcher matcherImgKey = pattern.matcher(imgKey);
            Matcher matcherSubKey = pattern.matcher(subKey);
            if (matcherImgKey.find()) {
                imgKey = matcherImgKey.group(1);
            }
            if (matcherSubKey.find()) {
                subKey = matcherSubKey.group(1);
            }
        }
        var param = WbiUtil.getParam(Map.of(
                "cid",cid,
                type,vid,
                "fnval",1,
                "qn","127"
        ),imgKey,subKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.bilibili.com/x/player/playurl?"+param))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject asJsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray accept_quality = asJsonObject.get("data").getAsJsonObject().get("accept_quality").getAsJsonArray();
        JsonObject sizes = new JsonObject();
        var result = new ArrayList<BiliPlaybackData>();
        for (JsonElement jsonElement : accept_quality) {
            var param1 = WbiUtil.getParam(Map.of(
                    "cid",cid,
                    type,vid,
                    "fnval",1,
                    "qn",jsonElement.getAsInt()
            ),imgKey,subKey);
            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.bilibili.com/x/player/playurl?"+param1))
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response1 = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject asJsonObject1 = JsonParser.parseString(response1.body()).getAsJsonObject().get("data").getAsJsonObject();
            var vo = asJsonObject1.get("durl").getAsJsonArray().get(0).getAsJsonObject();
            sizes.addProperty(String.valueOf(asJsonObject1.get("quality").getAsInt()),vo.get("size").getAsLong());
            var bpd = new BiliPlaybackData();
            bpd.isDash=false;
            bpd .qn=asJsonObject1.get("quality").getAsInt();
            bpd.size=vo.get("size").getAsLong();
            bpd.videoUrl=vo.get("url").getAsString();
            result.add(bpd);
        }

        asJsonObject.add("sizes",sizes);

        return Tuples.of(asJsonObject.toString(),result);
    }
}
