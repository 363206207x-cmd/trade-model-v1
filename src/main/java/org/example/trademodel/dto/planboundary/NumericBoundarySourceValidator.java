package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

public final class NumericBoundarySourceValidator {

    private NumericBoundarySourceValidator() {
    }

    public static List<String> validate(
            BoundaryEntryDTO entry,
            BoundaryStopDTO stop,
            List<BoundaryTakeProfitLevelDTO> takeProfitLevels,
            BoundarySourceFieldsDTO sourceFields
    ) {
        List<String> missing = new ArrayList<>();
        validateEntry(entry, missing);
        validateStop(stop, missing);
        validateTakeProfit(takeProfitLevels, missing);
        validateSourceFields(sourceFields, missing);
        return missing;
    }

    private static void validateEntry(BoundaryEntryDTO entry, List<String> missing) {
        if (entry == null) {
            missing.add("entry missing");
            return;
        }
        addWhenNull(entry.getEntryPrice(), "entry price missing", missing);
        addWhenBlank(entry.getNumericSourceType(), "entry numeric source type missing", missing);
        addWhenNull(entry.getNumericSourceValue(), "entry numeric source value missing", missing);
        addWhenBlank(entry.getSourceTimeframe(), "entry source timeframe missing", missing);
        addWhenBlank(entry.getReason(), "entry source reason missing", missing);
    }

    private static void validateStop(BoundaryStopDTO stop, List<String> missing) {
        if (stop == null) {
            missing.add("stop missing");
            return;
        }
        addWhenNull(stop.getStopPrice(), "stop price missing", missing);
        addWhenBlank(stop.getNumericSourceType(), "stop numeric source type missing", missing);
        addWhenNull(stop.getNumericSourceValue(), "stop numeric source value missing", missing);
        addWhenBlank(stop.getSourceTimeframe(), "stop source timeframe missing", missing);
        addWhenBlank(stop.getReason(), "stop source reason missing", missing);
    }

    private static void validateTakeProfit(List<BoundaryTakeProfitLevelDTO> takeProfitLevels, List<String> missing) {
        if (takeProfitLevels == null || takeProfitLevels.isEmpty()) {
            missing.add("takeProfitLevels missing");
            return;
        }
        for (int i = 0; i < takeProfitLevels.size(); i++) {
            BoundaryTakeProfitLevelDTO takeProfit = takeProfitLevels.get(i);
            if (takeProfit == null) {
                missing.add("TP level " + i + " missing");
                continue;
            }
            addWhenNull(takeProfit.getPrice(), "TP price missing", missing);
            addWhenNull(takeProfit.getRr(), "TP RR missing", missing);
            addWhenBlank(takeProfit.getNumericSourceType(), "TP numeric source type missing", missing);
            addWhenNull(takeProfit.getNumericSourceValue(), "TP numeric source value missing", missing);
            addWhenBlank(takeProfit.getSourceTimeframe(), "TP source timeframe missing", missing);
            addWhenBlank(takeProfit.getSourceRef(), "TP source ref missing", missing);
            addWhenBlank(takeProfit.getReason(), "TP source reason missing", missing);
        }
    }

    private static void validateSourceFields(BoundarySourceFieldsDTO sourceFields, List<String> missing) {
        if (sourceFields == null) {
            missing.add("sourceFields missing");
            return;
        }
        addWhenBlank(sourceFields.getEntrySourceField(), "entry source field missing", missing);
        addWhenBlank(sourceFields.getStopSourceField(), "stop source field missing", missing);
        addWhenBlank(sourceFields.getTakeProfitSourceField(), "TP source field missing", missing);
        addWhenBlank(sourceFields.getRrRule(), "RR rule field missing", missing);
        addWhenBlank(sourceFields.getDataSource(), "data source missing", missing);
    }

    private static void addWhenNull(Object value, String reason, List<String> missing) {
        if (value == null) {
            missing.add(reason);
        }
    }

    private static void addWhenBlank(String value, String reason, List<String> missing) {
        if (value == null || value.trim().isEmpty()) {
            missing.add(reason);
        }
    }
}
