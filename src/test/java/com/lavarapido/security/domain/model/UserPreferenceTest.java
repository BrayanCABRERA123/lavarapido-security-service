package com.lavarapido.security.domain.model;

import com.lavarapido.security.domain.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPreferenceTest {

    @Test
    void acceptsTheFourWebPalettes() {
        assertEquals(Theme.GREEN_LIGHT, Theme.fromCode("green-light"));
        assertEquals(Theme.GREEN_DARK, Theme.fromCode("green-dark"));
        assertEquals(Theme.PINK, Theme.fromCode("pink"));
        assertEquals(Theme.PINK_DARK, Theme.fromCode("PINK-DARK"));
        assertEquals("pink-dark", Theme.PINK_DARK.code());
    }

    @Test
    void rejectsUnknownThemes() {
        assertThrows(InvalidValueException.class, () -> Theme.fromCode("dark"));
        assertThrows(InvalidValueException.class, () -> Theme.fromCode(null));
    }

    @Test
    void newAccountsStartWithGreenLightAndSpanish() {
        UserPreference preference = UserPreference.defaultsFor(9L);
        assertEquals(Theme.GREEN_LIGHT, preference.theme());
        assertEquals(Language.ES, preference.language());
    }

    @Test
    void newAccountsReceiveEveryChannel() {
        UserPreference preference = UserPreference.defaultsFor(9L);
        assertTrue(preference.notificationsEnabled());
        assertTrue(preference.emailRemindersEnabled());
        assertTrue(preference.promotionsEnabled());
    }

    @Test
    void channelsThatArriveEmptyKeepTheirValue() {
        UserPreference preference = UserPreference.defaultsFor(9L);

        preference.changeNotificationChannels(null, false, null);

        assertTrue(preference.notificationsEnabled());
        assertFalse(preference.emailRemindersEnabled());
        assertTrue(preference.promotionsEnabled());
    }

    @Test
    void changingTheInterfaceDoesNotTouchTheChannels() {
        UserPreference preference = UserPreference.defaultsFor(9L);
        preference.changeNotificationChannels(false, false, false);

        preference.changeInterface(Theme.PINK_DARK, Language.EN);

        assertEquals(Theme.PINK_DARK, preference.theme());
        assertFalse(preference.notificationsEnabled());
        assertFalse(preference.emailRemindersEnabled());
        assertFalse(preference.promotionsEnabled());
    }
}
