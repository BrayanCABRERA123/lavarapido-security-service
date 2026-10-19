package com.lavarapido.security.infrastructure.adapter.in.web.dto;

import com.lavarapido.security.domain.model.RoleCode;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull RoleCode role) {
}
