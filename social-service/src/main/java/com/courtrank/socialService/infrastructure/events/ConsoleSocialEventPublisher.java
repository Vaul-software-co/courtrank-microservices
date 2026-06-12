package com.courtrank.socialService.infrastructure.events;

import com.courtrank.socialService.application.events.FollowAcceptedEvent;
import com.courtrank.socialService.application.events.FollowRejectedEvent;
import com.courtrank.socialService.application.events.FollowRemovedEvent;
import com.courtrank.socialService.application.events.FollowRequestedEvent;
import com.courtrank.socialService.application.events.FollowerRemovedEvent;
import com.courtrank.socialService.application.events.UserBlockedEvent;
import com.courtrank.socialService.application.events.UserUnblockedEvent;
import com.courtrank.socialService.application.ports.SocialEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleSocialEventPublisher implements SocialEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleSocialEventPublisher.class);

    @Override
    public void publishFollowRequested(FollowRequestedEvent event) {
        logger.info("Social event FOLLOW_REQUESTED: {}", event);
    }

    @Override
    public void publishFollowAccepted(FollowAcceptedEvent event) {
        logger.info("Social event FOLLOW_ACCEPTED: {}", event);
    }

    @Override
    public void publishFollowRejected(FollowRejectedEvent event) {
        logger.info("Social event FOLLOW_REJECTED: {}", event);
    }

    @Override
    public void publishFollowRemoved(FollowRemovedEvent event) {
        logger.info("Social event FOLLOW_REMOVED: {}", event);
    }

    @Override
    public void publishFollowerRemoved(FollowerRemovedEvent event) {
        logger.info("Social event FOLLOWER_REMOVED: {}", event);
    }

    @Override
    public void publishUserBlocked(UserBlockedEvent event) {
        logger.info("Social event USER_BLOCKED: {}", event);
    }

    @Override
    public void publishUserUnblocked(UserUnblockedEvent event) {
        logger.info("Social event USER_UNBLOCKED: {}", event);
    }
}
