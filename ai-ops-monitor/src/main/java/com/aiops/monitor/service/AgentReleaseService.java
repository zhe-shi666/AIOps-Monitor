package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.AgentRelease;
import com.aiops.monitor.repository.AgentReleaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentReleaseService {
    private final AgentReleaseRepository agentReleaseRepository;

    @Value("${monitor.agent.latest-version:agent-lite-1.2.0-cross}")
    private String latestAgentVersion;

    public Map<String, Object> currentRelease() {
        return agentReleaseRepository.findFirstByActiveTrueOrderByCreatedAtDesc()
                .map(this::toView)
                .orElseGet(this::fallbackRelease);
    }

    private Map<String, Object> toView(AgentRelease release) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("latestVersion", release.getVersion());
        view.put("packageName", release.getPackageName());
        view.put("sha256", release.getSha256());
        view.put("signature", release.getSignature() == null ? "" : release.getSignature());
        view.put("releaseNotes", release.getReleaseNotes() == null ? "" : release.getReleaseNotes());
        view.put("mandatory", release.isMandatory());
        view.put("supports", new String[]{"linux", "macos", "windows"});
        return view;
    }

    private Map<String, Object> fallbackRelease() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("latestVersion", latestAgentVersion);
        view.put("packageName", "aiops-agent-lite.tar.gz");
        view.put("sha256", "");
        view.put("signature", "");
        view.put("releaseNotes", "Fallback release metadata. Run tools/agent-lite/package-agent-lite.sh to generate signed manifest.");
        view.put("mandatory", false);
        view.put("supports", new String[]{"linux", "macos", "windows"});
        return view;
    }
}
