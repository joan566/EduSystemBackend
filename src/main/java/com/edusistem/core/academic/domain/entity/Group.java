package com.edusistem.core.academic.domain.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Group {
    private Long id;
    private Long gradeId;
    private String name;
    private int academicYear;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
