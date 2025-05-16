package org.eu.hanana.reimu.app.webui.ohuploader;

import org.eu.hanana.reimu.app.webui.ohuploader.util.OSType;
import reactor.netty.http.server.HttpServerResponse;

import java.io.*;
import java.net.*;
import java.net.http.HttpResponse;
import java.util.*;

public class Util extends org.eu.hanana.reimu.app.mod.webui.Util {

    // 添加普通文本字段
    public static void addFormField(PrintWriter writer, String boundary, String name, String value) {
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n");
        writer.append("Content-Type: text/plain; charset=UTF-8\r\n\r\n");
        writer.append(value).append("\r\n");
        writer.flush();
    }
    public static Map<String,String> parseCookies(HttpResponse<?> response){
        Map<String,String> stringStringMap = new HashMap<>();
        for (String cookieHeader : response.headers().allValues("Set-Cookie")) {
            List<HttpCookie> httpCookies = HttpCookie.parse(cookieHeader);
            for (HttpCookie cookie : httpCookies) {
                stringStringMap.put(cookie.getName(),cookie.getValue());
            }
        }
        return stringStringMap;
    }
    // 添加文件字段
    public static void addFilePart(OutputStream output, PrintWriter writer, String boundary, String fieldName, File uploadFile) throws IOException {
        String fileName = uploadFile.getName();
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"").append(fieldName)
                .append("\"; filename=\"").append(fileName).append("\"\r\n");
        writer.append("Content-Type: ").append("application/octet-stream").append("\r\n\r\n");
        writer.flush();

        // 读取文件数据并写入
        try (FileInputStream inputStream = new FileInputStream(uploadFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            output.flush();
        }
        writer.append("\r\n");
        writer.flush();
    }
    // 下载文件并转换为 Base64 带 MIME 类型
    public static String downloadFileToBase64(String fileUrl) throws Exception {
        // 下载文件到字节数组
        byte[] fileBytes = downloadFileAsBytes(fileUrl);

        // 转换为 Base64
        String base64 = Base64.getEncoder().encodeToString(fileBytes);

        // 获取 MIME 类型
        String mimeType = getMimeType(fileUrl);

        // 生成 data URL
        return "data:" + mimeType + ";base64," + base64;
    }

    // 下载文件到字节数组
    public static byte[] downloadFileAsBytes(String fileUrl) throws Exception {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (InputStream inputStream = connection.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        }
    }

}
