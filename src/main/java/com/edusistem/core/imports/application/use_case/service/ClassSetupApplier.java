package com.edusistem.core.imports.application.use_case.service;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.inputports.ManageTeachingPeriodScheduleUseCase;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodScheduleRepositoryPort;
import com.edusistem.core.academic.domain.vo.ScheduledClassView;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.evaluation.domain.entity.EvaluationCategory;
import com.edusistem.core.evaluation.domain.enums.EvaluationCategoryCode;
import com.edusistem.core.evaluation.domain.outputports.EvaluationCategoryRepositoryPort;
import com.edusistem.core.grading.application.use_case.dtos.GradingCommands;
import com.edusistem.core.grading.domain.entity.GradingConfiguration;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.grading.domain.entity.GradingWeight;
import com.edusistem.core.grading.domain.inputports.ConfigureGradingUseCase;
import com.edusistem.core.grading.domain.outputports.GradingConfigurationRepositoryPort;
import com.edusistem.core.grading.domain.outputports.GradingScaleRepositoryPort;
import com.edusistem.core.imports.domain.vo.ImportRowError;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.exceptions.DomainException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuración de una clase que llega en el Excel de configuración escolar: escala, nota mínima y pesos (columnas
 * opcionales de la hoja Clases) y bloques de horario (hoja Horarios). Valida antes de guardar para devolver mensajes en
 * español por celda; lo que queda lo valida el caso de uso de siempre.
 */
public class ClassSetupApplier {

    private static final String CLASSES = SchoolSetupSheets.CLASSES;
    private static final String SCHEDULES = SchoolSetupSheets.SCHEDULES;
    private static final BigDecimal HUNDRED = GradingConfiguration.HUNDRED;
    /** "0-5", "0 - 10", "0 a 100", "1 al 10", "0 hasta 5". */
    private static final Pattern RANGE = Pattern.compile(
            "^(\\d+(?:[.,]\\d+)?)\\s*(?:-|–|a|al|hasta)\\s*(\\d+(?:[.,]\\d+)?)$", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final Map<EvaluationCategoryCode, String> WEIGHT_COLUMNS = Map.of(
            EvaluationCategoryCode.EXAMS, "exams_weight",
            EvaluationCategoryCode.ACTIVITIES, "activities_weight",
            EvaluationCategoryCode.ATTENDANCE, "attendance_weight");

    private final ConfigureGradingUseCase configureGrading;
    private final GradingConfigurationRepositoryPort configurations;
    private final GradingScaleRepositoryPort scales;
    private final EvaluationCategoryRepositoryPort categories;
    private final ManageTeachingPeriodScheduleUseCase manageSchedule;
    private final TeachingPeriodScheduleRepositoryPort schedules;
    private final TeachingPeriodRepositoryPort teachingPeriods;

    public ClassSetupApplier(ConfigureGradingUseCase configureGrading, GradingConfigurationRepositoryPort configurations,
                             GradingScaleRepositoryPort scales, EvaluationCategoryRepositoryPort categories,
                             ManageTeachingPeriodScheduleUseCase manageSchedule,
                             TeachingPeriodScheduleRepositoryPort schedules, TeachingPeriodRepositoryPort teachingPeriods) {
        this.configureGrading = configureGrading;
        this.configurations = configurations;
        this.scales = scales;
        this.categories = categories;
        this.manageSchedule = manageSchedule;
        this.schedules = schedules;
        this.teachingPeriods = teachingPeriods;
    }

    /** Configuración de notas pedida en una fila de Clases; {@code scale} y cada peso son nulos si la celda está vacía. */
    record GradingInput(GradingScale scale, BigDecimal passingGrade, Map<EvaluationCategoryCode, BigDecimal> weights) {

        boolean hasWeights() {
            return !weights.isEmpty();
        }
    }

    record ScheduleInput(DayOfWeek day, LocalTime start, LocalTime end, String room) {
    }

    /** Escalas para la lista desplegable de la hoja Clases ({@code teacherId} nulo: sólo las del sistema). */
    public List<String> scaleOptions(Long teacherId) {
        return scales.findVisibleTo(teacherId).stream().map(GradingScale::displayName).distinct().toList();
    }

    // ------------------------------------------------------------------ configuración de notas (hoja Clases)

    /**
     * Lee las columnas de configuración de notas; vacío si la fila no trae ninguna. Las celdas inválidas se agregan a
     * {@code errors} (quien llama compara su tamaño para saber si la fila falló).
     */
    Optional<GradingInput> readGrading(Long teacherId, SpreadsheetRow row, List<ImportRowError> errors) {
        String rawScale = row.get("grading_scale");
        String rawPassing = row.get("passing_grade");
        Map<EvaluationCategoryCode, String> rawWeights = new EnumMap<>(EvaluationCategoryCode.class);
        WEIGHT_COLUMNS.forEach((code, column) -> {
            String raw = row.get(column);
            if (raw != null) {
                rawWeights.put(code, raw);
            }
        });
        if (rawScale == null && rawPassing == null && rawWeights.isEmpty()) {
            return Optional.empty();
        }
        GradingScale scale = null;
        if (rawScale != null) {
            scale = resolveScale(teacherId, rawScale).orElse(null);
            if (scale == null) {
                errors.add(new ImportRowError(row.rowNumber(), ref(CLASSES, "grading_scale"),
                        ImportMessages.unknownScale(scaleOptions(teacherId))));
            }
        }
        BigDecimal passing = null;
        if (rawPassing != null) {
            passing = CellValues.decimal(rawPassing);
            if (passing == null) {
                errors.add(new ImportRowError(row.rowNumber(), ref(CLASSES, "passing_grade"), ImportMessages.NOT_A_NUMBER));
            }
        }
        Map<EvaluationCategoryCode, BigDecimal> weights = new EnumMap<>(EvaluationCategoryCode.class);
        rawWeights.forEach((code, raw) -> {
            BigDecimal weight = CellValues.percent(raw);
            if (weight == null || weight.signum() < 0 || weight.compareTo(HUNDRED) > 0) {
                errors.add(new ImportRowError(row.rowNumber(), ref(CLASSES, WEIGHT_COLUMNS.get(code)),
                        ImportMessages.INVALID_WEIGHT));
            } else {
                weights.put(code, weight);
            }
        });
        BigDecimal total = weights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(HUNDRED) > 0) {
            errors.add(new ImportRowError(row.rowNumber(), ref(CLASSES, "exams_weight"),
                    ImportMessages.weightsExceed(total)));
        }
        return Optional.of(new GradingInput(scale, passing, weights));
    }

