package com.lavarapido.security.infrastructure.adapter.out.notification;

import com.lavarapido.security.domain.model.EmailAddress;
import com.lavarapido.security.domain.model.PersonName;
import com.lavarapido.security.domain.port.out.ResetCodeNotifier;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Envía el código de recuperación al correo del usuario por SMTP (Gmail, Brevo, Mailpit...).
 * Se activa con {@code app.mail.enabled=true}.
 *
 * <p>Es asíncrono a propósito: la respuesta de "olvidé mi contraseña" tarda lo mismo exista o no
 * la cuenta, así el tiempo de respuesta no revela qué correos están registrados. Si el envío falla
 * se registra en el log (sin el código) y el usuario puede pedir otro.
 */
@Component
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true")
class EmailResetCodeNotifier implements ResetCodeNotifier {

    private static final Logger log = LoggerFactory.getLogger(EmailResetCodeNotifier.class);

    /** Las horas se muestran en la hora de Colombia; en la base todo va en UTC. */
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("America/Bogota");
    /** Idioma fijo: el correo sale igual sin importar el idioma del servidor ("10:15 a. m."). */
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-CO"));

    static final String SUBJECT = "Tu código para recuperar la contraseña — LavaRápido";

    private final JavaMailSender mailSender;
    private final String from;
    private final Clock clock;

    EmailResetCodeNotifier(JavaMailSender mailSender, @Value("${app.mail.from}") String from, Clock clock) {
        this.mailSender = mailSender;
        this.from = from;
        this.clock = clock;
    }

    @Async
    @Override
    public void send(EmailAddress to, PersonName name, String code, Instant expiresAt) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to.value());
            helper.setSubject(SUBJECT);
            long minutes = Math.max(1, Duration.between(clock.instant(), expiresAt).toMinutes());
            // Texto plano + HTML: los clientes de correo que no muestran HTML igual ven el código.
            helper.setText(plainText(name, code, minutes, expiresAt), html(name, code, minutes, expiresAt));
            mailSender.send(message);
            log.info("Password recovery email sent (expires {})", expiresAt);
        } catch (MessagingException | MailException exception) {
            // Nunca se registra el código ni el correo completo: un log no debe servir para entrar.
            log.error("Could not send the password recovery email: {}", exception.getMessage());
        }
    }

    static String plainText(PersonName name, String code, long minutes, Instant expiresAt) {
        return """
                Hola %s,

                Recibimos una solicitud para recuperar la contraseña de tu cuenta de LavaRápido.

                Tu código es: %s

                Vence en %d minutos (a las %s) y solo se puede usar una vez.

                Si no fuiste tú, ignora este correo: tu contraseña sigue siendo la misma.
                """.formatted(name.firstName(), code, minutes, displayTime(expiresAt));
    }

    static String html(PersonName name, String code, long minutes, Instant expiresAt) {
        // El nombre lo escribió el usuario: se escapa para que no pueda inyectar HTML en el correo.
        String safeName = HtmlUtils.htmlEscape(name.firstName());
        return """
                <!doctype html>
                <html lang="es">
                <body style="margin:0;padding:24px;background:#f4f6f8;font-family:Arial,Helvetica,sans-serif;color:#1f2933;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                    <tr><td align="center">
                      <table role="presentation" width="480" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:12px;padding:32px;">
                        <tr><td>
                          <h1 style="margin:0 0 16px;font-size:20px;color:#0b5ed7;">LavaRápido</h1>
                          <p style="margin:0 0 12px;font-size:15px;">Hola %s,</p>
                          <p style="margin:0 0 20px;font-size:15px;">
                            Recibimos una solicitud para recuperar la contraseña de tu cuenta.
                            Escribe este código en la pantalla de verificación:
                          </p>
                          <p style="margin:0 0 20px;text-align:center;font-size:32px;font-weight:bold;
                                    letter-spacing:8px;color:#0b5ed7;">%s</p>
                          <p style="margin:0 0 12px;font-size:14px;color:#52606d;">
                            Vence en <strong>%d minutos</strong> (a las %s) y solo se puede usar una vez.
                          </p>
                          <p style="margin:0;font-size:13px;color:#7b8794;">
                            Si no fuiste tú, ignora este correo: tu contraseña sigue siendo la misma.
                          </p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(safeName, code, minutes, displayTime(expiresAt));
    }

    static String displayTime(Instant instant) {
        // Java usa espacios "duros" (U+00A0 / U+202F) en "a. m."; algunos clientes de correo los
        // muestran raro, así que se cambian por espacios normales.
        return TIME_FORMAT.format(instant.atZone(DISPLAY_ZONE)).replace(' ', ' ').replace(' ', ' ');
    }
}
