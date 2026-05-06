package com.aiops.monitor.controller;

import com.aiops.monitor.model.entity.AiActionPlan;
import com.aiops.monitor.model.entity.AiActionRun;
import com.aiops.monitor.model.entity.AiActionAudit;
import com.aiops.monitor.model.entity.AiHypothesis;
import com.aiops.monitor.model.entity.AiInvestigation;
import com.aiops.monitor.model.entity.AiModelTrace;
import com.aiops.monitor.model.entity.AiObservation;
import com.aiops.monitor.model.entity.AiReportSnapshot;
import com.aiops.monitor.model.entity.RcaReport;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.AiActionPlanRepository;
import com.aiops.monitor.repository.AiActionRunRepository;
import com.aiops.monitor.repository.AiActionAuditRepository;
import com.aiops.monitor.repository.AiHypothesisRepository;
import com.aiops.monitor.repository.AiInvestigationRepository;
import com.aiops.monitor.repository.AiModelTraceRepository;
import com.aiops.monitor.repository.AiObservationRepository;
import com.aiops.monitor.repository.AiRollbackRunRepository;
import com.aiops.monitor.repository.AiReportSnapshotRepository;
import com.aiops.monitor.repository.RcaReportRepository;
import com.aiops.monitor.service.CurrentUserService;
import com.aiops.monitor.service.InvestigationEventPublisher;
import com.aiops.monitor.service.InvestigationIntelligenceService;
import com.aiops.monitor.service.InvestigationQualityService;
import com.aiops.monitor.security.JwtAuthenticationFilter;
import com.aiops.monitor.security.PasswordChangeRequiredFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = InvestigationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class InvestigationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiInvestigationRepository aiInvestigationRepository;
    @MockBean
    private AiObservationRepository aiObservationRepository;
    @MockBean
    private AiHypothesisRepository aiHypothesisRepository;
    @MockBean
    private AiActionPlanRepository aiActionPlanRepository;
    @MockBean
    private AiActionRunRepository aiActionRunRepository;
    @MockBean
    private AiActionAuditRepository aiActionAuditRepository;
    @MockBean
    private AiRollbackRunRepository aiRollbackRunRepository;
    @MockBean
    private AiReportSnapshotRepository aiReportSnapshotRepository;
    @MockBean
    private RcaReportRepository rcaReportRepository;
    @MockBean
    private AiModelTraceRepository aiModelTraceRepository;
    @MockBean
    private CurrentUserService currentUserService;
    @MockBean
    private InvestigationQualityService investigationQualityService;
    @MockBean
    private InvestigationIntelligenceService investigationIntelligenceService;
    @MockBean
    private InvestigationEventPublisher investigationEventPublisher;

    @MockBean
    private com.aiops.monitor.service.RoleGuardService roleGuardService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private PasswordChangeRequiredFilter passwordChangeRequiredFilter;

    @Test
    void shouldReturnInvestigationPage() throws Exception {
        User user = buildUser(1L, "demo-user");
        AiInvestigation investigation = new AiInvestigation();
        investigation.setId(101L);
        investigation.setStatus("COLLECTING");
        investigation.setSeverity("P2");

        Page<AiInvestigation> page = new PageImpl<>(
                List.of(investigation),
                PageRequest.of(0, 1),
                1
        );

        when(currentUserService.requireUser(any())).thenReturn(user);
        when(aiInvestigationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/investigations").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(101))
                .andExpect(jsonPath("$.content[0].status").value("COLLECTING"))
                .andExpect(jsonPath("$.content[0].severity").value("P2"));
    }

    @Test
    void shouldReturnQualitySummaryPayload() throws Exception {
        User user = buildUser(2L, "ops");
        Map<String, Object> summary = Map.of(
                "windowDays", 30,
                "openInvestigations", 4,
                "falsePositiveRate", 25.0
        );

        when(currentUserService.requireUser(any())).thenReturn(user);
        when(investigationQualityService.buildSummary(2L)).thenReturn(summary);

        mockMvc.perform(get("/api/investigations/quality/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.windowDays").value(30))
                .andExpect(jsonPath("$.openInvestigations").value(4))
                .andExpect(jsonPath("$.falsePositiveRate").value(25.0));
    }

    @Test
    void shouldReturnRcaReportsForInvestigation() throws Exception {
        User user = buildUser(3L, "sre");
        AiInvestigation investigation = new AiInvestigation();
        investigation.setId(900L);

        RcaReport report = new RcaReport();
        report.setId(501L);
        report.setInvestigationId(900L);
        report.setUserId(3L);
        report.setConfidence(0.86d);
        report.setSummaryMd("CPU saturation by backup task");

        when(currentUserService.requireUser(any())).thenReturn(user);
        when(aiInvestigationRepository.findByIdAndUserId(900L, 3L)).thenReturn(Optional.of(investigation));
        when(rcaReportRepository.findByInvestigationIdAndUserIdOrderByCreatedAtDesc(eq(900L), eq(3L), any(Pageable.class)))
                .thenReturn(List.of(report));

        mockMvc.perform(get("/api/investigations/900/rca-reports").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(501))
                .andExpect(jsonPath("$[0].confidence").value(0.86))
                .andExpect(jsonPath("$[0].summaryMd").value("CPU saturation by backup task"));
    }

    @Test
    void shouldReturnModelTracePageForInvestigation() throws Exception {
        User user = buildUser(4L, "ops");
        AiInvestigation investigation = new AiInvestigation();
        investigation.setId(901L);

        AiModelTrace trace = new AiModelTrace();
        trace.setId(601L);
        trace.setInvestigationId(901L);
        trace.setUserId(4L);
        trace.setPhase("STRUCTURED_ANALYSIS");
        trace.setStatus("SUCCESS");

        Page<AiModelTrace> page = new PageImpl<>(
                List.of(trace),
                PageRequest.of(0, 10),
                1
        );

        when(currentUserService.requireUser(any())).thenReturn(user);
        when(aiInvestigationRepository.findByIdAndUserId(901L, 4L)).thenReturn(Optional.of(investigation));
        when(aiModelTraceRepository.findByInvestigationIdAndUserIdOrderByCreatedAtDesc(eq(901L), eq(4L), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/investigations/901/model-traces")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(601))
                .andExpect(jsonPath("$.content[0].phase").value("STRUCTURED_ANALYSIS"))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"));
    }

    @Test
    void shouldReturnActionAuditsPageForInvestigation() throws Exception {
        User user = buildUser(5L, "audit");
        AiInvestigation investigation = new AiInvestigation();
        investigation.setId(902L);

        AiActionAudit audit = new AiActionAudit();
        audit.setId(701L);
        audit.setInvestigationId(902L);
        audit.setActionPlanId(301L);
        audit.setUserId(5L);
        audit.setEventType("ACTION_APPROVED");
        audit.setDecision("APPROVED");

        Page<AiActionAudit> page = new PageImpl<>(
                List.of(audit),
                PageRequest.of(0, 10),
                1
        );

        when(currentUserService.requireUser(any())).thenReturn(user);
        when(aiInvestigationRepository.findByIdAndUserId(902L, 5L)).thenReturn(Optional.of(investigation));
        when(aiActionAuditRepository.search(eq(902L), eq(5L), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/investigations/902/actions/audits")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(701))
                .andExpect(jsonPath("$.content[0].eventType").value("ACTION_APPROVED"))
                .andExpect(jsonPath("$.content[0].decision").value("APPROVED"));
    }

    private User buildUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
