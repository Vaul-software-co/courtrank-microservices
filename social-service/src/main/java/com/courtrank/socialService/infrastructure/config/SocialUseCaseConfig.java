package com.courtrank.socialService.infrastructure.config;

import com.courtrank.socialService.application.ports.SocialEventPublisher;
import com.courtrank.socialService.application.ports.SocialUserProfileProvider;
import com.courtrank.socialService.application.ports.audit.SocialAuditLogger;
import com.courtrank.socialService.application.useCases.AcceptFollowRequestUseCase;
import com.courtrank.socialService.application.useCases.AreUsersBlockedUseCase;
import com.courtrank.socialService.application.useCases.BlockUserUseCase;
import com.courtrank.socialService.application.useCases.CreateSocialUserFromUserEventUseCase;
import com.courtrank.socialService.application.useCases.DeleteSocialUserFromUserEventUseCase;
import com.courtrank.socialService.application.useCases.FollowUserUseCase;
import com.courtrank.socialService.application.useCases.GetFollowStatusUseCase;
import com.courtrank.socialService.application.useCases.GetRelatedBlockedUserIdsUseCase;
import com.courtrank.socialService.application.useCases.GetSocialCountersUseCase;
import com.courtrank.socialService.application.useCases.GetUserSocialSummaryUseCase;
import com.courtrank.socialService.application.useCases.HandleUserBecamePublicUseCase;
import com.courtrank.socialService.application.useCases.ListBlockedUsersUseCase;
import com.courtrank.socialService.application.useCases.ListFollowersUseCase;
import com.courtrank.socialService.application.useCases.ListFollowingUseCase;
import com.courtrank.socialService.application.useCases.ListMyFollowRequestsUseCase;
import com.courtrank.socialService.application.useCases.RebuildAllSocialCountersUseCase;
import com.courtrank.socialService.application.useCases.RebuildSocialCounterUseCase;
import com.courtrank.socialService.application.useCases.ReconcileSocialUserUseCase;
import com.courtrank.socialService.application.useCases.RejectFollowRequestUseCase;
import com.courtrank.socialService.application.useCases.RemoveFollowerUseCase;
import com.courtrank.socialService.application.useCases.RestoreSocialUserFromUserEventUseCase;
import com.courtrank.socialService.application.useCases.SearchSocialUsersUseCase;
import com.courtrank.socialService.application.useCases.UnblockUserUseCase;
import com.courtrank.socialService.application.useCases.UnfollowUserUseCase;
import com.courtrank.socialService.application.useCases.UpdateSocialUserFromUserEventUseCase;
import com.courtrank.socialService.domain.repository.BlockRepository;
import com.courtrank.socialService.domain.repository.FollowRepository;
import com.courtrank.socialService.domain.repository.SocialCounterRepository;
import com.courtrank.socialService.domain.repository.SocialUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SocialUseCaseConfig {
    @Bean
    public FollowUserUseCase followUserUseCase(FollowRepository followRepository, SocialUserRepository socialUserRepository, BlockRepository blockRepository, SocialCounterRepository socialCounterRepository, SocialEventPublisher eventPublisher, SocialAuditLogger auditLogger) {
        return new FollowUserUseCase(followRepository, socialUserRepository, blockRepository, socialCounterRepository, eventPublisher, auditLogger);
    }

    @Bean
    public AcceptFollowRequestUseCase acceptFollowRequestUseCase(FollowRepository followRepository, SocialCounterRepository socialCounterRepository, SocialEventPublisher eventPublisher, SocialAuditLogger auditLogger) {
        return new AcceptFollowRequestUseCase(followRepository, socialCounterRepository, eventPublisher, auditLogger);
    }

    @Bean
    public RejectFollowRequestUseCase rejectFollowRequestUseCase(FollowRepository followRepository, SocialCounterRepository socialCounterRepository, SocialEventPublisher eventPublisher, SocialAuditLogger auditLogger) {
        return new RejectFollowRequestUseCase(followRepository, socialCounterRepository, eventPublisher, auditLogger);
    }

    @Bean
    public UnfollowUserUseCase unfollowUserUseCase(FollowRepository followRepository, SocialCounterRepository socialCounterRepository, SocialEventPublisher eventPublisher, SocialAuditLogger auditLogger) {
        return new UnfollowUserUseCase(followRepository, socialCounterRepository, eventPublisher, auditLogger);
    }

    @Bean
    public RemoveFollowerUseCase removeFollowerUseCase(FollowRepository followRepository, SocialCounterRepository socialCounterRepository, SocialEventPublisher eventPublisher, SocialAuditLogger auditLogger) {
        return new RemoveFollowerUseCase(followRepository, socialCounterRepository, eventPublisher, auditLogger);
    }

    @Bean
    public BlockUserUseCase blockUserUseCase(BlockRepository blockRepository, FollowRepository followRepository, SocialUserRepository socialUserRepository, SocialCounterRepository socialCounterRepository, SocialEventPublisher eventPublisher, SocialAuditLogger auditLogger) {
        return new BlockUserUseCase(blockRepository, followRepository, socialUserRepository, socialCounterRepository, eventPublisher, auditLogger);
    }

    @Bean
    public UnblockUserUseCase unblockUserUseCase(BlockRepository blockRepository, SocialCounterRepository socialCounterRepository, SocialEventPublisher eventPublisher, SocialAuditLogger auditLogger) {
        return new UnblockUserUseCase(blockRepository, socialCounterRepository, eventPublisher, auditLogger);
    }

    @Bean public ListFollowersUseCase listFollowersUseCase(FollowRepository followRepository, SocialUserRepository socialUserRepository, BlockRepository blockRepository) { return new ListFollowersUseCase(followRepository, socialUserRepository, blockRepository); }
    @Bean public ListFollowingUseCase listFollowingUseCase(FollowRepository followRepository, SocialUserRepository socialUserRepository, BlockRepository blockRepository) { return new ListFollowingUseCase(followRepository, socialUserRepository, blockRepository); }
    @Bean public ListMyFollowRequestsUseCase listMyFollowRequestsUseCase(FollowRepository followRepository, SocialUserRepository socialUserRepository) { return new ListMyFollowRequestsUseCase(followRepository, socialUserRepository); }
    @Bean public ListBlockedUsersUseCase listBlockedUsersUseCase(BlockRepository blockRepository, SocialUserRepository socialUserRepository) { return new ListBlockedUsersUseCase(blockRepository, socialUserRepository); }
    @Bean public GetFollowStatusUseCase getFollowStatusUseCase(FollowRepository followRepository) { return new GetFollowStatusUseCase(followRepository); }
    @Bean public GetSocialCountersUseCase getSocialCountersUseCase(SocialCounterRepository socialCounterRepository) { return new GetSocialCountersUseCase(socialCounterRepository); }
    @Bean public GetUserSocialSummaryUseCase getUserSocialSummaryUseCase(SocialUserRepository socialUserRepository, FollowRepository followRepository, BlockRepository blockRepository, SocialCounterRepository socialCounterRepository) { return new GetUserSocialSummaryUseCase(socialUserRepository, followRepository, blockRepository, socialCounterRepository); }
    @Bean public SearchSocialUsersUseCase searchSocialUsersUseCase(SocialUserRepository socialUserRepository, BlockRepository blockRepository) { return new SearchSocialUsersUseCase(socialUserRepository, blockRepository); }
    @Bean public AreUsersBlockedUseCase areUsersBlockedUseCase(BlockRepository blockRepository) { return new AreUsersBlockedUseCase(blockRepository); }
    @Bean public GetRelatedBlockedUserIdsUseCase getRelatedBlockedUserIdsUseCase(BlockRepository blockRepository) { return new GetRelatedBlockedUserIdsUseCase(blockRepository); }
    @Bean public CreateSocialUserFromUserEventUseCase createSocialUserFromUserEventUseCase(SocialUserRepository socialUserRepository, SocialCounterRepository socialCounterRepository) { return new CreateSocialUserFromUserEventUseCase(socialUserRepository, socialCounterRepository); }
    @Bean public DeleteSocialUserFromUserEventUseCase deleteSocialUserFromUserEventUseCase(SocialUserRepository socialUserRepository) { return new DeleteSocialUserFromUserEventUseCase(socialUserRepository); }
    @Bean public RestoreSocialUserFromUserEventUseCase restoreSocialUserFromUserEventUseCase(SocialUserRepository socialUserRepository, SocialCounterRepository socialCounterRepository) { return new RestoreSocialUserFromUserEventUseCase(socialUserRepository, socialCounterRepository); }
    @Bean public UpdateSocialUserFromUserEventUseCase updateSocialUserFromUserEventUseCase(SocialUserRepository socialUserRepository, SocialCounterRepository socialCounterRepository, FollowRepository followRepository, SocialEventPublisher eventPublisher) { return new UpdateSocialUserFromUserEventUseCase(socialUserRepository, socialCounterRepository, followRepository, eventPublisher); }
    @Bean public HandleUserBecamePublicUseCase handleUserBecamePublicUseCase(FollowRepository followRepository, SocialCounterRepository socialCounterRepository, SocialEventPublisher eventPublisher) { return new HandleUserBecamePublicUseCase(followRepository, socialCounterRepository, eventPublisher); }
    @Bean public RebuildSocialCounterUseCase rebuildSocialCounterUseCase(FollowRepository followRepository, BlockRepository blockRepository, SocialCounterRepository socialCounterRepository) { return new RebuildSocialCounterUseCase(followRepository, blockRepository, socialCounterRepository); }
    @Bean public RebuildAllSocialCountersUseCase rebuildAllSocialCountersUseCase(SocialUserRepository socialUserRepository, FollowRepository followRepository, BlockRepository blockRepository, SocialCounterRepository socialCounterRepository) { return new RebuildAllSocialCountersUseCase(socialUserRepository, followRepository, blockRepository, socialCounterRepository); }
    @Bean public ReconcileSocialUserUseCase reconcileSocialUserUseCase(SocialUserProfileProvider socialUserProfileProvider, UpdateSocialUserFromUserEventUseCase updateSocialUserFromUserEventUseCase) { return new ReconcileSocialUserUseCase(socialUserProfileProvider, updateSocialUserFromUserEventUseCase); }
}
