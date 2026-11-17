package com.taller.portal.service;
import com.taller.portal.model.User; import org.springframework.context.annotation.Profile; import org.springframework.stereotype.Component;
/** Marcador seguro hasta integrar un proveedor real de correo o SMS fuera de desarrollo. */
@Component @Profile("!dev") public class NotificadorRecuperacionPendiente implements NotificadorRecuperacion { public void notificar(User usuario,String codigo){} }
