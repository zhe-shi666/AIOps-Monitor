package com.aiops.monitor.collector;

import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

@Component
public class SystemHardwareCollector {

    private final SystemInfo si = new SystemInfo();
    private final HardwareAbstractionLayer hal = si.getHardware();

    /**
     * 获取实时 CPU 使用率
     */
    // 推荐的 CPU 采集逻辑（在类中维护 prevTicks 变量）
    private long[] prevTicks = new long[CentralProcessor.TickType.values().length];

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
