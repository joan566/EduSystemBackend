package com.edusistem.core.exports.application.schoolsetup;

import com.edusistem.core.academic.domain.vo.GroupView;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.activity.domain.vo.ActivityView;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.student.domain.entity.Student;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Datos del profesor que usan varias hojas del export, cargados una sola vez.
 * @param groups          grupos tal como los devuelve la búsqueda
 * @param classes         teaching periods ordenados por año, grado, grupo, asignatura y fecha de inicio
 * @param rosterByGroup   estudiantes activos con número de identificación, por grupo
 * @param visibleScales   escalas que el profesor puede usar (propias y del sistema)
 * @param activitiesByClass actividades de cada teaching period
 */
public record SchoolSetupExportContext(Long teacherId, List<GroupView> groups, List<TeachingPeriodView> classes,
                                       Map<Long, List<Student>> rosterByGroup, List<GradingScale> visibleScales,
                                       Map<Long, List<ActivityView>> activitiesByClass) {

    public List<String> scaleOptions() {
        return visibleScales.stream().map(GradingScale::displayName).distinct().toList();
    }

    public List<ActivityView> activities(TeachingPeriodView teachingPeriod) {
        return activitiesByClass.getOrDefault(teachingPeriod.id(), List.of());
    }

    /** Número de identificación por id de estudiante, de los matriculados en el grupo de la clase. */
    public Map<Long, String> identificationById(TeachingPeriodView teachingPeriod) {
        return rosterByGroup.getOrDefault(teachingPeriod.groupId(), List.of()).stream()
                .collect(Collectors.toMap(Student::getId, Student::getIdentificationNumber));
    }

    public int enrollments() {
        return rosterByGroup.values().stream().mapToInt(List::size).sum();
    }
}
