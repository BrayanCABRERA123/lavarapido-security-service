package com.lavarapido.security.domain.service;

import com.lavarapido.security.domain.exception.WeakPasswordException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void acceptsPasswordMeetingEveryRule() {
        assertThatCode(() -> policy.validate("Lavado2026!")).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} violates {1}")
    @CsvSource({
            "Ab1!,         MIN_LENGTH",
            "lavado2026!,  UPPERCASE",
            "LavadoRapido!, DIGIT",
            "Lavado2026,   SPECIAL_CHARACTER",
            "'Lavado 2026!', NO_WHITESPACE"
    })
    void reportsTheBrokenRule(String password, String violation) {
        assertThatThrownBy(() -> policy.validate(password))
                .isInstanceOf(WeakPasswordException.class)
                .extracting("violations").asList().contains(violation);
    }

    @Test
    void rejectsPasswordsLongerThanBcryptCanHash() {
        assertThatThrownBy(() -> policy.validate("A1!" + "a".repeat(70)))
                .isInstanceOf(WeakPasswordException.class)
                .extracting("violations").asList().contains("MAX_LENGTH");
    }

    @Test
    void treatsNullAsEmpty() {
        assertThatThrownBy(() -> policy.validate(null)).isInstanceOf(WeakPasswordException.class);
    }
}
