package com.edusistem.core.user.infrastructure.repository;

import com.edusistem.core.user.infrastructure.entity.UserRoleEntity;
import com.edusistem.core.user.infrastructure.entity.UserRoleId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataUserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleId> {

    @Query("select r.name from UserRoleEntity ur join RoleEntity r on r.id = ur.roleId where ur.userId = :userId")
    List<String> findRoleNamesByUserId(@Param("userId") Long userId);

    List<UserRoleEntity> findByUserId(Long userId);
}
