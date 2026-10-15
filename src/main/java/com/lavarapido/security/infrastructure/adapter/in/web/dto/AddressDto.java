package com.lavarapido.security.infrastructure.adapter.in.web.dto;

import com.lavarapido.security.domain.port.in.PersonAddressUseCase;
import jakarta.validation.constraints.Size;

/** Dirección del perfil, en la petición y en la respuesta. Vacía o null la borra. */
public record AddressDto(@Size(max = PersonAddressUseCase.MAX_LENGTH) String address) {
}
