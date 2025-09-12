package org.eu.hanana.reimu.app.webui.ohuploader.util;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class BvAvConverter {
    private static final String table = "fZodR9XQDSUm21yCkr6zBqiveYah8bt4xsWpHnJE7jL5VG3guMTKNPAwcF";
    private static final Map<Character, Integer> tr = new HashMap<>();
    private static final int[] s = {11, 10, 3, 8, 4, 6};
    private static final BigInteger xor = BigInteger.valueOf(177451812L);
    private static final BigInteger add = new BigInteger("8728348608");

    static {
        for (int i = 0; i < table.length(); i++) {
            tr.put(table.charAt(i), i);
        }
    }

    public static BigInteger dec(String bv) {
        BigInteger r = BigInteger.ZERO;
        for (int i = 0; i < 6; i++) {
            r = r.add(BigInteger.valueOf(tr.get(bv.charAt(s[i])))
                    .multiply(BigInteger.valueOf(58).pow(i)));
        }
        return r.subtract(add).xor(xor);
    }

    public static String enc(BigInteger av) {
        BigInteger x = av.xor(xor).add(add);
        char[] r = "BV1  4 1 7  ".toCharArray();
        for (int i = 0; i < 6; i++) {
            BigInteger div = x.divide(BigInteger.valueOf(58).pow(i));
            int idx = div.mod(BigInteger.valueOf(58)).intValue();
            r[s[i]] = table.charAt(idx);
        }
        return new String(r);
    }

    public static void main(String[] args) {
        // BV → AV
        System.out.println(dec("BV17x411w7KC"));
        System.out.println(dec("BV1Q541167Qg"));
        System.out.println(dec("BV1mK4y1C7Bz"));

        // AV → BV
        System.out.println(enc(new BigInteger("12")));
        System.out.println(enc(new BigInteger("455017605")));
        System.out.println(enc(new BigInteger("882584971")));

        // 测试超级大数
        System.out.println(enc(new BigInteger("115183120875749")));
    }
}
