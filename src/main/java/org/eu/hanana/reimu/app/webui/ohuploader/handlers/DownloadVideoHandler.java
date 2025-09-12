package org.eu.hanana.reimu.app.webui.ohuploader.handlers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import okhttp3.*;

import org.apache.logging.log4j.LogManager;
import org.eu.hanana.reimu.app.webui.ohuploader.Util;
import org.eu.hanana.reimu.app.webui.ohuploader.config.ConfigCore;
import org.eu.hanana.reimu.app.webui.ohuploader.util.ProgressedRequestBody;
import org.eu.hanana.reimu.app.webui.ohuploader.util.TimerLoopThread;
import org.eu.hanana.reimu.webui.handler.AbstractEasyPathHandler;
import org.eu.hanana.reimu.webui.handler.AbstractPathHandler;
import org.eu.hanana.reimu.webui.session.User;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.WebsocketServerSpec;
import reactor.netty.http.websocket.WebsocketOutbound;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.eu.hanana.reimu.app.webui.ohuploader.util.SystemInfoGenerator.generateSystemInfo;

public class DownloadVideoHandler extends AbstractEasyPathHandler {
    @Override
    protected String getPath() {
        return "/data/ohupd/do_upload";
    }

    @Override
    public Publisher<Void> process(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        return httpServerResponse.sendWebsocket((inbound, outbound) -> Mono.<Void>create(voidMonoSink -> {
            final Runner runner = new Runner(new OutCtrl() {
                @Override
                public void sendString(String string) {
                    outbound.sendString(Mono.just(string)).then().subscribe();
                }

                @Override
                public void sendClose() {
                    outbound.sendClose().subscribe();
                }
            }, webUi.getSessionManage().getUser(httpServerRequest));
            var tmp = new StringBuilder();
            inbound.receive()  // 获取接收到的帧
                    .map(TextWebSocketFrame::new)
                    .doOnNext(frame -> {
                        var text = frame.text();
                        if (tmp.toString().isEmpty()&&text.startsWith("{")&&!text.endsWith("}")){
                            tmp.append(text);
                        }else if (!tmp.toString().isEmpty()&&!text.endsWith("}")&&!text.startsWith("{")){
                            tmp.append(text);
                        }else if (!tmp.toString().isEmpty()&&text.endsWith("}")&&!text.startsWith("}")){
                            tmp.append(text);
                            runner.input(JsonParser.parseString(tmp.toString()));
                            tmp.delete(0,tmp.length()-1);
                        }else if (text.startsWith("{")&&text.endsWith("}")){
                            runner.input(JsonParser.parseString(text));
                        }else {
                            voidMonoSink.success();
                        }
                        //runner.input(JsonParser.parseString(message.text()));
                    }).doOnError(throwable -> {
                        throw new RuntimeException(throwable);
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();
        }).subscribeOn(Schedulers.boundedElastic()), WebsocketServerSpec.builder().maxFramePayloadLength(2097152).build());
    }
    public interface OutCtrl{
        void sendString(String string);
        void sendClose();
    }
    public static class Runner implements Runnable{
        private final OutCtrl outCtrl;
        private final User user;
        private JsonObject args = null;
        public Throwable error = null;
        public Runner(OutCtrl outCtrl, User user){
            this.outCtrl=outCtrl;
            sendOpString("msg",generateSystemInfo().replace("\n","<br/>"));
            outCtrl.sendString("{\"op\":\"status\",\"data\":\"waiting\"}");
            outCtrl.sendString(String.format("{\"op\":\"msg\",\"data\":\"%s\"}","server:等待客户端发送数据"));
            sendOpString("start_pre","");
            this.user=user;
        }

        public void input(JsonElement jsonElement) {
            var jo = jsonElement.getAsJsonObject();
            var op = jo.get("op").getAsString();
            if (op.equals("start")){
                if (args!=null) return;
                args=jo.get("data").getAsJsonObject();
                new Thread(this).start();
            }
        }
        protected void sendOpString(String op,String data){
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("op",op);
            jsonObject.addProperty("data",data);
            outCtrl.sendString(jsonObject.toString());
        }

        @Override
        public void run() {
            var tmpFiles = new ArrayList<File>();
            try {
                sendOpString("status","准备中");
                sendOpString("progress","99%");
                sendOpString("msg","数据已接收");
                sendOpString("msg","avid: "+args.get("aid").getAsString());
                sendOpString("msg","cid: "+args.get("cid").getAsString());
                sendOpString("msg","清晰度: "+args.get("qn").getAsString());
                BufferedImage pic = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(args.get("pic").getAsString().split(",")[1])));
                var title = args.get("title").getAsString();
                var desc = args.get("desc").getAsString();
                var qn = args.get("qn").getAsString();
                var tags = new HashSet<String>();
                for (JsonElement jsonElement : args.get("tags").getAsJsonArray()) {
                    tags.add(jsonElement.getAsString());
                }
                var punctuationMarks = new char[]{
                        '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '^', '_', '`', '{', '|', '}', '~', '—',
                        '！', '“', '”', '＃', '￥', '％', '＆', '’', '（', '）', '＊', '＋', '，', '－', '．', '／', '：', '；', '＜', '＝', '＞', '？', '＠', '［', '＼', '］', '＾', '＿', '｀', '｛', '｜', '｝', '～',
                        '。', '，', '！', '；', '：', '（', '）', '［', '］', '｛', '｝', '⋯', '﹐', '﹑', '。', '、', '〃', '〝', '〞', '〟', '﹔', '﹕', '﹖', '﹗', '「', '」', '『', '』', '【', '】', '〝', '〞',
                        '\u2000', '\u2001', '\u2002', '\u2003', '\u2004', '\u2005', '\u2006', '\u2007', '\u2008', '\u2009', '\u200A', '\u200B', '\u2028', '\u2029', '\u202F', '\u205F', '\u2060'
                };
                if (tags.size()>10){
                    throw new IllegalArgumentException("标签数量限制笑传之超超标");
                }
                for (String tag : tags) {
                    if (tag.isBlank()) throw new IllegalArgumentException("有空标签");
                    if (tag.length()>20) throw new IllegalArgumentException("那我希望你的退役时间和你的标签一样长");
                    for (char c : punctuationMarks) {
                        if (tag.indexOf(c) != -1) {
                            throw new IllegalArgumentException("标签有非法字符\\n牢内");
                        }
                    }
                }
                if (title.length()>=ConfigCore.Config.title_length)throw new IllegalArgumentException("标题太长了");
                if (desc.length()>=ConfigCore.Config.intro_length)throw new IllegalArgumentException("简介太长了");
                var picImage = new ByteArrayOutputStream();
                ImageIO.write(pic,"jpg",picImage);
                if (picImage.size()> 1048576) throw new IllegalArgumentException("封面过大");
                sendOpString("msg","封面(bytes): "+picImage.size());
                if (!args.has("right")) throw new IllegalArgumentException("未选择版权");
                var right = String.valueOf(args.get("right").getAsInt());
                if (!args.has("type")) throw new IllegalArgumentException("未选择类型");
                var type = String.valueOf(args.get("type").getAsInt());
                sendOpString("msg","获取视频元数据...");
                String vurl;
                long size;
                try(var httpClient = HttpClient.newHttpClient()){
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.bilibili.com/x/player/playurl?cid="+args.get("cid").getAsString()+"&avid="+args.get("aid").getAsString()+"&fnval=1&qn="+qn))
                            .method("GET", HttpRequest.BodyPublishers.noBody())
                            .build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    JsonObject asJsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
                    vurl = asJsonObject.get("data").getAsJsonObject().get("durl").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                    size = asJsonObject.get("data").getAsJsonObject().get("durl").getAsJsonArray().get(0).getAsJsonObject().get("size").getAsBigInteger().longValue();
                }
                sendOpString("msg",vurl);
                sendOpString("status", "下载视频");
                sendOpString("msg", "文件大小: "+size);
                if (size> 104857600 * 2) throw new IllegalStateException("视频大于200MB,请降低清晰度或手动上传");
                try(var httpClient = HttpClient.newHttpClient()){
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(vurl))
                            .method("GET", HttpRequest.BodyPublishers.noBody())
                            .header("referer","https://www.bilibili.com/")
                            .header("origin","https://www.bilibili.com")
                            .header("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
                            .build();
                    HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    // 获取文件总大小（如果服务器支持）
                    // 可能返回 -1，表示未知大小
                    var dir= new File("tmp/ohupd");
                    if (!dir.exists()) dir.mkdirs();
                    var file = new File(dir, Util.generateRandomString(10)+".mp4");
                    tmpFiles.add(file);
                    try (InputStream inputStream = response.body();
                         FileOutputStream outputStream = new FileOutputStream(file)) {

                        byte[] buffer = new byte[524288]; // 8KB 缓冲区
                        long downloaded = 0;
                        int bytesRead;
                        long startTime = System.currentTimeMillis();
                        var progress = new AtomicReference<>(0d);
                        Thread listener = new TimerLoopThread<>(progress, pg -> {
                            sendOpString("progress", String.valueOf(pg*100));
                            return pg >= 1d;
                        }, 100);
                        listener.start();
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            downloaded += bytesRead;

                            // 计算进度
                            progress.set((downloaded * 1d) / size);
                        }
                        listener.interrupt();
                        long totalTime = System.currentTimeMillis() - startTime;
                        sendOpString("msg", "下载完成,用时(ms): "+totalTime);
                    }
                    sendOpString("status", "上传中");
                    sendOpString("msg", "开始上传");
                    sendOpString("msg", "远程地址: "+ConfigCore.Config.upload_backend);
                    OkHttpClient client = new OkHttpClient.Builder()
                            .readTimeout(1, TimeUnit.MINUTES)
                            .build();
                    var tagstr = new StringBuilder();
                    for (String tag : tags) {
                        tagstr.append("#").append(tag);
                    }
                    JsonObject asJsonObject = user.data.get("ottohub_account").getAsJsonObject();
                    // 构造 Multipart 表单
                    MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("title", title)
                            .addFormDataPart("intro", desc)
                            .addFormDataPart("token", asJsonObject.get("token").getAsString())
                            .addFormDataPart("type", right)
                            .addFormDataPart("action", "submit_video")
                            .addFormDataPart("category", type)
                            .addFormDataPart("tag", tagstr.toString());
                    multipartBuilder.addFormDataPart("file_jpg", Util.generateRandomString(10)+".jpg", RequestBody.create(picImage.toByteArray(), MediaType.get("image/jpeg")));
                    multipartBuilder.addFormDataPart("file_mp4", Util.generateRandomString(10)+".mp4", RequestBody.create(file, MediaType.get("video/mp4")));

                    RequestBody requestBody = multipartBuilder.build();
                    ProgressedRequestBody requestBody1 = null;
                    // 构造请求
                    Request okHrequest = new Request.Builder()
                            .url(ConfigCore.Config.upload_backend)
                            .post(requestBody1=new ProgressedRequestBody(requestBody,(written, length, progress) -> {}))

                            //.addHeader("Cookie", String.format("login_token=%s; PHPSESSID=%s",asJsonObject.get("token").getAsString(),asJsonObject.get("PHPSESSID").getAsString())) // 替换为实际 token
                            .build();
                    ProgressedRequestBody finalRequestBody = requestBody1;
                    Thread listener = new TimerLoopThread<>(new AtomicReference<>(finalRequestBody), progressedRequestBody -> {
                        double progress = progressedRequestBody.getProgress();
                        sendOpString("progress", String.valueOf(progress*100));
                        return progress >= 1;
                    }, 100);
                    listener.start();
                    // 发送请求
                    try (Response okHresponse = client.newCall(okHrequest).execute()) {
                        listener.interrupt();
                        if (okHresponse.isSuccessful()) {
                            String string = okHresponse.body().string();
                            sendOpString("msg",string);
                            if (!string.contains("success")){
                                throw new IllegalStateException("远程返回: "+string);
                            }else {
                                sendOpString("success","ok");
                            }
                        } else {
                            System.out.println("Request failed: " + okHresponse.message());
                        }
                    }
                }

            }catch (Throwable e){
                sendOpString("error", Base64.getEncoder().encodeToString(e.toString().getBytes(StandardCharsets.UTF_8)));
                sendOpString("status", "发生错误,按返回上一步调整设置");
                e.printStackTrace();
                error=e;
            }finally {
                outCtrl.sendClose();
                for (File tmpFile : tmpFiles) {
                    if (tmpFile.exists()){
                        tmpFile.delete();
                    }
                }
            }
        }
    }
}
