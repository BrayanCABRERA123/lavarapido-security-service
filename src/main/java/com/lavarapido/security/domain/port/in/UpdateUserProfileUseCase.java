package com.lavarapido.security.domain.port.in;

/** El dueño edita su nombre y teléfono. El correo (su usuario) y el documento no cambian. */
public interface UpdateUserProfileUseCase {

    record UpdateProfileCommand(long userId, String firstName, String lastName, String phone) {
    }

    UserAccountView updateProfile(UpdateProfileCommand command);
}
