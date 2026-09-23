package com.edusistem.core.academic.domain.entity;

import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.time.LocalDate;
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
public class AcademicPeriod {
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void validateDates() {
        if (endDate.isBefore(startDate)) {
            throw new InvalidRequestException("INVALID_PERIOD_DATES", "endDate must not be before startDate");
        }
    }
}
