package org.eu.hanana.reimu.app.webui.ohuploader.config;

import org.eu.hanana.reimu.app.mod.webui.config.HasName;
import org.eu.hanana.reimu.hnnapp.mods.CfgCoreBase;

public class ConfigCore extends CfgCoreBase {
    @Override
    public void init() {
        addCfgClass(Config.class);
    }
    @HasName("OTTOHUB转载工具设置")
    public static class Config{
        public static String upload_backend = "https://ottohub.cn/src/send/php/send_video.php";
    }
}
