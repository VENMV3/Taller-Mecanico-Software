package com.taller.portal.config;

import com.taller.portal.model.RoleName;
import com.taller.portal.model.User;
import com.taller.portal.repository.RoleRepository;
import com.taller.portal.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Crea el primer administrador del sistema cuando se configura durante el despliegue. */
@Component
@Order(100)
public class BootstrapAdminRunner implements ApplicationRunner {
    private static final int MINIMUM_PASSWORD_LENGTH = 12;

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    /** Construye el ejecutor de alta inicial del administrador. */
    public BootstrapAdminRunner(UserRepository users, RoleRepository roles, PasswordEncoder encoder,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    /** Crea un administrador activo solamente cuando aún no existe uno. */
    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank() || existeAdministrador()) {
            return;
        }
        if (password.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD debe tener al menos 12 caracteres");
        }

        User admin = new User();
        admin.nombre = "Administrador del sistema";
        admin.email = email;
        admin.celular = celularTecnico(email);
        admin.passwordHash = encoder.encode(password);
        admin.rol = roles.findByNombre(RoleName.ADMINISTRADOR_SISTEMA)
                .orElseThrow(() -> new IllegalStateException("No existe el rol ADMINISTRADOR_SISTEMA"));
        admin.activo = true;
        admin.emailVerificado = true;
        users.save(admin);
    }

    private boolean existeAdministrador() {
        return users.findAll().stream().anyMatch(user -> user.rol != null
                && user.rol.nombre == RoleName.ADMINISTRADOR_SISTEMA);
    }

    private String celularTecnico(String correo) {
        return "9" + String.format("%09d", Math.floorMod(correo.hashCode(), 1_000_000_000));
    }
}
