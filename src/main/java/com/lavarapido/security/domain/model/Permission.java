package com.lavarapido.security.domain.model;

/** Una fila de {@code security.permission} (catálogo fijo, migración 026). */
public record Permission(short id, String code, String name, String resource, String action) {
}
