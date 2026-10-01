package com.edusistem.core.exports.application.schoolsetup.sheets;

import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.evaluation.domain.entity.EvaluationCategory;
import com.edusistem.core.evaluation.domain.enums.EvaluationCategoryCode;
import com.edusistem.core.evaluation.domain.outputports.EvaluationCategoryRepositoryPort;
import com.edusistem.core.exports.application.schoolsetup.SchoolSetupExportContext;
import com.edusistem.core.grading.domain.entity.GradingConfiguration;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.grading.domain.entity.GradingWeight;
import com.edusistem.core.grading.domain.outputports.GradingConfigurationRepositoryPort;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.shared.domain.vo.TabularData;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Hoja Clases: cada clase con su configuración de notas (escala, nota mínima y pesos). */
public class ClassesSheetExporter extends SheetExporterBase {

    private final GradingConfigurationRepositoryPort configurations;
    private final EvaluationCategoryRepositoryPort categories;

    public ClassesSheetExporter(GradingConfigurationRepositoryPort configurations,
                                EvaluationCategoryRepositoryPort categories) {
        super(SchoolSetupSheets.CLASSES);
        this.configurations = configurations;
        this.categories = categories;
    }

    @Override
    public TabularData export(SchoolSetupExportContext context) {
        Map<Long, GradingScale> scaleById = context.visibleScales().stream()
                .collect(Collectors.toMap(GradingScale::getId, Function.identity()));
        Map<Long, EvaluationCategoryCode> codeByCategoryId = new HashMap<>();
        for (EvaluationCategory category : categories.findAll()) {
            Arrays.stream(EvaluationCategoryCode.values()).filter(c -> c.name().equals(category.getName())).findFirst()
                    .ifPresent(code -> codeByCategoryId.put(category.getId(), code));
        }
        List<List<Object>> rows = new ArrayList<>();
        for (TeachingPeriodView tp : context.classes()) {
            GradingConfiguration configuration = configurations.findByTeachingPeriodId(tp.id()).orElse(null);
            if (configuration == null) {
                rows.add(row(tp, null, null, null, null, null));
                continue;
            }
            Map<EvaluationCategoryCode, BigDecimal> weights = new HashMap<>();
            for (GradingWeight w : configuration.getWeights()) {
                EvaluationCategoryCode code = codeByCategoryId.get(w.getEvaluationCategoryId());
                if (code != null) {
                    weights.put(code, w.getWeight());
                }
            }
            GradingScale scale = scaleById.get(configuration.getGradingScaleId());
            rows.add(row(tp, scale == null ? null : scale.displayName(), configuration.getPassingGrade(),
                    weights.getOrDefault(EvaluationCategoryCode.EXAMS, BigDecimal.ZERO),
                    weights.getOrDefault(EvaluationCategoryCode.ACTIVITIES, BigDecimal.ZERO),
                    weights.getOrDefault(EvaluationCategoryCode.ATTENDANCE, BigDecimal.ZERO)));
        }
        return SchoolSetupSheets.table(SchoolSetupSheets.CLASSES, rows, context.scaleOptions());
    }
}
