package com.aiops.monitor.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class PrometheusResponse {
    private String status;
    private DataContent data;

    @Data
    public static class DataContent {
        private String resultType;
        private List<Result> result;
    }

    @Data
    public static class Result {
        private Object metric; // 包含标签信息
        private List<Object> value; // [timestamp, value]
    }
}