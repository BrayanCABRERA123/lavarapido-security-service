package com.lavarapido.security.domain.port.in;

/** El dueño de la cuenta ve y cambia su dirección. Vacía significa que no tiene. */
public interface PersonAddressUseCase {

    int MAX_LENGTH = 200;

    String getAddress(long userId);

    /** Devuelve la dirección guardada (sin espacios a los lados); "" si se borró. */
    String updateAddress(long userId, String address);
}
