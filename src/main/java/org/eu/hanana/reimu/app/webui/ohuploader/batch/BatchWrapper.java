package org.eu.hanana.reimu.app.webui.ohuploader.batch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eu.hanana.reimu.app.webui.ohuploader.Util;
import org.eu.hanana.reimu.app.webui.ohuploader.config.ConfigCore;
import org.eu.hanana.reimu.app.webui.ohuploader.handlers.DownloadVideoHandler;
import org.eu.hanana.reimu.webui.session.User;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class BatchWrapper {
    private static final Logger logger = LogManager.getLogger("BatchWrapper");
    public String onTagOverflow="error";
    public Object owner;
    public int catalog;
    public User user;
    public BatchStatus status = BatchStatus.NEW;
    public List<BatchTask> tasks = new ArrayList<>();

    public void start() {
        Thread thread = new Thread(() -> {
            status=BatchStatus.WORKING;
            for (BatchTask task : tasks) {
                if (task.status!=BatchStatus.NEW) continue;
                try{
                    startTask(task);
                } catch (Exception e) {
                    e.printStackTrace();
                    task.logBuffer.add(e.toString());
                    task.status=BatchStatus.ERROR;
                }
            }
            status=BatchStatus.FINISH;
        });
        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
                status=BatchStatus.ERROR;
            }
        });
        thread.start();
    }
    protected JsonObject getTagInfo(String aid){
        try (HttpClient httpClient = HttpClient.newHttpClient()){
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.bilibili.com/x/tag/archive/tags?aid="+aid))
                    //.header("Referer", "https://bilibili.com/")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    protected JsonObject getVidInfo(String type,String vid){
        try (HttpClient httpClient = HttpClient.newHttpClient()){
            type = type.equals("av")?"aid":"bvid";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.bilibili.com/x/web-interface/view?"+type+"="+vid))
                    .header("Referer", "https://bilibili.com/")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonElement jsonElement = JsonParser.parseString(response.body());
            if (jsonElement.getAsJsonObject().has("data")) {
                jsonElement.getAsJsonObject().get("data").getAsJsonObject().addProperty("pic", Util.downloadFileToBase64(jsonElement.getAsJsonObject().get("data").getAsJsonObject().get("pic").getAsString()));
            }

            return jsonElement.getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    protected void startTask(BatchTask task) {
        var uid = user.data.get("ottohub_account").getAsJsonObject().get("uid").getAsInt();
        var type = task.rawSource.substring(0,2);
        var vid = task.rawSource.substring(2);
        var vData = getVidInfo(type,vid).getAsJsonObject().get("data").getAsJsonObject();
        var tData = getTagInfo(vData.get("aid").getAsString()).get("data").getAsJsonArray();
        var runner = new DownloadVideoHandler.Runner(new DownloadVideoHandler.OutCtrl() {
            @Override
            public void sendString(String string) {
                task.logBuffer.add(string);
                logger.info("[{}] {}.", uid, string);
            }

            @Override
            public void sendClose() {
                logger.info("[{}] {}.", uid, "Closed!");
            }
        },user);
        var desc = String.format("""
               转载bilbili: AV%s
               up主: %s
               批量转载(https://5160.hanana2.link:2053/static/cp/webui/index.html?act=ohupd)
               原视频简介:
               %s""",vData.get("aid").getAsString(),vData.get("owner").getAsJsonObject().get("name").getAsString(),vData.get("desc").getAsString());
        desc=desc.substring(0, Math.min(ConfigCore.Config.intro_length-1,desc.length()));
        JsonObject coreData = new JsonObject();
        coreData.add("aid",vData.get("aid"));
        coreData.add("cid",vData.get("cid"));
        coreData.add("pic",vData.get("pic"));
        coreData.addProperty("qn",64);
        coreData.add("title",vData.get("title"));
        coreData.addProperty("desc",desc);
        var punctuationMarks = new char[]{
                '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_', '`', '{', '|', '}', '~', '—',
                '！', '“', '”', '＃', '￥', '％', '＆', '’', '（', '）', '＊', '＋', '，', '－', '．', '／', '：', '；', '＜', '＝', '＞', '？', '＠', '［', '＼', '］', '＾', '＿', '｀', '｛', '｜', '｝', '～',
                '。', '，', '！', '；', '：', '（', '）', '［', '］', '｛', '｝', '⋯', '﹐', '﹑', '。', '、', '〃', '〝', '〞', '〟', '﹔', '﹕', '﹖', '﹗', '「', '」', '『', '』', '【', '】', '〝', '〞',
                '\u2000', '\u2001', '\u2002', '\u2003', '\u2004', '\u2005', '\u2006', '\u2007', '\u2008', '\u2009', '\u200A', '\u200B', '\u2028', '\u2029', '\u202F', '\u205F', '\u2060'
        };
        JsonArray tags = new JsonArray();
        for (JsonElement jsonElement : tData) {
            String asString = jsonElement.getAsJsonObject().get("tag_name").getAsString();
            for (char punctuationMark : punctuationMarks) {
                asString=asString.replace(String.valueOf(punctuationMark), "");
            }
            tags.add(asString);
        }
        coreData.add("tags",tags);
        coreData.addProperty("right",1);
        coreData.addProperty("type",catalog);


        JsonObject dataPending = new JsonObject();
        dataPending.addProperty("op","start");
        dataPending.add("data",coreData);
        runner.input(dataPending);
        runner.run();
        if (runner.error!=null){
            task.status=BatchStatus.ERROR;
        }else {
            task.status=BatchStatus.FINISH;
        }
    }
}
