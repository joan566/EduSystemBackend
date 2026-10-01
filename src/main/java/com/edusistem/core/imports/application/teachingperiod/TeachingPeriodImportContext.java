package com.edusistem.core.imports.application.teachingperiod;

import com.edusistem.core.imports.application.contracts.StudentRoster;
import com.edusistem.core.imports.application.support.RowErrors;
import com.edusistem.core.imports.domain.vo.ParsedWorkbook;
import java.util.Map;

/** Estado de UNA importación del libro de un teaching period, compartido por sus hojas. */
public final class TeachingPeriodImportContext {

    private final Long teacherId;
    private final Long teachingPeriodId;
    private final Long groupId;
    private final ParsedWorkbook workbook;
    private final StudentRoster roster;
    private final RowErrors errors = new RowErrors();
    private Map<String, Long> studentIdByIdentification;

    public TeachingPeriodImportContext(Long teacherId, Long teachingPeriodId, Long groupId, ParsedWorkbook workbook,
                                       StudentRoster roster) {
        this.teacherId = teacherId;
        this.teachingPeriodId = teachingPeriodId;
        this.groupId = groupId;
        this.workbook = workbook;
        this.roster = roster;
    }

    public Long teacherId() {
        return teacherId;
    }

    public Long teachingPeriodId() {
        return teachingPeriodId;
    }

    public Long groupId() {
        return groupId;
    }

    public ParsedWorkbook workbook() {
        return workbook;
    }

    public RowErrors errors() {
        return errors;
    }

    /**
     * Estudiantes activos del grupo por número de identificación. Se carga la primera vez que se pide (después de
     * aplicar la hoja Estudiantes) y se reutiliza.
     */
    public Map<String, Long> roster() {
        if (studentIdByIdentification == null) {
            studentIdByIdentification = roster.activeByIdentificationInGroup(groupId);
        }
        return studentIdByIdentification;
    }
}
