package com.lavarapido.security.domain.model;

import com.lavarapido.security.domain.exception.InvalidValueException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueObjectsTest {

    @Nested
    class EmailAddressTest {

        @Test
        void normalizesCaseAndSurroundingSpaces() {
            assertThat(EmailAddress.of("  Ana.Perez@Gmail.COM ").value()).isEqualTo("ana.perez@gmail.com");
        }

        @Test
        void twoSpellingsOfTheSameAddressAreEqual() {
            assertThat(EmailAddress.of("ANA@gmail.com")).isEqualTo(EmailAddress.of("ana@gmail.com"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"ana", "ana@", "@gmail.com", "ana@gmail", "ana perez@gmail.com"})
        void rejectsMalformedAddresses(String raw) {
            assertThatThrownBy(() -> EmailAddress.of(raw))
                    .isInstanceOf(InvalidValueException.class)
                    .extracting("code").isEqualTo("INVALID_EMAIL");
        }
    }

    @Nested
    class DocumentNumberTest {

        @Test
        void removesDotsAndSpacesTypedByHumans() {
            assertThat(DocumentNumber.of("1.023 456.789").value()).isEqualTo("1023456789");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"1234", "123456789012345678901", "12-34-56"})
        void rejectsInvalidDocuments(String raw) {
            assertThatThrownBy(() -> DocumentNumber.of(raw)).isInstanceOf(InvalidValueException.class);
        }
    }

    @Nested
    class PhoneNumberTest {

        @Test
        void acceptsMobileAndStripsSeparators() {
            assertThat(PhoneNumber.ofNullable("300 123-4567").value()).isEqualTo("3001234567");
        }

        @Test
        void blankMeansNoPhone() {
            assertThat(PhoneNumber.ofNullable("  ")).isNull();
        }

        @Test
        void rejectsLetters() {
            assertThatThrownBy(() -> PhoneNumber.ofNullable("30012abc67")).isInstanceOf(InvalidValueException.class);
        }
    }

    @Nested
    class PersonNameTest {

        @Test
        void collapsesInnerWhitespace() {
            PersonName name = PersonName.of("  María   José ", "Pérez  Gómez");
            assertThat(name.fullName()).isEqualTo("María José Pérez Gómez");
        }

        @Test
        void rejectsNamesLongerThanTheColumn() {
            assertThatThrownBy(() -> PersonName.of("a".repeat(61), "Pérez"))
                    .isInstanceOf(InvalidValueException.class);
        }
    }

    @Nested
    class HashedPasswordTest {

        @Test
        void neverPrintsItsValue() {
            assertThat(new HashedPassword("$2a$10$secret").toString()).doesNotContain("secret");
        }
    }
}
