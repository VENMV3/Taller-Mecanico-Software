package com.taller.portal.service;
import com.taller.portal.model.User; import org.slf4j.*; import org.springframework.context.annotation.Profile; import org.springframework.stereotype.Component;
/** Notificador local que registra el código solo durante desarrollo. */
@Component @Profile("dev") public class NotificadorRecuperacionDev implements NotificadorRecuperacion { private static final Logger log=LoggerFactory.getLogger(NotificadorRecuperacionDev.class); public void notificar(User usuario,String codigo){log.info("Código de recuperación generado para usuario {}: {}",usuario.id,codigo);} }
