package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.exception.InvalidValueException;
import com.lavarapido.security.domain.model.UserPreference;
import com.lavarapido.security.domain.port.in.PreferenceView;
import com.lavarapido.security.domain.port.in.UpdateUserPreferencesUseCase.UpdatePreferencesCommand;
import com.lavarapido.security.domain.port.out.UserPreferenceRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Preferencias de la cuenta: el cambio es parcial (lo que no llega se queda como estaba). */
class UserPreferenceServiceTest {

    private static final long ANA = 7;

    private final InMemoryPreferences repository = new InMemoryPreferences();
    private final UserPreferenceService service = new UserPreferenceService(repository);

    @Test
    void withoutSavedPreferencesEveryChannelIsOn() {
        PreferenceView view = service.getPreferences(ANA);

        assertTrue(view.notificationsEnabled());
        assertTrue(view.emailRemindersEnabled());
        assertTrue(view.promotionsEnabled());
    }

    @Test
    void savingOnlyTheSwitchesKeepsThemeAndLanguage() {
        service.updatePreferences(new UpdatePreferencesCommand(ANA, "pink-dark", "en", null, null, null));

        PreferenceView view = service.updatePreferences(new UpdatePreferencesCommand(ANA, null, null, false, true, false));

        assertEquals("pink-dark", view.theme());
        assertEquals("en", view.language());
        assertFalse(view.notificationsEnabled());
        assertTrue(view.emailRemindersEnabled());
        assertFalse(view.promotionsEnabled());
    }

    @Test
    void changingTheThemeKeepsTheSwitches() {
        service.updatePreferences(new UpdatePreferencesCommand(ANA, null, null, true, false, false));

        PreferenceView view = service.updatePreferences(new UpdatePreferencesCommand(ANA, "green-dark", "es", null, null, null));

        assertEquals("green-dark", view.theme());
        assertFalse(view.emailRemindersEnabled());
        assertFalse(view.promotionsEnabled());
    }

    @Test
    void anUnknownThemeIsRejected() {
        assertThrows(InvalidValueException.class,
                () -> service.updatePreferences(new UpdatePreferencesCommand(ANA, "dark", null, null, null, null)));
    }

    private static final class InMemoryPreferences implements UserPreferenceRepository {

        private final Map<Long, UserPreference> rows = new HashMap<>();

        @Override
        public Optional<UserPreference> findByUserId(long userId) {
            return Optional.ofNullable(rows.get(userId));
        }

        @Override
        public UserPreference save(UserPreference preference) {
            rows.put(preference.userId(), preference);
            return preference;
        }
    }
}
