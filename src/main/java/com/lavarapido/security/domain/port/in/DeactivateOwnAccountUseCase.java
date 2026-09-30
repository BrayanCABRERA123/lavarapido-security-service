package com.lavarapido.security.domain.port.in;

/**
 * El usuario "elimina" su propia cuenta desde el perfil. No se borra: se desactiva, así no puede
 * volver a entrar pero su historial (reservas, pagos, calificaciones) se conserva.
 */
public interface DeactivateOwnAccountUseCase {

    record DeactivateOwnAccountCommand(long userId, String currentPassword) {

        @Override
        public String toString() {
            return "DeactivateOwnAccountCommand[userId=" + userId + ", currentPassword=****]";
        }
    }

    void deactivateOwnAccount(DeactivateOwnAccountCommand command);
}
