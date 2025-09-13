package org.eu.hanana.reimu.app.webui.ohuploader.util;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

public class RsaEncryptUtil {

    /**
     * 使用 PEM 公钥加密字符串，并返回 Base64
     * @param s 待加密字符串
     * @param publicKeyPem 公钥 PEM 格式
     * @return Base64 加密结果
     */
    public static String encryptHashWithPublicKey(String s, String publicKeyPem) throws Exception {
        // 去掉 PEM 包装
        String pem = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        // 解码 Base64
        byte[] keyBytes = Base64.getDecoder().decode(pem);

        // 生成公钥对象
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);

        // B 站使用 RSA/ECB/PKCS1Padding (1024 位公钥)
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        // 直接加密明文密码
        byte[] encrypted = cipher.doFinal(s.getBytes(StandardCharsets.UTF_8));

        // 转 Base64 返回
        return Base64.getEncoder().encodeToString(encrypted);
    }

    // 测试
    public static void main(String[] args) throws Exception {
        String hash = "475b1f7e99e4ca86";
        String pubKey = """
                -----BEGIN PUBLIC KEY-----
                MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDjb4V7EidX/ym28t2ybo0U6t0n
                6p4ej8VjqKHg100va6jkNbNTrLQqMCQCAYtXMXXp2Fwkk6WR+12N9zknLjf+C9sx
                /+l48mjUU8RqahiFD1XT/u2e0m2EN029OhCgkHx3Fc/KlFSIbak93EH/XlYis0w+
                Xl69GV6klzgxW6d2xQIDAQAB
                -----END PUBLIC KEY-----""";

        String encryptedBase64 = encryptHashWithPublicKey(hash, pubKey);
        System.out.println("加密结果 Base64: " + encryptedBase64);
    }
}
