package com.edusistem.core.gradebook.infrastructure.adapter;

import com.edusistem.core.gradebook.domain.entity.StudentObservation;
import com.edusistem.core.gradebook.domain.outputports.StudentObservationRepositoryPort;
import com.edusistem.core.gradebook.infrastructure.entity.StudentObservationEntity;
import com.edusistem.core.gradebook.infrastructure.repository.SpringDataStudentObservationRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class StudentObservationRepositoryAdapter implements StudentObservationRepositoryPort {

    private final SpringDataStudentObservationRepository repository;

    public StudentObservationRepositoryAdapter(SpringDataStudentObservationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<StudentObservation> find(Long teachingPeriodId, Long studentId) {
        return repository.findByTeachingPeriodIdAndStudentId(teachingPeriodId, studentId)
                .map(StudentObservationRepositoryAdapter::toDomain);
    }

    @Override
    public StudentObservation save(StudentObservation o) {
        StudentObservationEntity e = o.getId() == null ? new StudentObservationEntity()
                : repository.findById(o.getId()).orElseGet(StudentObservationEntity::new);
        e.setTeachingPeriodId(o.getTeachingPeriodId());
        e.setStudentId(o.getStudentId());
        e.setText(o.getText());
        return toDomain(repository.saveAndFlush(e));
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private static StudentObservation toDomain(StudentObservationEntity e) {
        return StudentObservation.builder().id(e.getId()).teachingPeriodId(e.getTeachingPeriodId())
                .studentId(e.getStudentId()).text(e.getText()).updatedAt(e.getUpdatedAt()).build();
    }
}
