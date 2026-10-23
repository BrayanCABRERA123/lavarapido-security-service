package com.lavarapido.security.infrastructure.adapter.out.notification;

import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.PersonName;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailResetCodeNotifierTest {

    private static final Instant NOW = Instant.parse("2026-09-30T15:00:00Z");

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final EmailResetCodeNotifier notifier = new EmailResetCodeNotifier(mailSender,
            "LavaRápido <soporte@lavarapido.com>", Clock.fixed(NOW, ZoneOffset.UTC));

    EmailResetCodeNotifierTest() {
        when(mailSender.createMimeMessage()).thenAnswer(ignored -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void sendsTheCodeToTheUsersEmail() throws Exception {
        notifier.send(EmailAddress.of("ana@gmail.com"), PersonName.of("Ana", "Pérez"), "482913",
                NOW.plusSeconds(15 * 60));

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        MimeMessage message = sent.getValue();

        assertThat(message.getAllRecipients()).extracting(Object::toString).containsExactly("ana@gmail.com");
        assertThat(message.getSubject()).isEqualTo(EmailResetCodeNotifier.SUBJECT);
        String raw = rawContent(message);
        assertThat(raw).contains("482913").contains("15 minutos");
    }

    @Test
    void htmlEscapesTheNameTypedByTheUser() {
        String html = EmailResetCodeNotifier.html(PersonName.of("<script>x</script>", "Pérez"), "482913", 15, NOW);

        assertThat(html).doesNotContain("<script>").contains("&lt;script&gt;");
    }

    @Test
    void showsTheExpiryInColombianTime() {
        // 15:15 UTC = 10:15 a. m. en Bogotá (UTC-5)
        String text = EmailResetCodeNotifier.plainText(PersonName.of("Ana", "Pérez"), "482913", 15,
                NOW.plusSeconds(15 * 60));

        assertThat(text).contains("a las 10:15 a. m.");
    }

    @Test
    void aFailingSmtpServerDoesNotBreakTheRecoveryFlow() {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> notifier.send(EmailAddress.of("ana@gmail.com"), PersonName.of("Ana", "Pérez"),
                "482913", NOW.plusSeconds(900))).doesNotThrowAnyException();
    }

    private static String rawContent(MimeMessage message) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        message.writeTo(out);
        String raw = out.toString(StandardCharsets.UTF_8);
        // el cuerpo puede venir en quoted-printable: se quitan los saltos suaves para buscar texto
        return raw.replace("=\r\n", "").replace("=\n", "");
    }
}
