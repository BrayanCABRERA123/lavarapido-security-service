package com.lavarapido.security.infrastructure.adapter.in.web;

import com.lavarapido.security.domain.port.in.ChangePasswordUseCase;
import com.lavarapido.security.domain.port.in.ChangePasswordUseCase.ChangePasswordCommand;
import com.lavarapido.security.domain.port.in.GetUserPreferencesUseCase;
import com.lavarapido.security.domain.port.in.GetUserProfileUseCase;
import com.lavarapido.security.domain.port.in.UpdateUserPreferencesUseCase;
import com.lavarapido.security.domain.port.in.UpdateUserPreferencesUseCase.UpdatePreferencesCommand;
import com.lavarapido.security.domain.port.in.UpdateUserProfileUseCase;
import com.lavarapido.security.domain.port.in.UpdateUserProfileUseCase.UpdateProfileCommand;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.ChangePasswordRequest;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.PreferencesRequest;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.PreferencesResponse;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.UpdateProfileRequest;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * La cuenta propia de quien llama. El id del usuario siempre sale del token verificado, nunca de
 * la URL ni del cuerpo, así nadie puede leer ni editar el perfil de otro (regla de propiedad, ADR-006).
 */
@Tag(name = "My account", description = "Perfil, contraseña y preferencias del usuario con sesión")
@RestController
@RequestMapping("/api/v1/users/me")
class UserProfileController {

    private final GetUserProfileUseCase getProfile;
    private final UpdateUserProfileUseCase updateProfile;
    private final ChangePasswordUseCase changePassword;
    private final GetUserPreferencesUseCase getPreferences;
    private final UpdateUserPreferencesUseCase updatePreferences;

    UserProfileController(GetUserProfileUseCase getProfile, UpdateUserProfileUseCase updateProfile,
                          ChangePasswordUseCase changePassword, GetUserPreferencesUseCase getPreferences,
                          UpdateUserPreferencesUseCase updatePreferences) {
        this.getProfile = getProfile;
        this.updateProfile = updateProfile;
        this.changePassword = changePassword;
        this.getPreferences = getPreferences;
        this.updatePreferences = updatePreferences;
    }

    @GetMapping
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(getProfile.getProfile(userId(jwt)));
    }

    @PatchMapping
    UserResponse updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(updateProfile.updateProfile(new UpdateProfileCommand(userId(jwt),
                request.firstName(), request.lastName(), request.phone())));
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        changePassword.changePassword(new ChangePasswordCommand(userId(jwt), request.currentPassword(),
                request.newPassword()));
    }

    @GetMapping("/preferences")
    PreferencesResponse preferences(@AuthenticationPrincipal Jwt jwt) {
        return PreferencesResponse.from(getPreferences.getPreferences(userId(jwt)));
    }

    @PutMapping("/preferences")
    PreferencesResponse updatePreferences(@AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody PreferencesRequest request) {
        return PreferencesResponse.from(updatePreferences.updatePreferences(new UpdatePreferencesCommand(
                userId(jwt), request.theme(), request.language(), request.notificationsEnabled())));
    }

    private static long userId(Jwt jwt) {
        return AuthenticatedUser.from(jwt).userId();
    }
}
