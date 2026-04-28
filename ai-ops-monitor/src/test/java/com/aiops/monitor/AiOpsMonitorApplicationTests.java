package com.aiops.monitor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiOpsMonitorApplicationTests {

    @Test
    void shouldKeepApplicationEntryPointAvailable() {
        assertEquals("AiOpsMonitorApplication", AiOpsMonitorApplication.class.getSimpleName());
    }
}
