package com.aiops.monitor.service;

import com.aiops.monitor.model.entity.MonitorTarget;
import com.aiops.monitor.model.entity.User;
import com.aiops.monitor.repository.MonitorTargetRepository;
import com.aiops.monitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalCollectorOwnershipService {

    private final UserRepository userRepository;
    private final MonitorTargetRepository monitorTargetRepository;
    private final AgentKeyGenerator agentKeyGenerator;

    @Value("${monitor.local.default-user-id:}")
    private String defaultUserIdRaw;

    @Value("${monitor.local.default-username:}")
    private String defaultUsername;

    @Value("${monitor.local.auto-create-target:true}")
    private boolean autoCreateTarget;

    @Value("${monitor.local.cache-seconds:60}")
    private long cacheSeconds;

    @Value("${monitor.local.target-name-prefix:Local}")
    private String targetNamePrefix;

    private final Object lock = new Object();
    private volatile boolean cachePrimed = false;
    private volatile LocalOwnership cachedOwnership;
    private volatile String cachedHostname;
    private volatile LocalDateTime nextRefreshAt = LocalDateTime.MIN;

    public LocalOwnership resolve(String requestedHostname) {
        String hostname = normalizeHostname(requestedHostname);
        LocalDateTime now = LocalDateTime.now();
        if (cachePrimed && hostname.equals(cachedHostname) && now.isBefore(nextRefreshAt)) {
            return cachedOwnership;
        }
        synchronized (lock) {
            now = LocalDateTime.now();
            if (cachePrimed && hostname.equals(cachedHostname) && now.isBefore(nextRefreshAt)) {
                return cachedOwnership;
            }
            LocalOwnership refreshed = resolveFresh(hostname, now);
            long successTtl = Math.max(10L, cacheSeconds);
            long ttlSeconds = refreshed == null ? 10L : successTtl;
            cachedOwnership = refreshed;
            cachedHostname = hostname;
            cachePrimed = true;
            nextRefreshAt = now.plusSeconds(ttlSeconds);
            return refreshed;
        }
    }

    @Transactional
    protected LocalOwnership resolveFresh(String hostname, LocalDateTime now) {
        User owner = resolveOwner();
        if (owner == null) {
            log.warn("本地采集归属解析失败：暂无可用用户，hostname={}", hostname);
            return null;
        }

        MonitorTarget target = resolveOrCreateTarget(owner.getId(), hostname, now);
        if (target == null) {
            return new LocalOwnership(owner.getId(), null, hostname);
        }
        String effectiveHostname = isBlank(target.getHostname()) ? hostname : target.getHostname().trim();
        return new LocalOwnership(owner.getId(), target.getId(), effectiveHostname);
    }

    private User resolveOwner() {
        Optional<User> byId = parseLong(defaultUserIdRaw)
                .flatMap(userRepository::findById)
                .filter(User::isEnabled);
        if (byId.isPresent()) {
            return byId.get();
        }

        if (!isBlank(defaultUsername)) {
            Optional<User> byUsername = userRepository.findByUsername(defaultUsername.trim())
                    .filter(User::isEnabled);
            if (byUsername.isPresent()) {
                return byUsername.get();
            }
        }

        return userRepository.findFirstByEnabledTrueOrderByIdAsc().orElse(null);
    }

    private MonitorTarget resolveOrCreateTarget(Long userId, String hostname, LocalDateTime now) {
        Optional<MonitorTarget> byHost = monitorTargetRepository
                .findFirstByUserIdAndHostnameOrderByCreatedAtDesc(userId, hostname);
        if (byHost.isPresent()) {
            return touchHeartbeat(byHost.get(), now);
        }

        if (!autoCreateTarget) {
            return monitorTargetRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                    .map(target -> touchHeartbeat(target, now))
                    .orElse(null);
        }

        MonitorTarget created = new MonitorTarget();
        created.setUserId(userId);
        created.setName(limit(buildTargetName(hostname), 100));
        created.setHostname(limit(hostname, 255));
        created.setDescription("Auto created by local collector");
        created.setAgentKey(agentKeyGenerator.generateUniqueKey());
        created.setEnabled(true);
        created.setStatus("ONLINE");
        created.setIpAddress("127.0.0.1");
        created.setAgentVersion("LOCAL_COLLECTOR");
        created.setCreatedAt(now);
        created.setLastHeartbeatAt(now);
        MonitorTarget saved = monitorTargetRepository.save(created);
        log.info("本地采集自动创建监控目标: userId={}, targetId={}, hostname={}", userId, saved.getId(), hostname);
        return saved;
    }

    private MonitorTarget touchHeartbeat(MonitorTarget target, LocalDateTime now) {
        boolean changed = false;
        if (target.getLastHeartbeatAt() == null || target.getLastHeartbeatAt().plusSeconds(30).isBefore(now)) {
            target.setLastHeartbeatAt(now);
            changed = true;
        }
        if (!"ONLINE".equalsIgnoreCase(target.getStatus())) {
            target.setStatus("ONLINE");
            changed = true;
        }
        if (changed) {
            return monitorTargetRepository.save(target);
        }
        return target;
    }

    private String buildTargetName(String hostname) {
        String prefix = isBlank(targetNamePrefix) ? "Local" : targetNamePrefix.trim();
        if (isBlank(hostname)) {
            return prefix + "-Node";
        }
        return prefix + "-" + hostname.replaceAll("[^A-Za-z0-9._-]", "-");
    }

    private String normalizeHostname(String hostname) {
        if (isBlank(hostname)) {
            return "local-node";
        }
        return hostname.trim();
    }

    private Optional<Long> parseLong(String text) {
        if (isBlank(text)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(text.trim()));
        } catch (NumberFormatException ex) {
            log.warn("monitor.local.default-user-id 配置非法: {}", text);
            return Optional.empty();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String limit(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }

    public record LocalOwnership(Long userId, Long targetId, String hostname) {}
}
