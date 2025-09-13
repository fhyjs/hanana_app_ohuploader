package org.eu.hanana.reimu.app.webui.ohuploader.config;

import org.eu.hanana.reimu.app.mod.webui.config.HasName;
import org.eu.hanana.reimu.hnnapp.mods.CfgCoreBase;

import java.util.Map;

public class ConfigCore extends CfgCoreBase {
    @Override
    public void init() {
        addCfgClass(Config.class);
    }
    @HasName("OTTOHUB转载工具设置")
    public static class Config{
        public static String upload_backend = "https://api.ottohub.cn/module/creator/submit_video.php";
        public static String ffmpeg = "ffmpeg";
        public static int title_length = 44;
        public static int intro_length = 222;
        public static Map<String,Integer> video_catalogs = Map.of(
                "鬼畜",1,
                "人力VOCALOID",3,
                "剧场",4,
                "游戏",5,
                "怀旧",6,
                "音乐",7,
                "其他",0
        );
        public static int max_batch_count = 2;
        public static int max_batch_task_count = 5;
        public static int max_batch_global = 100;
    }
}
