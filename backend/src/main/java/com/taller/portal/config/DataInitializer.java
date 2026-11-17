package com.taller.portal.config;

import com.taller.portal.model.Role;
import com.taller.portal.model.RoleName;
import com.taller.portal.model.User;
import com.taller.portal.repository.RoleRepository;
import com.taller.portal.repository.TallerRepository;
import com.taller.portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Inicializa el catálogo de roles y, solo en desarrollo, una cuenta técnica local. */
@Configuration
public class DataInitializer {

    /**
     * Crea los roles del dominio cuando aún no existen.
     *
     * @param roles repositorio del catálogo de roles
     * @return inicializador idempotente del catálogo
     */
    @Bean
    CommandLineRunner seedRoles(RoleRepository roles) {
        return args -> {
            for (RoleName role : RoleName.values()) {
                roles.findByNombre(role).orElseGet(() -> roles.save(new Role(role, descripcion(role))));
            }
        };
    }

    /**
     * Crea la cuenta técnica local solo cuando el perfil de desarrollo está activo.
     *
     * @param users repositorio de usuarios
     * @param roles repositorio del catálogo de roles
     * @param talleres repositorio de talleres
     * @param encoder codificador BCrypt para la contraseña configurada por entorno
     * @param password contraseña recibida desde DEV_TECHNICAL_ACCOUNT_PASSWORD
     * @return inicializador idempotente de la cuenta técnica de desarrollo
     */
    @Bean
    @Profile("dev")
    CommandLineRunner seedTechnicalAccount(
            UserRepository users,
            RoleRepository roles,
            TallerRepository talleres,
            PasswordEncoder encoder,
            @Value("${app.technical-account-password}") String password) {
        return args -> {
            User user = users.findByEmail("desarrollador@taller.local").orElseGet(User::new);
            user.nombre = "Administrador del sistema";
            user.email = "desarrollador@taller.local";
            user.celular = "0000000000";
            user.passwordHash = encoder.encode(password);
            user.emailVerificado = true;
            user.rol = roles.findByNombre(RoleName.ADMINISTRADOR_SISTEMA).orElseThrow();
            user.taller = talleres.findByIdempotencyKey("semilla-motor-nebula-v1").orElse(null);
            users.save(user);
        };
    }

    /**
     * Obtiene la descripción visible de un rol predefinido.
     *
     * @param role rol cuyo texto se solicita
     * @return descripción del rol
     */
    private String descripcion(RoleName role) {
        return switch (role) {
            case DUENO -> "Administración total";
            case MECANICO -> "Órdenes de servicio";
            case CLIENTE -> "Consulta de sus vehículos";
            case SECRETARIA -> "Agenda y clientes";
            case DESARROLLADOR -> "Gestión técnica y cuentas";
            case ADMINISTRADOR_SISTEMA -> "Registro y administración del sistema";
            case RECEPCIONISTA -> "Registro y atención de clientes";
        };
    }
}
