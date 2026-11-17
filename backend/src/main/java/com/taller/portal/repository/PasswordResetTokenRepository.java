package com.taller.portal.repository;
import com.taller.portal.model.*; import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken,Long>{
 Optional<PasswordResetToken> findFirstByUsuarioAndUsadoEnIsNullOrderByCreadoEnDesc(User usuario);
 @Modifying @Query("update PasswordResetToken t set t.usadoEn = CURRENT_TIMESTAMP where t.usuario = :user and t.usadoEn is null") int invalidarPendientes(@Param("user") User user);
}
