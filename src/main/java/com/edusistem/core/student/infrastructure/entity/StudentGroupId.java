package com.edusistem.core.student.infrastructure.entity;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class StudentGroupId implements Serializable {
    private Long studentId;
    private Long groupId;
}
