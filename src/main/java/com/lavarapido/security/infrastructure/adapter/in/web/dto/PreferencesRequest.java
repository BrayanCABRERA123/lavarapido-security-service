package com.lavarapido.security.infrastructure.adapter.in.web.dto;

/**
 * Cambio parcial de las preferencias: el campo que no llegue se queda como estaba. Así la pantalla
 * de apariencia manda solo tema e idioma, y la de notificaciones solo los interruptores, sin pisarse.
 * notificationsEnabled son las notificaciones push. Un tema o idioma que llegue vacío o desconocido
 * responde 400.
 */
public record PreferencesRequest(
        String theme,
        String language,
        Boolean notificationsEnabled,
        Boolean emailRemindersEnabled,
        Boolean promotionsEnabled) {
}
