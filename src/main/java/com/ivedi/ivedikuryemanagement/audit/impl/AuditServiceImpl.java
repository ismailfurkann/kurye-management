package com.ivedi.ivedikuryemanagement.audit.impl;

import com.ivedi.ivedikuryemanagement.audit.api.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Async + REQUIRES_NEW: audit never blocks the caller and never shares its transaction.
     */
    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID actorId, String action, String entityType, UUID entityId, String details) {
        try {
            auditLogRepository.save(AuditLog.builder()
                    .actorId(actorId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .build());
        } catch (Exception e) {
            log.error("Audit log write failed — action={} entity={}/{} actor={}",
                    action, entityType, entityId, actorId, e);
        }
    }
}
