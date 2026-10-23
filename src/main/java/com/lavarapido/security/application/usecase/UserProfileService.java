package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.event.PasswordChanged;
import com.lavarapido.security.domain.exception.IncorrectCurrentPasswordException;
import com.lavarapido.security.domain.exception.SamePasswordException;
import com.lavarapido.security.domain.exception.UserNotFoundException;
import com.lavarapido.security.domain.model.PersonName;
import com.lavarapido.security.domain.model.PhoneNumber;
import com.lavarapido.security.domain.model.UserAccount;
import com.lavarapido.security.domain.port.in.ChangePasswordUseCase;
import com.lavarapido.security.domain.port.in.GetUserProfileUseCase;
import com.lavarapido.security.domain.port.in.UpdateUserProfileUseCase;
import com.lavarapido.security.domain.port.in.UserAccountView;
import com.lavarapido.security.domain.port.out.DomainEventPublisher;
import com.lavarapido.security.domain.port.out.PasswordHasher;
import com.lavarapido.security.domain.port.out.UserAccountRepository;
import com.lavarapido.security.domain.port.out.UserSessionRepository;
import com.lavarapido.security.domain.service.PasswordPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** Lo que el usuario con sesión puede ver y cambiar de su propia cuenta. */
@Service
@Transactional
public class UserProfileService implements GetUserProfileUseCase, UpdateUserProfileUseCase, ChangePasswordUseCase {

    private final UserAccountRepository accounts;
    private final UserSessionRepository sessions;
    private final PasswordHasher passwordHasher;
    private final PasswordPolicy passwordPolicy;
    private final DomainEventPublisher events;
    private final Clock clock;

    public UserProfileService(UserAccountRepository accounts, UserSessionRepository sessions,
                              PasswordHasher passwordHasher, PasswordPolicy passwordPolicy,
                              DomainEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.passwordHasher = passwordHasher;
        this.passwordPolicy = passwordPolicy;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public UserAccountView getProfile(long userId) {
        return UserAccountView.from(load(userId));
    }

    @Override
    public UserAccountView updateProfile(UpdateProfileCommand command) {
        UserAccount account = load(command.userId());
        account.person().rename(PersonName.of(command.firstName(), command.lastName()));
        account.person().changePhone(PhoneNumber.ofNullable(command.phone()));
        return UserAccountView.from(accounts.save(account));
    }

    @Override
    public void changePassword(ChangePasswordCommand command) {
        UserAccount account = load(command.userId());

        if (!passwordHasher.matches(command.currentPassword(), account.passwordHash())) {
            throw new IncorrectCurrentPasswordException();
        }
        passwordPolicy.validate(command.newPassword());
        if (passwordHasher.matches(command.newPassword(), account.passwordHash())) {
            throw new SamePasswordException();
        }

        Instant now = clock.instant();
        account.changePassword(passwordHasher.hash(command.newPassword()));
        accounts.save(account);
        // si alguien más tenía la contraseña vieja, sus sesiones dejan de servir; este equipo sigue adentro
        sessions.revokeAllExcept(account.id(), command.sessionId(), now);
        events.publish(new PasswordChanged(account.id(), PasswordChanged.Reason.CHANGED_BY_USER, now));
    }

    private UserAccount load(long userId) {
        return accounts.findById(userId).orElseThrow(UserNotFoundException::new);
    }
}