    /**
     * Guarda la configuración de notas de la clase. Las celdas vacías conservan lo que ya estuviera configurado (o la
     * escala 0-5 si la clase aún no tiene configuración). Devuelve false si la fila tuvo un error.
     */
    boolean applyGrading(Long teacherId, SpreadsheetRow row, Long teachingPeriodId, GradingInput input,
                         List<ImportRowError> errors) {
        GradingConfiguration existing = configurations.findByTeachingPeriodId(teachingPeriodId).orElse(null);
        GradingScale scale = input.scale() != null ? input.scale()
                : existing != null ? scales.findById(existing.getGradingScaleId()).orElse(null)
                : defaultScale(teacherId);
        if (scale == null) {
            errors.add(new ImportRowError(row.rowNumber(), ref(CLASSES, "grading_scale"), ImportMessages.REQUIRED));
            return false;
        }
        Map<Long, BigDecimal> weights = input.hasWeights() ? weightsByCategoryId(input.weights())
                : existing == null ? Map.of() : positiveWeights(existing);
        BigDecimal passing = input.passingGrade() != null ? input.passingGrade()
                : existing == null ? null : existing.getPassingGrade();
        if (passing != null && !scale.contains(passing)) {
            errors.add(new ImportRowError(row.rowNumber(), ref(CLASSES, "passing_grade"),
                    ImportMessages.outsideScale(scale)));
            return false;
        }
        if (existing != null && existing.getGradingScaleId().equals(scale.getId())
                && sameDecimal(existing.getPassingGrade(), passing) && sameWeights(positiveWeights(existing), weights)) {
            return true;
        }
        List<GradingCommands.WeightInput> inputs = new ArrayList<>();
        weights.forEach((categoryId, weight) -> inputs.add(new GradingCommands.WeightInput(categoryId, weight)));
        try {
            configureGrading.save(new GradingCommands.SaveConfiguration(teacherId, teachingPeriodId, scale.getId(), inputs,
                    passing));
            return true;
        } catch (DomainException e) {
            String message = "GRADING_SCALE_LOCKED".equals(e.getCode()) ? ImportMessages.SCALE_LOCKED
                    : ImportMessages.couldNotSave(e.getMessage());
            errors.add(new ImportRowError(row.rowNumber(), ref(CLASSES, "grading_scale"), message));
            return false;
        }
    }

