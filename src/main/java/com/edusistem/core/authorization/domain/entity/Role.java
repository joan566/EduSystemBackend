package com.edusistem.core.authorization.domain.entity;

import com.edusistem.core.authorization.domain.enums.RoleName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    private Long id;
    private RoleName name;
    private String description;
}
