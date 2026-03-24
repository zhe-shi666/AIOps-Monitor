/*
package com.aiops.monitor.collector;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class MetricsExporter {

    // 使用 AtomicReference 确保多线程更新时的可见性和原子性
    private final AtomicReference<Double> cpuUsage = new AtomicReference<>(0.0);
    private final AtomicReference<Double> memUsage = new AtomicReference<>(0.0);

    // 构造函数：只负责一次性的指标注册
    public MetricsExporter(MeterRegistry registry) {
        // 注册 CPU 指标
        // 这里的 system_cpu_usage 会出现在 /actuator/prometheus 中
        Gauge.builder("system_cpu_usage", cpuUsage, AtomicReference::get)
                .description("系统实时CPU使用率")
                .baseUnit("percent") // 加上单位更专业
                .register(registry);

        // 注册内存指标
        Gauge.builder("system_mem_usage", memUsage, AtomicReference::get)
                .description("系统实时内存使用率")
                .baseUnit("percent")
                .register(registry);
    }

    // 提供给 Collector 调用，更新内存中的值
    public void updateMetrics(double cpu, double mem) {
        this.cpuUsage.set(cpu);
        this.memUsage.set(mem);
    }
}*/
