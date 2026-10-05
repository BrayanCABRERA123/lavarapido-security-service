package com.lavarapido.security.domain.model;

import com.lavarapido.security.domain.exception.InvalidValueException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
