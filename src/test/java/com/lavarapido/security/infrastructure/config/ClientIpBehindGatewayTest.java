package com.lavarapido.security.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SEC-11: con el application.yml real, Tomcat toma la IP del cliente de X-Forwarded-For cuando la
 * conexión viene de una IP local o privada (el gateway). Antes las sesiones guardaban la IP del gateway.
 * Solo levanta Tomcat y Spring MVC, sin base de datos.
 */
@SpringBootTest(classes = ClientIpBehindGatewayTest.EchoApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClientIpBehindGatewayTest {

    // @Configuration y no @SpringBootConfiguration: si no, los @WebMvcTest de este paquete la tomarían
    // como la aplicación y no cargarían los controladores
    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({TomcatServletWebServerAutoConfiguration.class,
            DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class})
    @RestController
    static class EchoApp {

        @GetMapping("/ip")
        String ip(HttpServletRequest request) {
            return request.getRemoteAddr();
        }
    }

    @LocalServerPort
    private int port;

    private String remoteAddrSeenWith(String forwardedFor) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/ip")).GET();
        if (forwardedFor != null) {
            request.header("X-Forwarded-For", forwardedFor);
        }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    @Test
    void fromTheGatewayTheClientIpComesFromXForwardedFor() throws Exception {
        assertThat(remoteAddrSeenWith("203.0.113.7")).isEqualTo("203.0.113.7");
    }

    @Test
    void withoutTheHeaderItIsTheConnection() throws Exception {
        assertThat(remoteAddrSeenWith(null)).isEqualTo("127.0.0.1");
    }
}
