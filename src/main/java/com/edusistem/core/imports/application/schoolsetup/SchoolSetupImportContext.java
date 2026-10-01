package com.edusistem.core.imports.application.schoolsetup;

import com.edusistem.core.activity.domain.vo.ActivityView;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionView;
import com.edusistem.core.imports.application.contracts.StudentRoster;
import com.edusistem.core.imports.application.contracts.TeachingPeriodCatalog;
import com.edusistem.core.imports.application.support.ImportProgress;
import com.edusistem.core.imports.application.support.RowErrors;
import com.edusistem.core.imports.domain.vo.ParsedWorkbook;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Estado de UNA importación de configuración escolar, compartido por las hojas: el libro, los errores acumulados y
 * cachés de lo que ya se resolvió (para no consultar la base de datos fila por fila). Lo que una hoja crea (clases,
 * actividades, sesiones) queda en la caché para las hojas siguientes.
 */
public final class SchoolSetupImportContext {

    /** Actividad ya existente o recién creada, con lo necesario para validar sus notas. */
    public record ActivityRef(Long activityId, BigDecimal maximumScore) {
    }

    private final Long teacherId;
    private final ParsedWorkbook workbook;
    private final ImportProgress progress;
    private final RowErrors errors = new RowErrors();
    private final Map<ClassKey, Long> teachingPeriodByClass = new HashMap<>();
    private final Map<Long, Map<String, ActivityRef>> activityByPeriodThenName = new HashMap<>();
    private final Map<Long, Map<String, Long>> studentByPeriodThenIdentification = new HashMap<>();
    private final Map<Long, Map<LocalDate, Long>> sessionByPeriodThenDate = new HashMap<>();

    public SchoolSetupImportContext(Long teacherId, ParsedWorkbook workbook, ImportProgress progress) {
        this.teacherId = teacherId;
        this.workbook = workbook;
        this.progress = progress;
    }

    public Long teacherId() {
        return teacherId;
    }

    public ParsedWorkbook workbook() {
        return workbook;
    }

    public RowErrors errors() {
        return errors;
    }

    public ImportProgress progress() {
        return progress;
    }

    public Optional<Long> knownTeachingPeriod(ClassKey key) {
        return Optional.ofNullable(teachingPeriodByClass.get(key));
    }

    public void rememberTeachingPeriod(ClassKey key, Long teachingPeriodId) {
        teachingPeriodByClass.put(key, teachingPeriodId);
    }

    /** Actividades del teaching period por nombre (mutable: las que se crean se agregan aquí). */
    public Map<String, ActivityRef> activities(Long teachingPeriodId, TeachingPeriodCatalog catalog) {
        return activityByPeriodThenName.computeIfAbsent(teachingPeriodId, id -> {
            Map<String, ActivityRef> byName = new HashMap<>();
            for (ActivityView a : catalog.activities(id)) {
                byName.put(a.name(), new ActivityRef(a.activityId(), a.maximumScore()));
            }
            return byName;
        });
    }

    /** Sesiones de asistencia del teaching period por fecha (mutable: las que se crean se agregan aquí). */
    public Map<LocalDate, Long> sessions(Long teachingPeriodId, TeachingPeriodCatalog catalog) {
        return sessionByPeriodThenDate.computeIfAbsent(teachingPeriodId, id -> {
            Map<LocalDate, Long> byDate = new HashMap<>();
            for (AttendanceSessionView s : catalog.sessions(id)) {
                byDate.put(s.sessionDate(), s.sessionId());
            }
            return byDate;
        });
    }

    /** Estudiantes activos del grupo del teaching period por número de identificación. */
    public Map<String, Long> roster(Long teachingPeriodId, StudentRoster roster) {
        return studentByPeriodThenIdentification.computeIfAbsent(teachingPeriodId,
                roster::activeByIdentificationInTeachingPeriod);
    }
}
