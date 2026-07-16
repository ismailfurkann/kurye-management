package com.ivedi.ivedikuryemanagement.audit.api;

import java.util.UUID;

public interface AuditService {

    /**
     * Records an audit entry. Non-blocking — runs in a separate transaction.
     * Never throws; a logging failure must not break the main flow.
     */
    void record(UUID actorId, String action, String entityType, UUID entityId, String details);
}
