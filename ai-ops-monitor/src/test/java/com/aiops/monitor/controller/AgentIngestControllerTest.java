package com.aiops.monitor.controller;

import com.aiops.monitor.model.dto.AgentHeartbeatRequest;
import com.aiops.monitor.model.entity.AlertEscalationPolicy;
import com.aiops.monitor.model.entity.MonitorTarget;
import com.aiops.monitor.model.entity.SystemMetricsHistory;
import com.aiops.monitor.repository.IncidentLogRepository;
import com.aiops.monitor.repository.MonitorTargetRepository;
import com.aiops.monitor.repository.SystemMetricsRepository;
import com.aiops.monitor.service.AnomalyDetectionService;
import com.aiops.monitor.service.EscalationPolicyService;
import com.aiops.monitor.service.InvestigationOrchestrator;
import com.aiops.monitor.service.MetricsPublisher;
import com.aiops.monitor.service.NotificationDispatcherService;
import com.aiops.monitor.service.TargetThresholdService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentIngestControllerTest {
    private final MonitorTargetRepository targetRepository = mock(MonitorTargetRepository.class);
    private final SystemMetricsRepository metricsRepository = mock(SystemMetricsRepository.class);
    private final IncidentLogRepository incidentRepository = mock(IncidentLogRepository.class);
    private final MetricsPublisher metricsPublisher = mock(MetricsPublisher.class);
    private final TargetThresholdService targetThresholdService = mock(TargetThresholdService.class);
    private final EscalationPolicyService escalationPolicyService = mock(EscalationPolicyService.class);
    private final NotificationDispatcherService notificationDispatcherService = mock(NotificationDispatcherService.class);
    private final InvestigationOrchestrator investigationOrchestrator = mock(InvestigationOrchestrator.class);
    private final AnomalyDetectionService anomalyDetectionService = mock(AnomalyDetectionService.class);
    private final AgentIngestController controller = new AgentIngestController(
            targetRepository,
            metricsRepository,
            incidentRepository,
            metricsPublisher,
            targetThresholdService,
            escalationPolicyService,
            notificationDispatcherService,
            investigationOrchestrator,
            anomalyDetectionService
    );

    @Test
    void shouldAcceptHeartbeatAndPersistRealMetricDimensions() {
        MonitorTarget target = new MonitorTarget();
        target.setId(21L);
        target.setUserId(3L);
        target.setName("vm-ubuntu");
        target.setAgentKey("agent-key");
        target.setEnabled(true);

        when(targetRepository.findByAgentKey("agent-key")).thenReturn(Optional.of(target));
        when(targetRepository.save(any(MonitorTarget.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(metricsRepository.save(any(SystemMetricsHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(targetThresholdService.resolve(3L, 21L)).thenReturn(new TargetThresholdService.EffectiveThreshold(
                95.0, 95.0, 95.0, 500, 2, 180, false
        ));
        AlertEscalationPolicy policy = new AlertEscalationPolicy();
        policy.setP1Intervals("1,3");
        policy.setP2Intervals("5,15");
        policy.setP3Intervals("15,30");
        when(escalationPolicyService.getOrCreateByUserId(3L)).thenReturn(policy);

        AgentHeartbeatRequest request = new AgentHeartbeatRequest();
        request.setAgentKey("agent-key");
        request.setHostname("ubuntu-vm");
        request.setIp("192.168.64.11");
        request.setAgentVersion("agent-lite-1.2.0-cross");
        request.setCpuUsage(31.2);
        request.setMemUsage(42.3);
        request.setDiskUsage(53.4);
        request.setNetRxBytesPerSec(12345.0);
        request.setNetTxBytesPerSec(6789.0);
        request.setProcessCount(156);
        request.setTimestamp(1760000000000L);

        ResponseEntity<Map<String, Object>> response = controller.heartbeat(request);

        assertTrue(Boolean.TRUE.equals(response.getBody().get("accepted")));
        assertEquals(21L, response.getBody().get("targetId"));
        assertEquals("ONLINE", target.getStatus());
        assertEquals("ubuntu-vm", target.getHostname());
        assertEquals("192.168.64.11", target.getIpAddress());
        assertEquals("agent-lite-1.2.0-cross", target.getAgentVersion());

        ArgumentCaptor<SystemMetricsHistory> historyCaptor = ArgumentCaptor.forClass(SystemMetricsHistory.class);
        verify(metricsRepository).save(historyCaptor.capture());
        SystemMetricsHistory history = historyCaptor.getValue();
        assertEquals(31.2, history.getCpuUsage());
        assertEquals(42.3, history.getMemUsage());
        assertEquals(53.4, history.getDiskUsage());
        assertEquals(12345.0, history.getNetRxBytesPerSec());
        assertEquals(6789.0, history.getNetTxBytesPerSec());
        assertEquals(156, history.getProcessCount());
        assertEquals(null, history.getUserId());
        assertEquals(21L, history.getTargetId());
    }
}
