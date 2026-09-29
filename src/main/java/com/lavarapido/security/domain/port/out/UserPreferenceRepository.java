package com.lavarapido.security.domain.port.out;

import com.lavarapido.security.domain.model.UserPreference;

import java.util.Optional;

public interface UserPreferenceRepository {

    Optional<UserPreference> findByUserId(long userId);

    UserPreference save(UserPreference preference);
}
