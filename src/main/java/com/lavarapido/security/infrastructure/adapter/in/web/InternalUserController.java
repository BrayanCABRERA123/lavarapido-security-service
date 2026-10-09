package com.lavarapido.security.infrastructure.adapter.in.web;

import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.port.in.GetUserPreferencesUseCase;
import com.lavarapido.security.domain.port.in.GetUserProfileUseCase;
import com.lavarapido.security.domain.port.in.ListUserAccountsUseCase;
import com.lavarapido.security.domain.port.in.ListUserAccountsUseCase.ListUserAccountsQuery;
import com.lavarapido.security.domain.port.in.PreferenceView;
import com.lavarapido.security.domain.port.in.UserAccountView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consultas entre servicios (no las usa la web). Protegidas con la llave interna, no con el JWT
 * de un usuario (SecurityConfig.internalFilterChain).
 *
 * notification-service pide aquí el correo para enviar recordatorios y avisos de reserva: así el
 * correo vive solo en este servicio y siempre se usa el actual (ADR-011, sección 8). También pide
 * quiénes son los administradores para avisarles de reservas nuevas, reprogramadas o canceladas.
 */
@RestController
@RequestMapping("/internal/v1/users")
class InternalUserController {

    // el lavadero tiene pocos administradores: una página alcanza (es el máximo que permite el caso de uso)
    private static final int ADMINS_PAGE_SIZE = 100;

    private final GetUserProfileUseCase getProfile;
    private final ListUserAccountsUseCase listAccounts;
    private final GetUserPreferencesUseCase getPreferences;

    InternalUserController(GetUserProfileUseCase getProfile, ListUserAccountsUseCase listAccounts,
                           GetUserPreferencesUseCase getPreferences) {
        this.getProfile = getProfile;
        this.listAccounts = listAccounts;
        this.getPreferences = getPreferences;
    }

    /**
     * Solo lo necesario para escribirle al usuario; nada de documento, teléfono ni roles. Incluye por
     * qué canales quiere sus avisos (Configuración > Notificaciones) para que notification-service los
     * respete: push, correo de recordatorios y promociones.
     */
    @GetMapping("/{userId}/contact")
    ContactResponse contact(@PathVariable long userId) {
        UserAccountView user = getProfile.getProfile(userId);
        PreferenceView preferences = getPreferences.getPreferences(userId);
        return new ContactResponse(user.id(), user.email(), user.firstName(), user.active(),
                preferences.notificationsEnabled(), preferences.emailRemindersEnabled(),
                preferences.promotionsEnabled());
    }

    /** Ids de las cuentas ADMIN activas; una cuenta desactivada no recibe avisos. */
    @GetMapping("/admins")
    List<Long> activeAdmins() {
        return listAccounts.listAccounts(new ListUserAccountsQuery(RoleCode.ADMIN, 0, ADMINS_PAGE_SIZE)).items().stream()
                .filter(UserAccountView::active)
                .map(UserAccountView::id)
                .toList();
    }

    record ContactResponse(long userId, String email, String firstName, boolean active, boolean pushEnabled,
                           boolean emailRemindersEnabled, boolean promotionsEnabled) {
    }
}
