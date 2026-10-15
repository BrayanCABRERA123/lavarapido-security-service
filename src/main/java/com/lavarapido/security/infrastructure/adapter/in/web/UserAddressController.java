package com.lavarapido.security.infrastructure.adapter.in.web;

import com.lavarapido.security.domain.port.in.PersonAddressUseCase;
import com.lavarapido.security.infrastructure.adapter.in.web.dto.AddressDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dirección de quien tiene la sesión (ADR-010). Cualquier rol con sesión. */
@RestController
@RequestMapping("/api/v1/users/me/address")
class UserAddressController {

    private final PersonAddressUseCase addresses;

    UserAddressController(PersonAddressUseCase addresses) {
        this.addresses = addresses;
    }

    @GetMapping
    AddressDto address(@AuthenticationPrincipal Jwt jwt) {
        return new AddressDto(addresses.getAddress(userId(jwt)));
    }

    @PutMapping
    AddressDto updateAddress(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddressDto request) {
        return new AddressDto(addresses.updateAddress(userId(jwt), request.address()));
    }

    private static long userId(Jwt jwt) {
        return AuthenticatedUser.from(jwt).userId();
    }
}
