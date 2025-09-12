package org.eu.hanana.reimu.app.webui.ohuploader.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eu.hanana.reimu.app.webui.ohuploader.batch.BatchManager;
import org.eu.hanana.reimu.app.webui.ohuploader.batch.BatchStatus;
import org.eu.hanana.reimu.app.webui.ohuploader.batch.BatchTask;
import org.eu.hanana.reimu.app.webui.ohuploader.batch.BatchWrapper;
import org.eu.hanana.reimu.app.webui.ohuploader.config.ConfigCore;
import org.eu.hanana.reimu.webui.handler.AbstractEasyPathHandler;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CreateBatchHandler extends AbstractEasyPathHandler {
    private static final Logger log = LogManager.getLogger(CreateBatchHandler.class);

    @Override
    protected String getPath() {
        return "/data/ohupd/batch_create.json";
    }

    @Override
    public Publisher<Void> process(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        return httpServerResponse.header("content-type","application/json; charset=utf-8").sendString(Mono.create(stringMonoSink -> {
            var uid = webUi.getSessionManage().getUser(httpServerRequest).data.get("ottohub_account").getAsJsonObject().get("uid").getAsInt();
            if (!BatchManager.getInstance().batchWorkFlow.containsKey(uid)){
                BatchManager.getInstance().batchWorkFlow.put(uid,new ArrayList<>());
            }
            List<BatchWrapper> batchWrappers = new ArrayList<>();
            for (BatchWrapper batchWrapper : BatchManager.getInstance().batchWorkFlow.get(uid)) {
                if (batchWrapper.status!= BatchStatus.NEW&&batchWrapper.status!=BatchStatus.WORKING&&batchWrapper.status!=BatchStatus.WAIT){
                    continue;
                }
                batchWrappers.add(batchWrapper);
            }
            JsonObject data = JsonParser.parseString(Objects.requireNonNull(httpServerRequest.receive().aggregate().asString(StandardCharsets.UTF_8).block())).getAsJsonObject();
            if (data.has("debug_clear")&&data.get("debug_clear").getAsBoolean()){
                batchWrappers.clear();
            }
            var allUserTasks = new ArrayList<>(BatchManager.getInstance().batchWorkFlow.get(uid));
            for (BatchWrapper batchWrapper : allUserTasks) {
                if (batchWrapper.status!= BatchStatus.NEW&&batchWrapper.status!=BatchStatus.WORKING&&batchWrapper.status!=BatchStatus.WAIT){
                    BatchManager.getInstance().batchWorkFlow.get(uid).remove(batchWrapper);
                    log.info("[{}] Removed finished task {}",uid,batchWrapper);
                }
            }
            // check
            if (batchWrappers.size()>= ConfigCore.Config.max_batch_count){
                throw new IllegalStateException("您最多只能创建"+ConfigCore.Config.max_batch_count+"个批处理。");
            }
            if (BatchManager.getInstance().getAllBatch().size()>= ConfigCore.Config.max_batch_global){
                throw new IllegalStateException("服务器最大承载"+ConfigCore.Config.max_batch_count+"个批处理，请等待其他用户处理完成。");
            }
            if (data.get("targets").getAsJsonArray().size()>= ConfigCore.Config.max_batch_task_count){
                throw new IllegalStateException("每个任务最多"+ConfigCore.Config.max_batch_task_count+"个项目，请慢一点。");
            }
            var task = new BatchWrapper();
            task.owner=uid;
            task.user=webUi.getSessionManage().getUser(httpServerRequest);
            if (!data.has("catalog")){
                throw new IllegalArgumentException("没选分区");
            }
            task.catalog=data.get("catalog").getAsInt();
            task.tasks=new ArrayList<>();
            task.onTagOverflow=data.get("on_tag_flow").getAsString();
            for (JsonElement jsonElement : data.get("targets").getAsJsonArray()) {
                var taskItem = new BatchTask();
                taskItem.rawSource=jsonElement.getAsString();
                task.tasks.add(taskItem);
            }
            BatchManager.getInstance().batchWorkFlow.get(uid).add(task);
            task.start();
            stringMonoSink.success("{\"status\":\"success\"}");
        }));
    }
}
