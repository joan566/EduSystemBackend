package com.edusistem.core.imports.application.classsetup;

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
import com.edusistem.core.imports.application.contracts.ClassGradingConfigurer;
import com.edusistem.core.imports.application.support.CellValues;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.shared.domain.exceptions.DomainException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary;
import java.math.BigDecimal;
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
 * Configuración de notas de una clase que llega en el Excel de configuración escolar (columnas opcionales de la hoja
 * Clases). Valida antes de guardar para devolver mensajes en español por celda; lo que queda lo valida el caso de uso
 * de siempre.
 */
public class GradingSetupApplier implements ClassGradingConfigurer {

    private static final BigDecimal HUNDRED = GradingConfiguration.HUNDRED;
    /** "0-5", "0 - 10", "0 a 100", "1 al 10", "0 hasta 5". */
    private static final Pattern RANGE = Pattern.compile(
            "^(\\d+(?:[.,]\\d+)?)\\s*(?:-|–|a|al|hasta)\\s*(\\d+(?:[.,]\\d+)?)$", Pattern.CASE_INSENSITIVE);
    private static final Map<EvaluationCategoryCode, String> WEIGHT_COLUMNS = Map.of(
            EvaluationCategoryCode.EXAMS, "exams_weight",
            EvaluationCategoryCode.ACTIVITIES, "activities_weight",
            EvaluationCategoryCode.ATTENDANCE, "attendance_weight");

    private final ConfigureGradingUseCase configureGrading;
    private final GradingConfigurationRepositoryPort configurations;
    private final GradingScaleRepositoryPort scales;
    private final EvaluationCategoryRepositoryPort categories;

    public GradingSetupApplier(ConfigureGradingUseCase configureGrading, GradingConfigurationRepositoryPort configurations,
                               GradingScaleRepositoryPort scales, EvaluationCategoryRepositoryPort categories) {
        this.configureGrading = configureGrading;
        this.configurations = configurations;
        this.scales = scales;
        this.categories = categories;
    }

    @Override
    public List<String> scaleOptions(Long teacherId) {
        return scales.findVisibleTo(teacherId).stream().map(GradingScale::displayName).distinct().toList();
    }

    @Override
    public Optional<GradingInput> read(Long teacherId, RowReader row) {
        String rawScale = row.raw("grading_scale");
        String rawPassing = row.raw("passing_grade");
        Map<EvaluationCategoryCode, String> rawWeights = new EnumMap<>(EvaluationCategoryCode.class);
        WEIGHT_COLUMNS.forEach((code, column) -> {
            String raw = row.raw(column);
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
                row.error("grading_scale", ImportMessages.unknownScale(scaleOptions(teacherId)));
            }
        }
        BigDecimal passing = null;
        if (rawPassing != null) {
            passing = CellValues.decimal(rawPassing);
            if (passing == null) {
                row.error("passing_grade", ImportMessages.NOT_A_NUMBER);
            }
        }
        Map<EvaluationCategoryCode, BigDecimal> weights = new EnumMap<>(EvaluationCategoryCode.class);
        rawWeights.forEach((code, raw) -> {
            BigDecimal weight = CellValues.percent(raw);
            if (weight == null || weight.signum() < 0 || weight.compareTo(HUNDRED) > 0) {
                row.error(WEIGHT_COLUMNS.get(code), ImportMessages.INVALID_WEIGHT);
            } else {
                weights.put(code, weight);
            }
        });
        BigDecimal total = weights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(HUNDRED) > 0) {
            row.error("exams_weight", ImportMessages.weightsExceed(total));
        }
        return Optional.of(new GradingInput(scale, passing, weights));
    }

    /**
     * Las celdas vacías conservan lo que ya estuviera configurado (o la escala 0-5 si la clase aún no tiene
     * configuración).
     */
    @Override
    public boolean apply(Long teacherId, RowReader row, Long teachingPeriodId, GradingInput input) {
        GradingConfiguration existing = configurations.findByTeachingPeriodId(teachingPeriodId).orElse(null);
        GradingScale scale = input.scale() != null ? input.scale()
                : existing != null ? scales.findById(existing.getGradingScaleId()).orElse(null)
                : defaultScale(teacherId);
        if (scale == null) {
            row.error("grading_scale", ImportMessages.REQUIRED);
            return false;
        }
        Map<Long, BigDecimal> weights = input.hasWeights() ? weightsByCategoryId(input.weights())
                : existing == null ? Map.of() : positiveWeights(existing);
        BigDecimal passing = input.passingGrade() != null ? input.passingGrade()
                : existing == null ? null : existing.getPassingGrade();
        if (passing != null && !scale.contains(passing)) {
            row.error("passing_grade", ImportMessages.outsideScale(scale));
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
            row.error("grading_scale", message);
            return false;
        }
    }

    /** Por nombre (sin tildes ni mayúsculas) o por rango ("0-5", "0 a 5"); ante empate, primero las del sistema. */
    private Optional<GradingScale> resolveScale(Long teacherId, String raw) {
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
}