    /** Por nombre (sin tildes ni mayúsculas) o por rango ("0-5", "0 a 5"); ante empate, primero las del sistema. */
    Optional<GradingScale> resolveScale(Long teacherId, String raw) {
        List<GradingScale> visible = scales.findVisibleTo(teacherId);
        String normalized = SpreadsheetVocabulary.normalize(raw);
        for (GradingScale scale : visible) {
            if (SpreadsheetVocabulary.normalize(scale.getName()).equals(normalized)
                    || SpreadsheetVocabulary.normalize(scale.displayName()).equals(normalized)) {
                return Optional.of(scale);
            }
        }
        Matcher m = RANGE.matcher(raw.trim());
        if (!m.matches()) {
            return Optional.empty();
        }
        BigDecimal min = CellValues.decimal(m.group(1));
        BigDecimal max = CellValues.decimal(m.group(2));
        return visible.stream()
                .filter(s -> s.getMinimumValue().compareTo(min) == 0 && s.getMaximumValue().compareTo(max) == 0)
                .min((a, b) -> Boolean.compare(a.getTeacherId() != null, b.getTeacherId() != null));
    }

    private GradingScale defaultScale(Long teacherId) {
        return resolveScale(teacherId, "0-5").orElseGet(() -> scales.findVisibleTo(teacherId).stream()
                .filter(s -> s.getTeacherId() == null).findFirst().orElse(null));
    }

    private Map<Long, BigDecimal> weightsByCategoryId(Map<EvaluationCategoryCode, BigDecimal> weights) {
        Map<Long, BigDecimal> byId = new HashMap<>();
        weights.forEach((code, weight) -> {
            if (weight.signum() > 0) {
                byId.put(categoryId(code), weight);
            }
        });
        return byId;
    }

    private Long categoryId(EvaluationCategoryCode code) {
        return categories.findByName(code.name()).map(EvaluationCategory::getId)
                .orElseThrow(() -> ResourceNotFoundException.of("EvaluationCategory", code.name()));
    }

    private static Map<Long, BigDecimal> positiveWeights(GradingConfiguration configuration) {
        Map<Long, BigDecimal> byId = new HashMap<>();
        for (GradingWeight w : configuration.getWeights()) {
            if (w.getWeight() != null && w.getWeight().signum() > 0) {
                byId.put(w.getEvaluationCategoryId(), w.getWeight());
            }
        }
        return byId;
    }

    private static boolean sameWeights(Map<Long, BigDecimal> a, Map<Long, BigDecimal> b) {
        return a.keySet().equals(b.keySet()) && a.entrySet().stream()
                .allMatch(e -> e.getValue().compareTo(b.get(e.getKey())) == 0);
    }

    private static boolean sameDecimal(BigDecimal a, BigDecimal b) {
        return a == null || b == null ? Objects.equals(a, b) : a.compareTo(b) == 0;
    }

    // ------------------------------------------------------------------ horarios (hoja Horarios)

