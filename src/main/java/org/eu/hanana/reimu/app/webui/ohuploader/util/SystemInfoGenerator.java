package org.eu.hanana.reimu.app.webui.ohuploader.util;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class SystemInfoGenerator {
    // 在原有代码基础上添加以下方法
    private static String getCpuUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            double cpuLoad = osBean.getSystemCpuLoad() * 100;
            
            // 首次调用可能返回-1，需要二次尝试
            if (cpuLoad < 0) {
                Thread.sleep(500);
                cpuLoad = osBean.getSystemCpuLoad() * 100;
            }
            
            return (cpuLoad >= 0) ? String.format("%.1f%%", cpuLoad) : "N/A";
        } catch (Exception e) {
            return "获取失败";
        }
    }
    public static String generateSystemInfo() {
        StringBuilder sb = new StringBuilder();
        Runtime runtime = Runtime.getRuntime();
        DecimalFormat df = new DecimalFormat("#,##0.0");

        // 操作系统信息
        sb.append("========== 系统信息 ==========\n");
        sb.append(String.format("操作系统: %s %s%n",
                System.getProperty("os.name"),
                System.getProperty("os.version")));
        sb.append(String.format("系统架构: %s%n", System.getProperty("os.arch")));

        // Java 环境信息
        sb.append("\n========== Java 环境 ==========\n");
        sb.append(String.format("Java 版本: %s%n", System.getProperty("java.version")));
        sb.append(String.format("JVM 名称: %s%n", System.getProperty("java.vm.name")));

        // 内存信息
        sb.append("\n========== 内存信息 ==========\n");
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        sb.append(String.format("总内存: %s MB%n", bytesToMB(totalMemory, df)));
        sb.append(String.format("已用内存: %s MB%n", bytesToMB(usedMemory, df)));
        sb.append(String.format("空闲内存: %s MB%n", bytesToMB(freeMemory, df)));
        sb.append(String.format("最大可用内存: %s MB%n", bytesToMB(maxMemory, df)));

        // 处理器信息
        sb.append("\n========== 处理器信息 ==========\n");
        sb.append(String.format("处理器核心数: %d%n", runtime.availableProcessors()));
        sb.append(String.format("系统CPU占用率: %s%n", getCpuUsage()));

        // 系统运行时间
        sb.append("\n========== 运行时间 ==========\n");
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        long uptime = rb.getUptime();
        sb.append(formatUptime(uptime));

        // 时间戳
        sb.append("\n========== 当前时间 ==========\n");
        sb.append(new Date());

        return sb.toString();
    }

    private static String bytesToMB(long bytes, DecimalFormat df) {
        return df.format(bytes / (1024.0 * 1024.0));
    }

    private static String formatUptime(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%d 天 %d 小时 %d 分 %d 秒", days, hours, minutes, seconds);
    }

    public static void main(String[] args) {
        System.out.println(generateSystemInfo());
    }
    // 在generateSystemInfo()的处理器部分添加：
}