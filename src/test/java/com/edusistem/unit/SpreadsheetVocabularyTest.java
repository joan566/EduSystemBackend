package com.edusistem.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusistem.core.shared.domain.vo.PeriodWorkbookColumns;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SpreadsheetVocabularyTest {

    @Test
    void normalizeIgnoresAccentsCaseAndSeparators() {
        assertThat(SpreadsheetVocabulary.normalize("  Número de-identificación ")).isEqualTo("numero_de_identificacion");
        assertThat(SpreadsheetVocabulary.normalize("identification_number")).isEqualTo("identification_number");
    }

    @Test
    void sheetSpecAcceptsSpanishAndLegacyEnglishNames() {
        var spec = SpreadsheetVocabulary.STUDENTS;
        assertThat(spec.isNamed("Estudiantes")).isTrue();
        assertThat(spec.isNamed("Students")).isTrue();
        assertThat(spec.isNamed("Notas")).isFalse();
        assertThat(spec.canonicalColumn(SpreadsheetVocabulary.normalize("Número de identificación")))
                .isEqualTo("identification_number");
        assertThat(spec.canonicalColumn("identification_number")).isEqualTo("identification_number");
        assertThat(spec.canonicalColumn("otra_columna")).isEqualTo("otra_columna");
        assertThat(spec.ref("email")).isEqualTo("Estudiantes:Correo electrónico");
    }

    @Test
    void attendanceAcceptsSpanishAndEnglish() {
        assertThat(SpreadsheetVocabulary.attendanceLabel("PRESENT")).isEqualTo("Presente");
        assertThat(SpreadsheetVocabulary.attendanceStatusName("ausente")).contains("ABSENT");
        assertThat(SpreadsheetVocabulary.attendanceStatusName("EXCUSED")).contains("EXCUSED");
        assertThat(SpreadsheetVocabulary.attendanceStatusName("tarde")).isEmpty();
    }

    @Test
    void idIsReadFromNewAndLegacyHeaders() {
        String header = PeriodWorkbookColumns.activityHeader("Quiz #3", 12, BigDecimal.valueOf(5));
        assertThat(header).isEqualTo("Quiz #3 #12 (máx. 5)");
        assertThat(PeriodWorkbookColumns.idFromHeader(SpreadsheetVocabulary.normalize(header))).contains(12L);
        assertThat(PeriodWorkbookColumns.idFromHeader("taller_1_#12_(max_5)")).contains(12L);
        assertThat(PeriodWorkbookColumns.idFromHeader(PeriodWorkbookColumns.sessionHeader(LocalDate.of(2026, 2, 10), 34)))
                .contains(34L);
        assertThat(PeriodWorkbookColumns.idFromHeader("apellidos")).isEmpty();
    }

    @Test
    void daysOfWeekAcceptSpanishWithOrWithoutAccentsAbbreviationsAndEnglish() {
        assertThat(SpreadsheetVocabulary.dayLabel(DayOfWeek.WEDNESDAY)).isEqualTo("Miércoles");
        assertThat(SpreadsheetVocabulary.dayOfWeek("Miércoles")).contains(DayOfWeek.WEDNESDAY);
        assertThat(SpreadsheetVocabulary.dayOfWeek("miercoles")).contains(DayOfWeek.WEDNESDAY);
        assertThat(SpreadsheetVocabulary.dayOfWeek("Sáb.")).contains(DayOfWeek.SATURDAY);
        assertThat(SpreadsheetVocabulary.dayOfWeek("Lu")).contains(DayOfWeek.MONDAY);
        assertThat(SpreadsheetVocabulary.dayOfWeek("FRIDAY")).contains(DayOfWeek.FRIDAY);
        assertThat(SpreadsheetVocabulary.dayOfWeek("Feriado")).isEmpty();
    }

    @Test
    void percentHeadersNormalizeToTheirCanonicalColumn() {
        var classes = SchoolSetupSheets.spec(SchoolSetupSheets.CLASSES);
        assertThat(classes.canonicalColumn(SpreadsheetVocabulary.normalize("% Exámenes"))).isEqualTo("exams_weight");
        assertThat(classes.canonicalColumn(SpreadsheetVocabulary.normalize("% asistencia"))).isEqualTo("attendance_weight");
        assertThat(classes.canonicalColumn(SpreadsheetVocabulary.normalize("Nota minima"))).isEqualTo("passing_grade");
    }
}
