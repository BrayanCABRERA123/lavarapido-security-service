package com.lavarapido.security.domain.port.out;

/** Genera el código numérico de un solo uso que el usuario digita en la pantalla de recuperación. */
public interface ResetCodeGenerator {

    String generate();
}
