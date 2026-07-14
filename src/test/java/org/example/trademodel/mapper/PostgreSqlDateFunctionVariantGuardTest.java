package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.MonitorAlertDO;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.vo.KeyCountVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class PostgreSqlDateFunctionVariantGuardTest {

    @Test
    void analysisRunBaselineUsesExplicitBoundedUtcRange() throws Exception {
        assertBoundedUtcRange(AnalysisRunMapper.class.getMethod(
                "countInWindow", LocalDateTime.class, LocalDateTime.class), "analysis_time");
        assertBoundedUtcRange(AnalysisRunMapper.class.getMethod(
                "countLowQualityInWindow", LocalDateTime.class, LocalDateTime.class, int.class), "analysis_time");
    }

    @Test
    void pushSnapshotMapperKeepsH2DateaddFallbackAndAddsPostgreSqlIntervalVariant() throws Exception {
        Method method = PushSnapshotMapper.class.getMethod("listPendingRecheckNext",
                String.class, String.class, String.class, int.class, int.class, LocalDateTime.class, int.class);
        assertThat(method.getReturnType()).isAssignableFrom(List.class);
        assertDateaddVariant(method);
    }

    @Test
    void hotResetEventBaselineUsesExplicitBoundedUtcRange() throws Exception {
        assertBoundedUtcRange(HotResetEventMapper.class.getMethod(
                "countInWindow", LocalDateTime.class, LocalDateTime.class), "event_time");
        Method grouped = HotResetEventMapper.class.getMethod(
                "selectTriggerTypeCountsInWindow", LocalDateTime.class, LocalDateTime.class);
        assertThat(grouped.getGenericReturnType().getTypeName()).contains(KeyCountVO.class.getSimpleName());
        assertBoundedUtcRange(grouped, "event_time");
    }

    @Test
    void decisionCountUsesBoundedRangeSql() throws Exception {
        Method method = DecisionResultMapper.class.getMethod(
                "countDecisionsInRange", LocalDateTime.class, LocalDateTime.class);
        String boundedSql = sql(genericSelect(method));

        assertThat(boundedSql)
                .contains("create_time >= #{startInclusive}")
                .contains("create_time < #{endExclusive}");
    }

    @Test
    void decisionCountDoesNotDependOnDatabaseCurrentDate() throws Exception {
        String boundedSql = sql(genericSelect(DecisionResultMapper.class.getMethod(
                "countDecisionsInRange", LocalDateTime.class, LocalDateTime.class)))
                .toUpperCase(Locale.ROOT);

        assertThat(boundedSql)
                .doesNotContain("CURRENT_DATE")
                .doesNotContain("CURRENT_TIMESTAMP")
                .doesNotContain("CAST(CREATE_TIME AS DATE)");
    }

    @Test
    void recheckWindowUsesBoundedRangeSql() throws Exception {
        Method method = PushRecheckLogMapper.class.getMethod(
                "countByStatusInWindow", String.class, LocalDateTime.class, LocalDateTime.class);
        String boundedSql = sql(genericSelect(method));

        assertThat(boundedSql)
                .contains("create_time >= #{windowStartInclusive}")
                .contains("create_time <= #{asOfInclusive}");
        assertThat(method.getAnnotationsByType(Select.class)).hasSize(1);
    }

    @Test
    void utcNaiveQueriesDoNotUseDatabaseCurrentTime() throws Exception {
        String decisionSql = sql(genericSelect(DecisionResultMapper.class.getMethod(
                "countDecisionsInRange", LocalDateTime.class, LocalDateTime.class)));
        String recheckSql = sql(genericSelect(PushRecheckLogMapper.class.getMethod(
                "countByStatusInWindow", String.class, LocalDateTime.class, LocalDateTime.class)));
        String alertSql = sql(genericSelect(MonitorAlertMapper.class.getMethod(
                "countByStatusInWindow", String.class, LocalDateTime.class, LocalDateTime.class)));
        String alertTypeSql = sql(genericSelect(MonitorAlertMapper.class.getMethod(
                "countByStatusAndTypeInWindow", String.class, String.class,
                LocalDateTime.class, LocalDateTime.class)));
        String alertThrottleSql = sql(genericSelect(MonitorAlertMapper.class.getMethod(
                "countOpenInThrottleWindow", String.class, String.class,
                LocalDateTime.class, LocalDateTime.class)));
        String alertSemanticSql = sql(genericSelect(MonitorAlertMapper.class.getMethod(
                "countAnyInSemanticWindow", String.class, String.class,
                LocalDateTime.class, LocalDateTime.class)));
        String analysisSql = sql(genericSelect(AnalysisRunMapper.class.getMethod(
                "countInWindow", LocalDateTime.class, LocalDateTime.class)));
        String lowQualitySql = sql(genericSelect(AnalysisRunMapper.class.getMethod(
                "countLowQualityInWindow", LocalDateTime.class, LocalDateTime.class, int.class)));
        String hotResetSql = sql(genericSelect(HotResetEventMapper.class.getMethod(
                "countInWindow", LocalDateTime.class, LocalDateTime.class)));
        String hotResetTypeSql = sql(genericSelect(HotResetEventMapper.class.getMethod(
                "selectTriggerTypeCountsInWindow", LocalDateTime.class, LocalDateTime.class)));
        String utcNaiveSql = String.join(" ", decisionSql, recheckSql, alertSql, alertTypeSql,
                alertThrottleSql, alertSemanticSql, analysisSql, lowQualitySql,
                hotResetSql, hotResetTypeSql).toUpperCase(Locale.ROOT);

        assertThat(utcNaiveSql)
                .doesNotContain("CURRENT_DATE")
                .doesNotContain("CURRENT_TIMESTAMP")
                .doesNotContain("LOCALTIMESTAMP")
                .doesNotContain("DATEADD")
                .doesNotContain("INTERVAL '1 MINUTE'")
                .doesNotContain("CAST(CREATE_TIME AS DATE)");
    }

    @Test
    void monitorAlertWriteThrottleUsesExplicitBoundedUtcRange() throws Exception {
        assertBoundedUtcRange(MonitorAlertMapper.class.getMethod(
                "countOpenInThrottleWindow", String.class, String.class,
                LocalDateTime.class, LocalDateTime.class), "created_at");
        assertBoundedUtcRange(MonitorAlertMapper.class.getMethod(
                "countAnyInSemanticWindow", String.class, String.class,
                LocalDateTime.class, LocalDateTime.class), "created_at");
    }

    @Test
    void databaseDefaultTimeIsNotUsedForNewMonitorAlerts() throws Exception {
        Insert insert = MonitorAlertMapper.class.getMethod("insert", MonitorAlertDO.class)
                .getAnnotation(Insert.class);
        String insertSql = String.join(" ", insert.value());
        String upperSql = insertSql.toUpperCase(Locale.ROOT);

        assertThat(insertSql)
                .contains("created_at")
                .contains("updated_at")
                .contains("#{createdAtUtc}")
                .contains("#{updatedAtUtc}")
                .contains("#{cooldownUntilUtc}");
        assertThat(upperSql)
                .doesNotContain("CURRENT_TIMESTAMP")
                .doesNotContain("CURRENT_DATE")
                .doesNotContain("DATEADD")
                .doesNotContain("INTERVAL '1 MINUTE'");
    }

    @Test
    void monitorAlertBaselineUsesExplicitBoundedUtcRange() throws Exception {
        assertBoundedUtcRange(MonitorAlertMapper.class.getMethod(
                "countByStatusInWindow", String.class, LocalDateTime.class, LocalDateTime.class), "created_at");
        assertBoundedUtcRange(MonitorAlertMapper.class.getMethod(
                "countByStatusAndTypeInWindow", String.class, String.class,
                LocalDateTime.class, LocalDateTime.class), "created_at");
    }

    @Test
    void monitorAlertMapperKeepsH2FormatFallbackAndAddsPostgreSqlToCharVariants() throws Exception {
        assertFormatVariant(MonitorAlertMapper.class.getMethod("selectRecent", int.class));
        assertFormatVariant(MonitorAlertMapper.class.getMethod("listByAnalysisId", String.class));
    }

    @Test
    void userPositionMapperGeneratedKeyAnnotationSpecifiesKeyColumn() throws Exception {
        Options options = UserPositionMapper.class.getMethod("insert", UserPositionDO.class).getAnnotation(Options.class);

        assertThat(options.useGeneratedKeys()).isTrue();
        assertThat(options.keyProperty()).isEqualTo("id");
        assertThat(options.keyColumn()).isEqualTo("id");
    }

    @SuppressWarnings("unused")
    private static void keepPushSnapshotTypeVisible(TmPushSnapshotDO ignored) {
    }

    private static void assertDateaddVariant(Method method) {
        Select generic = genericSelect(method);
        Select postgres = postgresSelect(method);

        assertThat(sql(generic)).contains("DATEADD");
        assertThat(sql(postgres))
                .contains("INTERVAL '1 minute'")
                .doesNotContain("DATEADD")
                .doesNotContain("FORMATDATETIME");
    }

    private static void assertFormatVariant(Method method) {
        Select generic = genericSelect(method);
        Select postgres = postgresSelect(method);

        assertThat(sql(generic)).contains("FORMATDATETIME");
        assertThat(sql(postgres))
                .contains("TO_CHAR")
                .contains("YYYY-MM-DD HH24:MI:SS")
                .doesNotContain("FORMATDATETIME")
                .doesNotContain("DATEADD");
    }

    private static void assertBoundedUtcRange(Method method, String timestampColumn) {
        Select select = genericSelect(method);
        String boundedSql = sql(select);
        String upperSql = boundedSql.toUpperCase(Locale.ROOT);

        assertThat(method.getAnnotationsByType(Select.class)).hasSize(1);
        assertThat(boundedSql)
                .contains(timestampColumn + " >= #{windowStartInclusive}")
                .contains(timestampColumn + " <= #{asOfInclusive}");
        assertThat(upperSql)
                .doesNotContain("CURRENT_TIMESTAMP")
                .doesNotContain("CURRENT_DATE")
                .doesNotContain("DATEADD")
                .doesNotContain("INTERVAL '1 MINUTE'");
    }

    private static Select genericSelect(Method method) {
        return Arrays.stream(method.getAnnotationsByType(Select.class))
                .filter(annotation -> annotation.databaseId().isBlank())
                .findFirst()
                .orElseThrow();
    }

    private static Select postgresSelect(Method method) {
        return Arrays.stream(method.getAnnotationsByType(Select.class))
                .filter(annotation -> "postgresql".equals(annotation.databaseId()))
                .findFirst()
                .orElseThrow();
    }

    private static String sql(Select select) {
        return String.join(" ", select.value());
    }
}
