package com.aiops.monitor.collector;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class MetricsExporter {

    private final AtomicReference<Double> cpuUsage = new AtomicReference<>(0.0);
    private final AtomicReference<Double> memUsage = new AtomicReference<>(0.0);

    public MetricsExporter(MeterRegistry registry) {
        // 注册 CPU 指标到 Prometheus 注册表
        Gauge.builder("system_cpu_usage", cpuUsage, AtomicReference::get)
                .description("系统实时CPU使用率")
                .register(registry);

        // 注册内存指标到 Prometheus 注册表
        Gauge.builder("system_mem_usage", memUsage, AtomicReference::get)
                .description("系统实时内存使用率")
                .register(registry);
    }

    public void updateMetrics(double cpu, double mem) {
        this.cpuUsage.set(cpu);
        this.memUsage.set(mem);
    }
}