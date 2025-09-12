package org.eu.hanana.reimu.app.webui.ohuploader.batch;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BatchManager {
    private static BatchManager INSTANCE;
    @NotNull
    public static BatchManager getInstance() {
        if (INSTANCE==null){
            INSTANCE=new BatchManager();
        }
        return INSTANCE;
    }

    public final Map<Object, List<BatchWrapper>> batchWorkFlow= new HashMap<>();
    public List<BatchWrapper> getAllBatch(){
        var result =  new ArrayList<BatchWrapper>();
        var vals = new ArrayList<>(batchWorkFlow.values());
        for (List<BatchWrapper> val : vals) {
            result.addAll(val);
        }
        return result;
    }
    //public BatchWrapper createBW()
}
