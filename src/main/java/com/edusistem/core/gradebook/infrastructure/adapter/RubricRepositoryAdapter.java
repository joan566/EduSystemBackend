package com.edusistem.core.gradebook.infrastructure.adapter;

import com.edusistem.core.gradebook.domain.entity.RubricCriterion;
import com.edusistem.core.gradebook.domain.entity.RubricScore;
import com.edusistem.core.gradebook.domain.outputports.RubricRepositoryPort;
import com.edusistem.core.gradebook.infrastructure.entity.RubricCriterionEntity;
import com.edusistem.core.gradebook.infrastructure.entity.RubricScoreEntity;
import com.edusistem.core.gradebook.infrastructure.repository.SpringDataRubricCriterionRepository;
import com.edusistem.core.gradebook.infrastructure.repository.SpringDataRubricScoreRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RubricRepositoryAdapter implements RubricRepositoryPort {

    private final SpringDataRubricCriterionRepository criteria;
    private final SpringDataRubricScoreRepository scores;

    public RubricRepositoryAdapter(SpringDataRubricCriterionRepository criteria, SpringDataRubricScoreRepository scores) {
        this.criteria = criteria;
        this.scores = scores;
    }

    @Override
    public List<RubricCriterion> findCriteria(Long evaluationId) {
        return criteria.findByEvaluationIdOrderByPositionAscIdAsc(evaluationId).stream()
                .map(RubricRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<RubricCriterion> saveCriteria(List<RubricCriterion> list) {
        List<RubricCriterionEntity> entities = list.stream().map(c -> {
            RubricCriterionEntity e = c.getId() == null ? new RubricCriterionEntity()
                    : criteria.findById(c.getId()).orElseGet(RubricCriterionEntity::new);
            e.setEvaluationId(c.getEvaluationId());
            e.setPosition(c.getPosition());
            e.setName(c.getName());
            e.setWeight(c.getWeight());
            return e;
        }).toList();
        return criteria.saveAll(entities).stream().map(RubricRepositoryAdapter::toDomain).toList();
    }

    @Override
    public void deleteCriteria(Collection<Long> criterionIds) {
        if (!criterionIds.isEmpty()) {
            // Los puntajes se borran en cascada (FK), pero se sacan también del contexto de persistencia.
            criteria.deleteAllByIdInBatch(criterionIds);
        }
    }

    @Override
    public List<RubricScore> findScores(Long evaluationId, Long studentId) {
        return scores.findByEvaluationAndStudent(evaluationId, studentId).stream()
                .map(s -> RubricScore.builder().id(s.getId()).criterionId(s.getCriterionId())
                        .studentId(s.getStudentId()).score(s.getScore()).build()).toList();
    }

    @Override
    public void saveScores(List<RubricScore> list) {
        scores.saveAll(list.stream().map(s -> {
            RubricScoreEntity e = s.getId() == null ? new RubricScoreEntity()
                    : scores.findById(s.getId()).orElseGet(RubricScoreEntity::new);
            e.setCriterionId(s.getCriterionId());
            e.setStudentId(s.getStudentId());
            e.setScore(s.getScore());
            return e;
        }).toList());
    }

    private static RubricCriterion toDomain(RubricCriterionEntity e) {
        return RubricCriterion.builder().id(e.getId()).evaluationId(e.getEvaluationId()).position(e.getPosition())
                .name(e.getName()).weight(e.getWeight()).build();
    }
}
