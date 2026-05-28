package com.example.userService.domain.valueObjects;

import java.time.Instant;

public record UsernameChangeInfo (
    int changesUsed,
    int changesLeft,
    Instant nextAvailableAt
){}
