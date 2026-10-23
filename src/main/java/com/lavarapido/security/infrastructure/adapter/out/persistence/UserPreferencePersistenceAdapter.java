package com.lavarapido.security.infrastructure.adapter.out.persistence;

import com.lavarapido.security.domain.model.Language;
import com.lavarapido.security.domain.model.Theme;
import com.lavarapido.security.domain.model.UserPreference;
import com.lavarapido.security.domain.port.out.UserPreferenceRepository;
import com.lavarapido.security.infrastructure.adapter.out.persistence.entity.UserPreferenceJpaEntity;
import com.lavarapido.security.infrastructure.adapter.out.persistence.repository.UserPreferenceJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;

@Component
class UserPreferencePersistenceAdapter implements UserPreferenceRepository {

    private final UserPreferenceJpaRepository preferences;
    private final Clock clock;

    UserPreferencePersistenceAdapter(UserPreferenceJpaRepository preferences, Clock clock) {
        this.preferences = preferences;
        this.clock = clock;
    }

    @Override
    public Optional<UserPreference> findByUserId(long userId) {
        return preferences.findByUserId(userId).map(UserPreferencePersistenceAdapter::toDomain);
    }

    @Override
    public UserPreference save(UserPreference preference) {
        UserPreferenceJpaEntity entity = preferences.findByUserId(preference.userId())
                .orElseGet(() -> new UserPreferenceJpaEntity(preference.userId(), clock.instant()));
        entity.setTheme(preference.theme().code());
        entity.setLanguage(preference.language().code());
        entity.setNotificationsEnabled(preference.notificationsEnabled());
        entity.setEmailRemindersEnabled(preference.emailRemindersEnabled());
        entity.setPromotionsEnabled(preference.promotionsEnabled());
        if (entity.getId() != null) {
            // Las preferencias solo las edita su dueño.
            entity.setUpdatedBy(preference.userId());
        }
        return toDomain(preferences.save(entity));
    }

    private static UserPreference toDomain(UserPreferenceJpaEntity entity) {
        return UserPreference.reconstitute(entity.getId(), entity.getUserId(), Theme.fromCode(entity.getTheme()),
                Language.fromCode(entity.getLanguage()), entity.isNotificationsEnabled(),
                entity.isEmailRemindersEnabled(), entity.isPromotionsEnabled());
    }
}
