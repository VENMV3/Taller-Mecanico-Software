package com.taller.portal.service;
import com.taller.portal.model.User;
/** Punto de integración para entregar un código de recuperación por correo o SMS. */
public interface NotificadorRecuperacion { void notificar(User usuario,String codigo); }
