package org.eu.hanana.reimu.app.webui.ohuploader.batch;

import java.util.ArrayList;
import java.util.List;

public class BatchTask {
    public BatchStatus status = BatchStatus.NEW;
    public String rawSource;
    public String avid;
    public List<String> logBuffer = new ArrayList<>();
}
