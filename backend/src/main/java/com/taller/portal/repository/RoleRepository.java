package com.taller.portal.repository;
import com.taller.portal.model.*; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface RoleRepository extends JpaRepository<Role,Long>{ Optional<Role> findByNombre(RoleName nombre); }
