package com.taller.portal.repository;
import com.taller.portal.model.User; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface UserRepository extends JpaRepository<User,Long>{ Optional<User> findByEmail(String email); Optional<User> findByEmailOrCelular(String email,String celular); boolean existsByEmail(String email); boolean existsByCelular(String celular); }
