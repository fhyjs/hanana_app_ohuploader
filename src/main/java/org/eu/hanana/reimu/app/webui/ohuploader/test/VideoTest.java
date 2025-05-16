package org.eu.hanana.reimu.app.webui.ohuploader.test;

import org.eu.hanana.reimu.app.webui.ohuploader.ffmpeg.Video;

public class VideoTest{
    public static void main(String[] args) {
        var video = new Video();
        video.setInput(args[0]);
        video.close();
    }
}
