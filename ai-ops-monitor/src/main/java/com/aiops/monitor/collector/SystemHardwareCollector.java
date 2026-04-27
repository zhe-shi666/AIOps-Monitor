package com.aiops.monitor.collector;

import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;

@Component
public class SystemHardwareCollector {

    private final SystemInfo si = new SystemInfo();
    private final HardwareAbstractionLayer hal = si.getHardware();

    /**
     * 获取实时 CPU 使用率
     */
    // 推荐的 CPU 采集逻辑（在类中维护 prevTicks 变量）
    private long[] prevTicks = new long[CentralProcessor.TickType.values().length];
    private long prevNetRxBytes = -1L;
    private long prevNetTxBytes = -1L;
    private long prevNetSampleTimeMs = -1L;

    public double getCpuUsage() {
        CentralProcessor processor = hal.getProcessor();
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        prevTicks = processor.getSystemCpuLoadTicks(); // 更新 ticks 给下一次使用
        return cpuLoad;
    }

    /**
     * 获取内存使用率
     */
    public double getMemoryUsage() {
        GlobalMemory memory = hal.getMemory();
        long total = memory.getTotal();
        long available = memory.getAvailable();
        return (double) (total - available) / total * 100;
    }

    /**
     * 获取磁盘整体使用率（按所有可用文件系统聚合）
     */
    public double getDiskUsage() {
        FileSystem fileSystem = si.getOperatingSystem().getFileSystem();
        long totalSpace = 0L;
        long usableSpace = 0L;

        for (OSFileStore store : fileSystem.getFileStores()) {
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();
            if (total <= 0L || usable < 0L) {
                continue;
            }
            totalSpace += total;
            usableSpace += Math.min(usable, total);
        }

        if (totalSpace <= 0L) {
            return 0D;
        }
        return ((double) (totalSpace - usableSpace) / totalSpace) * 100.0;
    }

    /**
     * 获取网络吞吐（bytes/s），返回 [rxBytesPerSec, txBytesPerSec]
     */
    public synchronized double[] getNetworkBytesPerSec() {
        long currentRx = 0L;
        long currentTx = 0L;
        for (NetworkIF networkIF : hal.getNetworkIFs()) {
            networkIF.updateAttributes();
            currentRx += Math.max(0L, networkIF.getBytesRecv());
            currentTx += Math.max(0L, networkIF.getBytesSent());
        }

        long now = System.currentTimeMillis();
        if (prevNetSampleTimeMs <= 0L || now <= prevNetSampleTimeMs || prevNetRxBytes < 0L || prevNetTxBytes < 0L) {
            prevNetSampleTimeMs = now;
            prevNetRxBytes = currentRx;
            prevNetTxBytes = currentTx;
            return new double[]{0D, 0D};
        }

        double seconds = (now - prevNetSampleTimeMs) / 1000.0;
        double rxPerSec = Math.max(0D, (currentRx - prevNetRxBytes) / seconds);
        double txPerSec = Math.max(0D, (currentTx - prevNetTxBytes) / seconds);

        prevNetSampleTimeMs = now;
        prevNetRxBytes = currentRx;
        prevNetTxBytes = currentTx;
        return new double[]{rxPerSec, txPerSec};
    }

    /**
     * 获取系统进程总数
     */
    public int getProcessCount() {
        return si.getOperatingSystem().getProcessCount();
    }

    /**
     * 获取系统运行时间 (秒)
     */
    public long getSystemUptime() {
        return si.getOperatingSystem().getSystemUptime();
    }

    /**
     * 获取 CPU 型号名称
     */
    public String getCpuModel() {
        return hal.getProcessor().getProcessorIdentifier().getName();
    }

    /**
     * 获取总内存（字节）
     */
    public long getTotalMemoryBytes() {
        return hal.getMemory().getTotal();
    }
}
