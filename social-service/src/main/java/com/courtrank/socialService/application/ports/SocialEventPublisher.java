package com.courtrank.socialService.application.ports;

import com.courtrank.socialService.application.events.FollowAcceptedEvent;
import com.courtrank.socialService.application.events.FollowRejectedEvent;
import com.courtrank.socialService.application.events.FollowRemovedEvent;
import com.courtrank.socialService.application.events.FollowRequestedEvent;
import com.courtrank.socialService.application.events.FollowerRemovedEvent;
import com.courtrank.socialService.application.events.UserBlockedEvent;
import com.courtrank.socialService.application.events.UserUnblockedEvent;

public interface SocialEventPublisher {
    void publishFollowRequested(FollowRequestedEvent event);
    void publishFollowAccepted(FollowAcceptedEvent event);
    void publishFollowRejected(FollowRejectedEvent event);
    void publishFollowRemoved(FollowRemovedEvent event);
    void publishFollowerRemoved(FollowerRemovedEvent event);
    void publishUserBlocked(UserBlockedEvent event);
    void publishUserUnblocked(UserUnblockedEvent event);
}
