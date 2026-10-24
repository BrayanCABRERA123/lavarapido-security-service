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
    void theLimitIsInBytesBecauseBcryptCountsBytes() {
        // SEC-11: 43 caracteres pero 83 bytes en UTF-8 (cada ñ ocupa 2); antes pasaba y el hash daba 500
        String accented = "A1!" + "ñ".repeat(40);
        assertThatThrownBy(() -> policy.validate(accented))
                .isInstanceOf(WeakPasswordException.class)
                .extracting("violations").asList().contains("MAX_LENGTH");

        // 36 ñ + "A1!" = 75 bytes; 34 ñ + "A1!" = 71 bytes
        assertThatThrownBy(() -> policy.validate("A1!" + "ñ".repeat(36))).isInstanceOf(WeakPasswordException.class);
        assertThatCode(() -> policy.validate("A1!" + "ñ".repeat(34))).doesNotThrowAnyException();
    }

    @Test
    void aPasswordThePolicyAcceptsCanAlwaysBeHashedWithBcrypt() {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder bcrypt =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4);
        String longest = "A1!" + "ñ".repeat(34) + "a";

        assertThatCode(() -> policy.validate(longest)).doesNotThrowAnyException();
        assertThatCode(() -> bcrypt.encode(longest)).doesNotThrowAnyException();
    }

    @Test
    void treatsNullAsEmpty() {
        assertThatThrownBy(() -> policy.validate(null)).isInstanceOf(WeakPasswordException.class);
    }
}
