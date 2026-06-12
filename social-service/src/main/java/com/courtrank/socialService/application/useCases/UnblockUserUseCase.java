package com.courtrank.socialService.application.useCases;

import com.courtrank.socialService.application.dto.TraceContext;
import com.courtrank.socialService.application.dto.UnblockUserRequest;
import com.courtrank.socialService.application.events.UserUnblockedEvent;
import com.courtrank.socialService.application.ports.SocialEventPublisher;
import com.courtrank.socialService.application.ports.audit.SocialAuditEventType;
import com.courtrank.socialService.application.ports.audit.SocialAuditLogger;
import com.courtrank.socialService.domain.entity.Block;
import com.courtrank.socialService.domain.entity.SocialCounter;
import com.courtrank.socialService.domain.repository.BlockRepository;
import com.courtrank.socialService.domain.repository.SocialCounterRepository;

import java.time.Instant;
import java.util.Map;

public class UnblockUserUseCase extends SocialUseCaseSupport {
    private final BlockRepository blockRepository;
    private final SocialCounterRepository socialCounterRepository;
    private final SocialEventPublisher eventPublisher;
    private final SocialAuditLogger auditLogger;

    public UnblockUserUseCase(
            BlockRepository blockRepository,
            SocialCounterRepository socialCounterRepository,
            SocialEventPublisher eventPublisher,
            SocialAuditLogger auditLogger
    ) {
        this.blockRepository = blockRepository;
        this.socialCounterRepository = socialCounterRepository;
        this.eventPublisher = eventPublisher;
        this.auditLogger = auditLogger;
    }

    public void execute(UnblockUserRequest request, TraceContext trace) {
        Block block = this.blockRepository.findByBlockerIdAndBlockedId(request.blockerId(), request.blockedId()).orElse(null);
        if (block == null) {
            this.log(this.auditLogger, SocialAuditEventType.BLOCK_REMOVE_SKIPPED_NOT_FOUND, request.blockerId(), request.blockedId(), trace, Map.of("reason", "block_not_found"));
            return;
        }

        block.assertCanBeRemovedBy(request.blockerId());
        this.blockRepository.delete(block);
        SocialCounter blockerCounter = this.findOrCreateCounter(this.socialCounterRepository, request.blockerId());
        blockerCounter.removeBlockAsBlocker();
        this.socialCounterRepository.save(blockerCounter);

        this.eventPublisher.publishUserUnblocked(new UserUnblockedEvent(block.getId(), block.getBlockerId(), block.getBlockedId(), Instant.now()));
        this.log(this.auditLogger, SocialAuditEventType.BLOCK_REMOVED, request.blockerId(), request.blockedId(), trace, Map.of("blockId", block.getId().toString()));
    }
}
