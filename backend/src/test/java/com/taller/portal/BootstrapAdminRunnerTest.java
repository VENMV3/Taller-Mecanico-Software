package com.taller.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.taller.portal.config.BootstrapAdminRunner;
import com.taller.portal.model.Role;
import com.taller.portal.model.RoleName;
import com.taller.portal.model.User;
import com.taller.portal.repository.RoleRepository;
import com.taller.portal.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class BootstrapAdminRunnerTest {
    private final UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
    private final RoleRepository roles = org.mockito.Mockito.mock(RoleRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void creaAdministradorActivoUnaSolaVez() throws Exception {
        Role adminRole = new Role(RoleName.ADMINISTRADOR_SISTEMA, "Administración");
        List<User> almacenados = new ArrayList<>();
        when(users.findAll()).thenAnswer(invocation -> almacenados);
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            almacenados.add(user);
            return user;
        });
        when(roles.findByNombre(RoleName.ADMINISTRADOR_SISTEMA)).thenReturn(Optional.of(adminRole));
        BootstrapAdminRunner runner = runner("admin@example.invalid", "password-segura");

        runner.run(null);
        runner.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        User admin = captor.getValue();
        assertEquals("admin@example.invalid", admin.email);
        assertEquals(RoleName.ADMINISTRADOR_SISTEMA, admin.rol.nombre);
        assertEquals(true, admin.activo);
        assertEquals(true, encoder.matches("password-segura", admin.passwordHash));
    }

    @Test
    void noDuplicaCuandoYaExisteAdministrador() throws Exception {
        User existente = new User();
        existente.rol = new Role(RoleName.ADMINISTRADOR_SISTEMA, "Administración");
        when(users.findAll()).thenReturn(List.of(existente));

        runner("admin@example.invalid", "password-segura").run(null);

        verify(users, never()).save(any());
    }

    @Test
    void rechazaContrasenaDeMenosDeDoceCaracteres() {
        when(users.findAll()).thenReturn(List.of());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> runner("admin@example.invalid", "corta").run(null));

        assertEquals("BOOTSTRAP_ADMIN_PASSWORD debe tener al menos 12 caracteres", exception.getMessage());
        verify(users, never()).save(any());
    }

    private BootstrapAdminRunner runner(String email, String password) {
        return new BootstrapAdminRunner(users, roles, encoder, email, password);
    }
}
