package com.lavarapido.security.application.usecase;

import com.lavarapido.security.domain.exception.UnknownPermissionException;
import com.lavarapido.security.domain.model.Permission;
import com.lavarapido.security.domain.model.RoleCode;
import com.lavarapido.security.domain.model.RolePermissions;
import com.lavarapido.security.domain.port.out.RolePermissionRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RolePermissionAdministrationServiceTest {

    private final InMemoryRolePermissions repository = new InMemoryRolePermissions();
    private final RolePermissionAdministrationService service = new RolePermissionAdministrationService(repository);

    @Test
    void replacesThePermissionsOfTheRole() {
        RolePermissions updated = service.updateRolePermissions(RoleCode.OPERATOR, List.of((short) 1, (short) 4));

        assertThat(updated.permissionIds()).containsExactly((short) 1, (short) 4);
        assertThat(repository.findRolePermissions(RoleCode.OPERATOR).permissionIds()).containsExactly((short) 1, (short) 4);
    }

    @Test
    void repeatedIdsAreSavedOnce() {
        RolePermissions updated = service.updateRolePermissions(RoleCode.OPERATOR, List.of((short) 1, (short) 1, (short) 3));

        assertThat(updated.permissionIds()).containsExactly((short) 1, (short) 3);
    }

    @Test
    void anEmptyListLeavesTheRoleWithoutPermissions() {
        service.updateRolePermissions(RoleCode.CLIENT, List.of());

        assertThat(repository.findRolePermissions(RoleCode.CLIENT).permissionIds()).isEmpty();
    }

    @Test
    void anUnknownPermissionIsRejectedAndNothingChanges() {
        assertThatThrownBy(() -> service.updateRolePermissions(RoleCode.OPERATOR, List.of((short) 1, (short) 999)))
                .isInstanceOf(UnknownPermissionException.class)
                .hasMessageContaining("999");

        // el rol conserva lo que tenía
        assertThat(repository.findRolePermissions(RoleCode.OPERATOR).permissionIds()).containsExactly((short) 1, (short) 3);
    }

    /** Catálogo y asignaciones iguales al seed de la migración 026. */
    private static final class InMemoryRolePermissions implements RolePermissionRepository {

        private final List<Permission> catalog = List.of(
                new Permission((short) 1, "VIEW_PANELS", "Ver paneles", "panel", "view"),
                new Permission((short) 2, "CREATE_RECORDS", "Crear registros", "record", "create"),
                new Permission((short) 3, "EDIT_DATA", "Editar datos", "record", "edit"),
                new Permission((short) 4, "DELETE", "Eliminar", "record", "delete"));
        private final Map<RoleCode, List<Short>> assigned = new EnumMap<>(Map.of(
                RoleCode.ADMIN, List.of((short) 1, (short) 2, (short) 3, (short) 4),
                RoleCode.OPERATOR, List.of((short) 1, (short) 3),
                RoleCode.CLIENT, List.of((short) 1)));

        @Override
        public List<Permission> findPermissions() {
            return catalog;
        }

        @Override
        public List<RolePermissions> findRolePermissions() {
            return assigned.entrySet().stream().map(e -> new RolePermissions(e.getKey(), e.getValue())).toList();
        }

        @Override
        public RolePermissions findRolePermissions(RoleCode role) {
            return new RolePermissions(role, assigned.get(role));
        }

        @Override
        public RolePermissions replacePermissions(RoleCode role, List<Short> permissionIds) {
            assigned.put(role, new ArrayList<>(permissionIds));
            return new RolePermissions(role, permissionIds);
        }
    }
}
