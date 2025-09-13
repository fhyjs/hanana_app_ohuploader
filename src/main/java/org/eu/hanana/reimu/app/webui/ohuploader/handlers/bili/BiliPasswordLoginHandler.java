package org.eu.hanana.reimu.app.webui.ohuploader.handlers.bili;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eu.hanana.reimu.app.webui.ohuploader.Util;
import org.eu.hanana.reimu.app.webui.ohuploader.util.RsaEncryptUtil;
import org.eu.hanana.reimu.webui.handler.AbstractEasyPathHandler;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BiliPasswordLoginHandler extends AbstractEasyPathHandler {
    @Override
    protected String getPath() {
        return "/data/ohupd/bili/pw_login.json";
    }

    @Override
    public Publisher<Void> process(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        return Util.autoContentType(httpServerResponse).sendString(Mono.create(stringMonoSink -> {
            // 解析请求 body
            JsonObject data = JsonParser.parseString(
                    Objects.requireNonNull(
                            httpServerRequest.receive().aggregate().asString(StandardCharsets.UTF_8).block()
                    )
            ).getAsJsonObject();
            OkHttpClient client = new OkHttpClient();
            var pwd =  data.get("password").getAsString();
            try {
                pwd = RsaEncryptUtil.encryptHashWithPublicKey(pwd,data.get("key").getAsString());
            } catch (Exception e) {
                stringMonoSink.error(e);
            }
            // 构建表单参数
            FormBody formBody = new FormBody.Builder()
                    .add("username", data.get("username").getAsString())
                    .add("password",pwd)
                    .add("token", data.get("token").getAsString())
                    .add("challenge", data.get("challenge").getAsString())
                    .add("validate", data.get("validate").getAsString())
                    .add("seccode", data.get("seccode").getAsString())
                    .add("keep", "0")
                    .build();

            // 构建请求
            Request request = new Request.Builder()
                    .url("https://passport.bilibili.com/x/passport-login/web/login")
                    .post(formBody)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
                    .build();

            // 发送请求
            try (Response response = client.newCall(request).execute()) {
                // 构建返回 JSON
                JsonObject result = new JsonObject();
                result.addProperty("body", response.body().string());

                JsonObject head = new JsonObject();
                for (Map.Entry<String, List<String>> entry : response.headers().toMultimap().entrySet()) {
                    JsonArray arr = new JsonArray();
                    for (String v : entry.getValue()) arr.add(v);
                    head.add(entry.getKey(), arr);
                }
                result.add("head", head);

                stringMonoSink.success(result.toString());
            } catch (IOException e) {
                stringMonoSink.error(e);
            }
        }));
    }
}
