package com.courtrank.auditService.infrastructure.config;

import com.courtrank.auditService.application.useCases.GetAuditEventUseCase;
import com.courtrank.auditService.application.useCases.IngestAuditEventUseCase;
import com.courtrank.auditService.application.useCases.SearchAuditEventsUseCase;
import com.courtrank.auditService.domain.repository.AuditEventRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditUseCaseConfig {
    @Bean
    public IngestAuditEventUseCase ingestAuditEventUseCase(AuditEventRepository auditEventRepository) {
        return new IngestAuditEventUseCase(auditEventRepository);
    }

    @Bean
    public GetAuditEventUseCase getAuditEventUseCase(AuditEventRepository auditEventRepository) {
        return new GetAuditEventUseCase(auditEventRepository);
    }

    @Bean
    public SearchAuditEventsUseCase searchAuditEventsUseCase(AuditEventRepository auditEventRepository) {
        return new SearchAuditEventsUseCase(auditEventRepository);
    }
}
