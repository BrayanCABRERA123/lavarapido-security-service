package com.lavarapido.security.domain.port.in;

public interface GetUserProfileUseCase {

    UserAccountView getProfile(long userId);
}
