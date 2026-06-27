package org.example.trademodel.mapper;

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.example.trademodel.entity.TmPushSnapshotDO;
import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.vo.KeyCountVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostgreSqlDateFunctionVariantGuardTest {

    @Test
    void analysisRunMapperKeepsH2DateaddFallbackAndAddsPostgreSqlIntervalVariants() throws Exception {
        assertDateaddVariant(AnalysisRunMapper.class.getMethod("countInWindow", int.class));
        assertDateaddVariant(AnalysisRunMapper.class.getMethod("countLowQualityInWindow", int.class, int.class));
    }

    @Test
    void pushSnapshotMapperKeepsH2DateaddFallbackAndAddsPostgreSqlIntervalVariant() throws Exception {
        Method method = PushSnapshotMapper.class.getMethod("listPendingRecheckNext",
                String.class, String.class, String.class, int.class, int.class, int.class);
        assertThat(method.getReturnType()).isAssignableFrom(List.class);
        assertDateaddVariant(method);
    }

    @Test
    void hotResetEventMapperKeepsH2DateaddFallbackAndAddsPostgreSqlIntervalVariants() throws Exception {
        assertDateaddVariant(HotResetEventMapper.class.getMethod("countInWindow", int.class));
        Method grouped = HotResetEventMapper.class.getMethod("selectTriggerTypeCountsInWindow", int.class);
        assertThat(grouped.getGenericReturnType().getTypeName()).contains(KeyCountVO.class.getSimpleName());
        assertDateaddVariant(grouped);
    }

    @Test
    void pushRecheckLogMapperKeepsH2DateaddFallbackAndAddsPostgreSqlIntervalVariant() throws Exception {
        assertDateaddVariant(PushRecheckLogMapper.class.getMethod("countByStatusInWindow", String.class, int.class));
    }

    @Test
    void monitorAlertMapperKeepsH2DateaddFallbackAndAddsPostgreSqlIntervalVariants() throws Exception {
        assertDateaddVariant(MonitorAlertMapper.class.getMethod("countOpenInThrottleWindow", String.class, String.class, int.class));
        assertDateaddVariant(MonitorAlertMapper.class.getMethod("countAnyInSemanticWindow", String.class, String.class, int.class));
        assertDateaddVariant(MonitorAlertMapper.class.getMethod("countByStatusInWindow", String.class, int.class));
        assertDateaddVariant(MonitorAlertMapper.class.getMethod("countByStatusAndTypeInWindow", String.class, String.class, int.class));
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
