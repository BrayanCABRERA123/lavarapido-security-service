package com.lavarapido.security.domain.port.in;

public interface GetUserPreferencesUseCase {

    /** Devuelve las preferencias guardadas o las de por defecto si el usuario nunca guardó. */
    PreferenceView getPreferences(long userId);
}