    /** Lee día, horas y salón; null si alguna celda es inválida (los errores quedan en {@code errors}). */
    ScheduleInput readSchedule(SpreadsheetRow row, List<ImportRowError> errors) {
        int before = errors.size();
        DayOfWeek day = null;
        String rawDay = row.get("day_of_week");
        if (rawDay == null) {
            errors.add(new ImportRowError(row.rowNumber(), ref(SCHEDULES, "day_of_week"), ImportMessages.REQUIRED));
        } else {
            day = SpreadsheetVocabulary.dayOfWeek(rawDay).orElse(null);
            if (day == null) {
                errors.add(new ImportRowError(row.rowNumber(), ref(SCHEDULES, "day_of_week"), ImportMessages.INVALID_DAY));
            }
        }
        LocalTime start = readTime(row, "start_time", errors);
        LocalTime end = readTime(row, "end_time", errors);
        if (start != null && end != null && !end.isAfter(start)) {
            errors.add(new ImportRowError(row.rowNumber(), ref(SCHEDULES, "end_time"), ImportMessages.END_BEFORE_START));
        }
        String room = row.get("room");
        if (room != null && room.length() > 50) {
            errors.add(new ImportRowError(row.rowNumber(), ref(SCHEDULES, "room"), ImportMessages.maxLength(50)));
        }
        return errors.size() > before ? null : new ScheduleInput(day, start, end, room);
    }

    /**
     * Crea el bloque si no existe. Un bloque con el mismo día y horas ya existente no se duplica (sólo se actualiza el
     * salón si cambió). Devuelve false si la fila tuvo un error.
     */
    boolean applySchedule(Long teacherId, SpreadsheetRow row, Long teachingPeriodId, ScheduleInput input,
                          List<ImportRowError> errors) {
        Optional<ScheduledClassView> same = schedules.findViewsByTeachingPeriodId(teachingPeriodId).stream()
                .filter(s -> s.dayOfWeek() == input.day() && s.startTime().equals(input.start())
                        && s.endTime().equals(input.end()))
                .findFirst();
        try {
            if (same.isPresent()) {
                if (input.room() != null && !input.room().equals(same.get().room())) {
                    manageSchedule.update(new AcademicCommands.SaveSchedule(teacherId, teachingPeriodId,
                            same.get().scheduleId(), input.day(), input.start(), input.end(), input.room()));
                }
                return true;
            }
            TeachingPeriodView period = teachingPeriods.findViewById(teachingPeriodId)
                    .orElseThrow(() -> ResourceNotFoundException.of("TeachingPeriod", teachingPeriodId));
            Optional<ScheduledClassView> conflict = schedules.findConflict(teacherId, input.day(), input.start(),
                    input.end(), period.startDate(), period.endDate(), null);
            if (conflict.isPresent()) {
                ScheduledClassView other = conflict.get();
                errors.add(new ImportRowError(row.rowNumber(), ref(SCHEDULES, "start_time"), ImportMessages.scheduleConflict(
                        other.subjectName() + " " + other.gradeName() + " " + other.groupName(),
                        SpreadsheetVocabulary.dayLabel(other.dayOfWeek()), other.startTime().format(HH_MM),
                        other.endTime().format(HH_MM))));
                return false;
            }
            manageSchedule.create(new AcademicCommands.SaveSchedule(teacherId, teachingPeriodId, null, input.day(),
                    input.start(), input.end(), input.room()));
            return true;
        } catch (DomainException e) {
            errors.add(new ImportRowError(row.rowNumber(), SchoolSetupSheets.spec(SCHEDULES).label(),
                    ImportMessages.couldNotCreate(e.getMessage())));
            return false;
        }
    }

    private static LocalTime readTime(SpreadsheetRow row, String column, List<ImportRowError> errors) {
        String raw = row.get(column);
        if (raw == null) {
            errors.add(new ImportRowError(row.rowNumber(), ref(SCHEDULES, column), ImportMessages.REQUIRED));
            return null;
        }
        LocalTime time = CellValues.time(raw);
        if (time == null) {
            errors.add(new ImportRowError(row.rowNumber(), ref(SCHEDULES, column), ImportMessages.INVALID_TIME));
        }
        return time;
    }

    private static String ref(String sheetName, String column) {
        return SchoolSetupSheets.spec(sheetName).ref(column);
    }
}
