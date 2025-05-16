package org.eu.hanana.reimu.app.webui.ohuploader.util;

import java.util.Locale;

public enum OSType {
    WINDOWS("dll"),
    LINUX("so"),
    MACOS("dylib");
   public String linkFileExt;
   OSType(String linkExt){
       this.linkFileExt=linkExt;
   }
    public static OSType getOS() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) {
            return OSType.WINDOWS;
        } else if (osName.contains("mac")) {
            return OSType.MACOS;
        } else if (osName.contains("nix") || osName.contains("nux")) {
            return OSType.LINUX;
        }
        throw new UnsupportedOperationException("Unsupported OS: " + osName);
    }
}