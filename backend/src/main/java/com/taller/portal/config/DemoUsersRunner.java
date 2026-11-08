package com.taller.portal.config;

import com.taller.portal.model.*;
import com.taller.portal.repository.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Siembra manual e idempotente de identidades demo con el codificador BCrypt del portal. */
@Component @ConditionalOnProperty(name="app.demo-users.seed",havingValue="true")
public class DemoUsersRunner implements ApplicationRunner {
 private final UserRepository users; private final RoleRepository roles; private final TallerRepository talleres; private final PasswordEncoder encoder; private final ConfigurableApplicationContext context;
 /** Construye el ejecutor de siembra explícitamente activado. */ public DemoUsersRunner(UserRepository users,RoleRepository roles,TallerRepository talleres,PasswordEncoder encoder,ConfigurableApplicationContext context){this.users=users;this.roles=roles;this.talleres=talleres;this.encoder=encoder;this.context=context;}
 /** Crea solo las cuentas demo ausentes y finaliza sin dejar un servidor web. */ @Override public void run(ApplicationArguments args){Taller taller=talleres.findAll().stream().findFirst().orElseThrow(()->new IllegalStateException("No hay taller para usuarios demo"));crear("demo.recepcion@taller.local","5599000001",RoleName.RECEPCIONISTA,System.getenv("DEMO_RECEPCIONISTA_PASSWORD"),taller);if(roles.findByNombre(RoleName.DESARROLLADOR).isPresent())crear("demo.desarrollo@taller.local","5599000002",RoleName.DESARROLLADOR,System.getenv("DEMO_DESARROLLADOR_PASSWORD"),taller);System.exit(SpringApplication.exit(context));}
 private void crear(String email,String celular,RoleName rol,String password,Taller taller){if(password==null||password.isBlank())throw new IllegalStateException("Falta contraseña demo en entorno");var existente=users.findByEmail(email);if(existente.isPresent())return;User u=new User();u.nombre="Usuario Demo";u.email=email;u.celular=celular;u.passwordHash=encoder.encode(password);u.rol=roles.findByNombre(rol).orElseThrow();u.taller=taller;u.activo=true;u.emailVerificado=true;users.save(u);}
}
