package org.example.trademodel.dto.planboundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.service.EntryCompletionValidationContextAssembler;
import org.example.trademodel.service.SourceTraceEntryCompletionContract;
import org.example.trademodel.service.SourceTraceEntryOwnershipAdapter;
import org.example.trademodel.service.SourceTraceEntryOwnershipValidator;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

class SourceTraceEntryPositiveCompletionFixtureFactoryMapperTest {

    private final SourceTraceEntryPositiveCompletionFixtureFactory factory =
            new SourceTraceEntryPositiveCompletionFixtureFactory();
    private final SourceTraceEntryPositiveCompletionFixtureMapper mapper =
            new SourceTraceEntryPositiveCompletionFixtureMapper();

    @Test
    void factoryStartsFromDtoFailClosedDefaults() {
        SourceTraceEntryPositiveCompletionContractDTO dto = factory.defaultFixture();

        assertThat(dto.getCompletionStatus())
                .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
        assertThat(dto.getCompletionTransition())
                .isEqualTo(SourceTraceEntryPositiveCompletionTransitionEnum.NONE);
        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.DEFAULT_FAIL_CLOSED);
        assertStillNonProduction(dto);
        assertThat(dto.getMissingFields()).contains("sourceTraceEntryOwnershipCompletionPath");
    }

    @Test
    void mapperAcceptsSyntheticFixtureValuesButDoesNotCreateRuntimeReadiness() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                mapper.fromFixture(SourceTraceEntryPositiveCompletionFixtureInput.syntheticFixture());

        assertThat(dto.getCompletionStatus())
                .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.POSITIVE_FIXTURE_READY);
        assertThat(dto.getCompletionTransition())
                .isEqualTo(SourceTraceEntryPositiveCompletionTransitionEnum.INCOMPLETE_TO_POSITIVE_FIXTURE_READY);
        assertThat(dto.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(dto.getTimeframe()).isEqualTo("15m");
        assertThat(dto.getSourceTraceEntryOwnershipCompletionPath())
                .isEqualTo("fixture-only-completion-path");
        assertThat(dto.getEntryPriceSource()).isEqualByComparingTo("1.00");
        assertThat(dto.getEntrySourceType()).isEqualTo("rule-owned-boundary");
        assertThat(dto.getEntrySourceTimeframe()).isEqualTo("15m");
        assertThat(dto.getEntrySourceReason()).isEqualTo("fixture-only-source-reason");
        assertThat(dto.getEntrySourceRef()).isEqualTo("fixture-source-ref");
        assertThat(dto.getRuleId()).isEqualTo("fixture-entry-rule");
        assertThat(dto.getRuleVersion()).isEqualTo("fixture-v1");
        assertThat(dto.getSourceWindow()).isEqualTo("fixture-window");
        assertThat(dto.getFreshnessStatus()).isEqualTo("FRESH");
        assertThat(dto.getObservedAtMs()).isEqualTo(100L);
        assertThat(dto.getDecisionCreateTimeMs()).isEqualTo(200L);
        assertThat(dto.getConflictsWithLiquidity()).isFalse();
        assertThat(dto.getConflictsWithEvent()).isFalse();
        assertThat(dto.getConflictsWithWick()).isFalse();
        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.FIXTURE_ONLY_NOT_PRODUCTION_READY);
        assertStillNonProduction(dto);
    }

    @Test
    void factoryMapsSyntheticFixtureValuesButPreservesNonProductionSafety() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                factory.syntheticFixture(SourceTraceEntryPositiveCompletionFixtureInput.syntheticFixture());

        assertThat(dto.getCompletionStatus())
                .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.POSITIVE_FIXTURE_READY);
        assertThat(dto.getMissingFields()).containsExactly("fixture-only-not-runtime-ready");
        assertStillNonProduction(dto);
    }

    @Test
    void nullFixtureInputDowngradesToFailClosed() {
        SourceTraceEntryPositiveCompletionContractDTO dto = mapper.fromFixture(null);

        assertThat(dto.getCompletionStatus())
                .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD);
        assertThat(dto.getMissingFields()).containsExactly("fixtureInput");
        assertStillNonProduction(dto);
    }

    @Test
    void runtimeLikeSourceTagsDowngradeToFailClosed() {
        List<String> sourceTags = List.of(
                "LATEST_PRICE_ONLY",
                "RAW_KLINE_ONLY",
                "AI_TEXT",
                "DASHBOARD_TEXT",
                "EXTERNAL_DATA",
                "ORDER_DATA",
                "EXECUTION_DATA"
        );

        SourceTraceEntryPositiveCompletionContractDTO dto =
                mapper.fromFixture(SourceTraceEntryPositiveCompletionFixtureInput.withSourceTags(sourceTags));

        assertThat(dto.getCompletionStatus())
                .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION);
        assertThat(dto.getMissingFields()).containsExactlyElementsOf(sourceTags);
        assertThat(dto.getEntryPriceSource()).isNull();
        assertThat(dto.getSourceTraceEntryOwnershipCompletionPath()).isNull();
        assertStillNonProduction(dto);
    }

    @Test
    void emptySourceTagsStaySyntheticAndNonProduction() {
        SourceTraceEntryPositiveCompletionFixtureInput input =
                SourceTraceEntryPositiveCompletionFixtureInput.withSourceTags(List.of());

        SourceTraceEntryPositiveCompletionContractDTO dto = mapper.fromFixture(input);

        assertThat(dto.getCompletionStatus())
                .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.POSITIVE_FIXTURE_READY);
        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.FIXTURE_ONLY_NOT_PRODUCTION_READY);
        assertThat(dto.getSourceTraceEntryOwnershipCompletionPath())
                .isEqualTo("fixture-only-completion-path");
        assertStillNonProduction(dto);
    }

    @Test
    void runtimeLikeSourceTagsDowngradeOneAtATime() {
        List<String> runtimeLikeTags = List.of(
                "LATEST_PRICE_ONLY",
                "RAW_KLINE_ONLY",
                "AI_TEXT",
                "DASHBOARD_TEXT",
                "EXTERNAL_DATA",
                "ORDER_DATA",
                "EXECUTION_DATA"
        );

        for (String runtimeLikeTag : runtimeLikeTags) {
            SourceTraceEntryPositiveCompletionContractDTO dto =
                    mapper.fromFixture(SourceTraceEntryPositiveCompletionFixtureInput.withSourceTags(
                            List.of(runtimeLikeTag)
                    ));

            assertThat(dto.getCompletionStatus())
                    .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
            assertThat(dto.getDowngradeReason())
                    .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION);
            assertThat(dto.getMissingFields()).containsExactly(runtimeLikeTag);
            assertThat(dto.getEntryPriceSource()).isNull();
            assertStillNonProduction(dto);
        }
    }

    @Test
    void mixedSafeAndUnsafeSourceTagsDowngrade() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                mapper.fromFixture(SourceTraceEntryPositiveCompletionFixtureInput.withSourceTags(List.of(
                        "FIXTURE_SYNTHETIC_METADATA",
                        "LATEST_PRICE_ONLY"
                )));

        assertThat(dto.getCompletionStatus())
                .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION);
        assertThat(dto.getMissingFields()).containsExactly(
                "FIXTURE_SYNTHETIC_METADATA",
                "LATEST_PRICE_ONLY"
        );
        assertStillNonProduction(dto);
    }

    @Test
    void mutableInputEvidenceIsDefensivelyCopied() {
        List<String> mutableMissingFields = new ArrayList<>();
        mutableMissingFields.add("fixture-only-not-runtime-ready");
        SourceTraceEntryPositiveCompletionFixtureInput input =
                SourceTraceEntryPositiveCompletionFixtureInput.builder()
                        .missingFields(mutableMissingFields)
                        .build();
        mutableMissingFields.add("mutated-after-input-build");

        SourceTraceEntryPositiveCompletionContractDTO dto = mapper.fromFixture(input);
        mutableMissingFields.add("mutated-after-map");

        assertThat(input.getMissingFields()).containsExactly("fixture-only-not-runtime-ready");
        assertThat(dto.getMissingFields()).containsExactly("fixture-only-not-runtime-ready");
        assertStillNonProduction(dto);
    }

    @Test
    void sourceTagsFromFixtureInputAreDefensivelyCopied() {
        List<String> mutableSourceTags = new ArrayList<>();
        mutableSourceTags.add("LATEST_PRICE_ONLY");
        SourceTraceEntryPositiveCompletionFixtureInput input =
                SourceTraceEntryPositiveCompletionFixtureInput.builder()
                        .sourceTags(mutableSourceTags)
                        .build();
        mutableSourceTags.add("RAW_KLINE_ONLY");

        SourceTraceEntryPositiveCompletionContractDTO dto = mapper.fromFixture(input);
        mutableSourceTags.add("AI_TEXT");

        assertThat(input.getSourceTags()).containsExactly("LATEST_PRICE_ONLY");
        assertThat(dto.getMissingFields()).containsExactly("LATEST_PRICE_ONLY");
        assertStillNonProduction(dto);
    }

    @Test
    void mutableOutputEvidenceIsDefensivelyCopied() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                mapper.fromFixture(SourceTraceEntryPositiveCompletionFixtureInput.syntheticFixture());

        List<String> returnedMissingFields = dto.getMissingFields();
        returnedMissingFields.add("mutated-outside-dto");

        assertThat(dto.getMissingFields()).containsExactly("fixture-only-not-runtime-ready");
        assertStillNonProduction(dto);
    }

    @Test
    void syntheticFixturePositiveMetadataDoesNotImplyRuntimeSourceTraceCompletion() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                factory.syntheticFixture(SourceTraceEntryPositiveCompletionFixtureInput.syntheticFixture());

        assertThat(dto.getCompletionStatus())
                .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.POSITIVE_FIXTURE_READY);
        assertThat(dto.isSourceTraceEntryCompleted()).isFalse();
        assertStillNonProduction(dto);
    }

    @Test
    void syntheticFixturePositiveMetadataDoesNotImplyBoundaryCandidateServiceValid() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                factory.syntheticFixture(SourceTraceEntryPositiveCompletionFixtureInput.syntheticFixture());

        assertThat(dto.getCompletionStatus())
                .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.POSITIVE_FIXTURE_READY);
        assertThat(dto.isCompletionReady()).isFalse();
        assertNoMethodNameFragments(
                SourceTraceEntryPositiveCompletionFixtureFactory.class,
                List.of("boundarycandidateservice", "valid")
        );
        assertNoMethodNameFragments(
                SourceTraceEntryPositiveCompletionFixtureMapper.class,
                List.of("boundarycandidateservice", "valid")
        );
        assertStillNonProduction(dto);
    }

    @Test
    void syntheticFixturePositiveMetadataDoesNotImplyExecutionPlanReadiness() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                factory.syntheticFixture(SourceTraceEntryPositiveCompletionFixtureInput.syntheticFixture());

        assertThat(dto.getCompletionTransition())
                .isEqualTo(SourceTraceEntryPositiveCompletionTransitionEnum.INCOMPLETE_TO_POSITIVE_FIXTURE_READY);
        assertThat(dto.isCompletionReady()).isFalse();
        assertNoMethodNameFragments(
                SourceTraceEntryPositiveCompletionFixtureFactory.class,
                List.of("executionplan", "readiness", "ready")
        );
        assertNoMethodNameFragments(
                SourceTraceEntryPositiveCompletionFixtureMapper.class,
                List.of("executionplan", "readiness", "ready")
        );
        assertStillNonProduction(dto);
    }

    @Test
    void factoryMapperExposeNoOrderExecutionCloseReverseAutoTradingOrTradeReadyMethodNames() {
        assertNoForbiddenMethodNames(SourceTraceEntryPositiveCompletionFixtureFactory.class);
        assertNoForbiddenMethodNames(SourceTraceEntryPositiveCompletionFixtureMapper.class);
    }

    @Test
    void factoryMapperHaveNoSpringAnnotationsAndNoProductionBoundaryInterfaces() {
        assertNoSpringAnnotations(SourceTraceEntryPositiveCompletionFixtureFactory.class);
        assertNoSpringAnnotations(SourceTraceEntryPositiveCompletionFixtureMapper.class);
        assertNoProductionBoundaryInterfaces(SourceTraceEntryPositiveCompletionFixtureFactory.class);
        assertNoProductionBoundaryInterfaces(SourceTraceEntryPositiveCompletionFixtureMapper.class);
    }

    @Test
    void productionAdapterAndProductionCompletionContractRemainAbsent() {
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryOwnershipAdapter"
                ))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryCompletionContract"
                ))
                .isInstanceOf(ClassNotFoundException.class);
    }

    private void assertStillNonProduction(SourceTraceEntryPositiveCompletionContractDTO dto) {
        assertThat(dto.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(dto.isManualReviewRequired()).isTrue();
        assertThat(dto.isNotTradeInstruction()).isTrue();
        assertThat(dto.isSourceTraceEntryCompleted()).isFalse();
        assertThat(dto.isCompletionReady()).isFalse();
    }

    private void assertNoSpringAnnotations(Class<?> type) {
        assertThat(type.getAnnotation(Service.class)).isNull();
        assertThat(type.getAnnotation(Component.class)).isNull();
    }

    private void assertNoProductionBoundaryInterfaces(Class<?> type) {
        assertThat(SourceTraceEntryCompletionContract.class.isAssignableFrom(type)).isFalse();
        assertThat(SourceTraceEntryOwnershipAdapter.class.isAssignableFrom(type)).isFalse();
        assertThat(SourceTraceEntryOwnershipValidator.class.isAssignableFrom(type)).isFalse();
        assertThat(EntryCompletionValidationContextAssembler.class.isAssignableFrom(type)).isFalse();
    }

    private void assertNoForbiddenMethodNames(Class<?> type) {
        assertThat(Arrays.stream(type.getDeclaredMethods())
                        .map(Method::getName)
                        .map(name -> name.toLowerCase(Locale.ROOT)))
                .allSatisfy(name -> {
                    assertThat(name).doesNotContain("order");
                    assertThat(name).doesNotContain("execution");
                    assertThat(name).doesNotContain("execute");
                    assertThat(name).doesNotContain("close");
                    assertThat(name).doesNotContain("reverse");
                    assertThat(name).doesNotContain("autotrading");
                    assertThat(name).doesNotContain("auto");
                    assertThat(name).doesNotContain("tradeready");
                });
    }

    private void assertNoMethodNameFragments(Class<?> type, List<String> forbiddenFragments) {
        assertThat(Arrays.stream(type.getDeclaredMethods())
                        .map(Method::getName)
                        .map(name -> name.toLowerCase(Locale.ROOT)))
                .allSatisfy(name -> assertThat(forbiddenFragments).noneMatch(name::contains));
    }
}
