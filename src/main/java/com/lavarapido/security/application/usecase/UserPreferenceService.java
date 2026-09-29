package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.model.Language;
import com.lavarapido.security.domain.model.Theme;
import com.lavarapido.security.domain.model.UserPreference;
import com.lavarapido.security.domain.port.in.GetUserPreferencesUseCase;
import com.lavarapido.security.domain.port.in.PreferenceView;
import com.lavarapido.security.domain.port.in.UpdateUserPreferencesUseCase;
import com.lavarapido.security.domain.port.out.UserPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tema, idioma y notificaciones del usuario con sesión iniciada (RF-020). */
@Service
@Transactional
public class UserPreferenceService implements GetUserPreferencesUseCase, UpdateUserPreferencesUseCase {

    private final UserPreferenceRepository preferences;

    public UserPreferenceService(UserPreferenceRepository preferences) {
        this.preferences = preferences;
    }

    @Override
    @Transactional(readOnly = true)
    public PreferenceView getPreferences(long userId) {
        return PreferenceView.from(preferences.findByUserId(userId)
                .orElseGet(() -> UserPreference.defaultsFor(userId)));
    }

    @Override
    public PreferenceView updatePreferences(UpdatePreferencesCommand command) {
        Theme theme = Theme.fromCode(command.theme());
        Language language = Language.fromCode(command.language());

        UserPreference preference = preferences.findByUserId(command.userId())
                .orElseGet(() -> UserPreference.defaultsFor(command.userId()));
        preference.update(theme, language, command.notificationsEnabled());
        return PreferenceView.from(preferences.save(preference));
    }
}
