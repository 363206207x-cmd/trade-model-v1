package org.example.trademodel.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipAuditEnvelope;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipReviewDowngradeReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipReviewRequest;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipReviewResult;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipReviewStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceReviewModeEnum;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

class FailClosedSourceTraceEntryProductionOwnershipReviewBoundaryEdgeRegressionTest {

    private static final List<String> OWNER_EVIDENCE_FIELDS = List.of(
            "sourceTraceEntryOwnershipCompletionPath",
            "entryPriceSource",
            "entrySourceType",
            "entrySourceTimeframe",
            "entrySourceReason",
            "entrySourceRef",
            "sourceWindow",
            "ruleId",
            "ruleVersion",
            "freshnessOwnership",
            "conflictFamilyOwnership"
    );
    private static final List<String> UNSAFE_SUBSTITUTION_TOKENS = List.of(
            "latestprice",
            "latest-price",
            "rawkline",
            "raw-kline",
            "klineitem",
            "ai text",
            "aitext",
            "dashboard",
            "external",
            "coinglass",
            "order",
            "execution"
    );
    private static final List<String> RISK_ACTION_GUARD_TOKENS = List.of(
            "highrisk",
            "high-risk",
            "wick",
            "pinbar",
            "pin-bar",
            "liquiditystress",
            "liquidity-stress",
            "stampede",
            "missingevent",
            "missing-event",
            "multitimeframe",
            "multi-timeframe"
    );
    private static final List<String> POSITIVE_LOOKING_TOKENS = List.of(
            "valid",
            "completed",
            "complete",
            "signal",
            "buy",
            "sell",
            "open",
            "ready"
    );
    private static final List<String> FORBIDDEN_SURFACE_TOKENS = List.of(
            "order",
            "execution",
            "close",
            "reverse",
            "autoTrading",
            "autoTrade",
            "tradeReady",
            "readyToTrade",
            "valid",
            "completed",
            "signal",
            "buy",
            "sell",
            "open"
    );
    private static final List<String> GENERATED_TRADING_VALUE_TOKENS = List.of(
            "generatedEntry",
            "generatedStop",
            "generatedTakeProfit",
            "generatedRiskReward",
            "stopValue",
            "takeProfitValue",
            "riskRewardValue"
    );
    private static final List<String> PRODUCTION_WIRING_BLOCKERS = List.of(
            "productionOwnershipReviewBoundaryUnwired",
            "productionWiringStillBlocked",
            "failClosedImplementationSkeleton"
    );

    private final FailClosedSourceTraceEntryProductionOwnershipReviewBoundary boundary =
            new FailClosedSourceTraceEntryProductionOwnershipReviewBoundary();

    @TestFactory
    Stream<DynamicTest> edgeOwnerEvidenceCasesRemainFailClosedAndBlockerPreserving() {
        return Stream.of(
                edgeCase(
                        "malformed owner evidence preserves blocker",
                        request -> request.setOwnerEvidenceFields(Arrays.asList("entryPriceSource", null, " ")),
                        List.of("malformedOwnerEvidence"),
                        List.of("malformedOwnerEvidence"),
                        List.of()
                ),
                edgeCase(
                        "unsupported owner field preserves field-specific blocker",
                        request -> request.setOwnerEvidenceFields(List.of(
                                "entryPriceSource",
                                "fixtureRuntimeOwner"
                        )),
                        List.of("unsupportedOwnerEvidenceField"),
                        List.of(
                                "unsupportedOwnerEvidenceField",
                                "unsupportedOwnerEvidenceField:fixtureRuntimeOwner"
                        ),
                        List.of()
                ),
                edgeCase(
                        "empty-but-present owner evidence is missing and malformed",
                        request -> request.setOwnerEvidenceFields(List.of(" ", "\t")),
                        List.of("malformedOwnerEvidence"),
                        List.of("ownerEvidenceMissing", "malformedOwnerEvidence"),
                        List.of()
                ),
                edgeCase(
                        "mixed safe and unsafe owner evidence fails closed",
                        request -> request.setOwnerEvidenceFields(List.of(
                                "entryPriceSource",
                                "dashboard",
                                "fixtureUnsupportedOwner"
                        )),
                        List.of("unsupportedOwnerEvidenceField", "runtimeLikeSubstitution"),
                        List.of(
                                "unsupportedOwnerEvidenceField:dashboard",
                                "unsupportedOwnerEvidenceField:fixtureUnsupportedOwner",
                                "runtimeSubstitutionToken:dashboard"
                        ),
                        List.of()
                ),
                edgeCase(
                        "missing required owner fields remain fail-closed",
                        request -> {
                            request.setEntryPriceSource(" ");
                            request.setRuleId(null);
                            request.setOwnerEvidenceFields(List.of());
                        },
                        List.of(),
                        List.of("ownerEvidenceMissing"),
                        List.of("entryPriceSource", "ruleId")
                ),
                edgeCase(
                        "downgrade and rollback edge evidence preserves blockers",
                        request -> {
                            SourceTraceEntryProductionOwnershipAuditEnvelope auditEnvelope =
                                    new SourceTraceEntryProductionOwnershipAuditEnvelope();
                            auditEnvelope.setDowngradeReason("fixture downgrade");
                            auditEnvelope.setRollbackReason("fixture rollback");
                            request.setAuditEnvelope(auditEnvelope);
                        },
                        List.of("downgradeRequired", "rollbackRequired"),
                        List.of("downgradeRequired", "rollbackRequired"),
                        List.of()
                ),
                edgeCase(
                        "close and reverse labels cannot imply action behavior",
                        request -> request.setEntrySourceReason("close reverse fixture-only labels"),
                        List.of(),
                        List.of(),
                        List.of()
                )
        ).map(fixtureCase -> DynamicTest.dynamicTest(fixtureCase.name(), () -> assertFixtureCase(fixtureCase)));
    }

    @TestFactory
    Stream<DynamicTest> eachUnsafeSubstitutionTokenIsPreservedAsBlockerEvidence() {
        return UNSAFE_SUBSTITUTION_TOKENS.stream()
                .map(token -> DynamicTest.dynamicTest(token, () -> {
                    SourceTraceEntryProductionOwnershipReviewRequest request = completeFixtureRequest();
                    request.setEntryPriceSource("fixture-token " + token + " fixture-token");

                    SourceTraceEntryProductionOwnershipReviewResult result = boundary.reviewEntryOwnership(request);

                    assertMandatoryFailClosedFixtureOutcome(result);
                    assertThat(result.getUnsafeFields()).contains("runtimeLikeSubstitution");
                    assertThat(result.getBlockingFields()).contains(
                            "runtimeLikeSubstitution",
                            "runtimeSubstitutionToken:" + token
                    );
                }));
    }

    @TestFactory
    Stream<DynamicTest> eachRiskActionGuardTokenIsPreservedAsReviewOnlyBlockerEvidence() {
        return RISK_ACTION_GUARD_TOKENS.stream()
                .map(token -> DynamicTest.dynamicTest(token, () -> {
                    SourceTraceEntryProductionOwnershipReviewRequest request = completeFixtureRequest();
                    request.setConflictFamilyOwnership("fixture-risk " + token + " fixture-risk");

                    SourceTraceEntryProductionOwnershipReviewResult result = boundary.reviewEntryOwnership(request);

                    assertMandatoryFailClosedFixtureOutcome(result);
                    assertThat(result.getUnsafeFields()).contains("riskActionGuardReviewRequired");
                    assertThat(result.getBlockingFields()).contains(
                            "riskActionGuardReviewRequired",
                            "riskActionGuardToken:" + token
                    );
                }));
    }

    @TestFactory
    Stream<DynamicTest> eachPositiveLookingLabelTokenIsPreservedAsReviewOnlyBlockerEvidence() {
        return POSITIVE_LOOKING_TOKENS.stream()
                .map(token -> DynamicTest.dynamicTest(token, () -> {
                    SourceTraceEntryProductionOwnershipReviewRequest request = completeFixtureRequest();
                    request.setEntrySourceReason("fixture-label " + token + " fixture-label");

                    SourceTraceEntryProductionOwnershipReviewResult result = boundary.reviewEntryOwnership(request);

                    assertMandatoryFailClosedFixtureOutcome(result);
                    assertThat(result.getUnsafeFields()).contains("positiveLookingLabel");
                    assertThat(result.getBlockingFields()).contains(
                            "positiveLookingLabel",
                            "positiveLookingLabelToken:" + token
                    );
                }));
    }

    @Test
    void nullRequestRemainsFailClosedAndReviewOnly() {
        SourceTraceEntryProductionOwnershipReviewResult result = boundary.reviewEntryOwnership(null);

        assertMandatoryFailClosedFixtureOutcome(result);
        assertThat(result.getMissingFields()).containsExactly("request");
        assertThat(result.getBlockingFields()).contains("nullRequest");
    }

    @Test
    void implementationExposesNoForbiddenOrGeneratedTradingValueSurface() {
        Class<?> type = FailClosedSourceTraceEntryProductionOwnershipReviewBoundary.class;

        assertThat(Arrays.stream(type.getDeclaredMethods()).map(Method::getName))
                .noneMatch(this::containsForbiddenSurfaceToken)
                .noneMatch(this::containsGeneratedTradingValueToken);
        assertThat(Arrays.stream(type.getDeclaredFields()).map(Field::getName))
                .noneMatch(this::containsForbiddenSurfaceToken)
                .noneMatch(this::containsGeneratedTradingValueToken);
        assertThat(Arrays.stream(type.getDeclaredMethods()).map(Method::getReturnType))
                .doesNotContain(BigDecimal.class);
        assertThat(Arrays.stream(type.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getParameterTypes())))
                .doesNotContain(BigDecimal.class);
        assertThat(Arrays.stream(type.getDeclaredFields()).map(Field::getType))
                .doesNotContain(BigDecimal.class);
    }

    @Test
    void implementationHasNoSpringAnnotationsAndProductionClassesRemainAbsent() {
        Class<?> type = FailClosedSourceTraceEntryProductionOwnershipReviewBoundary.class;

        assertThat(type.getAnnotation(Service.class)).isNull();
        assertThat(type.getAnnotation(Component.class)).isNull();
        assertThat(type.getAnnotation(Repository.class)).isNull();
        assertThat(type.getAnnotation(Controller.class)).isNull();
        assertThat(type.getAnnotation(RestController.class)).isNull();
        assertThat(Arrays.stream(type.getAnnotations()).map(Annotation::annotationType))
                .doesNotContain(Service.class, Component.class, Repository.class, Controller.class, RestController.class);
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryOwnershipAdapter"
                ))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryCompletionContract"
                ))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryProductionOwnershipReviewBoundary"
                ))
                .isInstanceOf(ClassNotFoundException.class);
    }

    private void assertFixtureCase(EdgeCase edgeCase) {
        SourceTraceEntryProductionOwnershipReviewRequest request = completeFixtureRequest();
        edgeCase.mutate().accept(request);

        SourceTraceEntryProductionOwnershipReviewResult result = boundary.reviewEntryOwnership(request);

        assertMandatoryFailClosedFixtureOutcome(result);
        assertThat(result.getUnsafeFields()).containsAll(edgeCase.expectedUnsafeFields());
        assertThat(result.getBlockingFields()).containsAll(edgeCase.expectedBlockingFields());
        assertThat(result.getMissingFields()).containsAll(edgeCase.expectedMissingFields());
    }

    private SourceTraceEntryProductionOwnershipReviewRequest completeFixtureRequest() {
        SourceTraceEntryProductionOwnershipReviewRequest request =
                new SourceTraceEntryProductionOwnershipReviewRequest();
        request.setSymbol("FIXTURE_SYMBOL");
        request.setTimeframe("fixture-timeframe");
        request.setSourceTraceEntryOwnershipCompletionPath("fixture-owner-completion-path");
        request.setEntryPriceSource("fixture-entry-source-metadata");
        request.setEntrySourceType("FIXTURE_RULE_OWNED_BOUNDARY");
        request.setEntrySourceTimeframe("fixture-timeframe");
        request.setEntrySourceReason("fixture-review-only-reason");
        request.setEntrySourceRef("fixture-source-ref");
        request.setSourceWindow("fixture-source-window");
        request.setRuleId("fixture-rule-id");
        request.setRuleVersion("fixture-rule-version");
        request.setFreshnessOwnership("fixture-freshness-owner");
        request.setConflictFamilyOwnership("fixture-conflict-family-owner");
        request.setAuthenticationVisibility("fixture-review-visibility");
        request.setOwnerEvidenceFields(OWNER_EVIDENCE_FIELDS);
        request.setSourceRefs(List.of("fixture-source-ref"));
        request.setMissingFields(List.of("productionOwnershipReviewBoundaryUnwired"));
        return request;
    }

    private void assertMandatoryFailClosedFixtureOutcome(SourceTraceEntryProductionOwnershipReviewResult result) {
        assertThat(result.getReviewStatus())
                .isEqualTo(SourceTraceEntryProductionOwnershipReviewStatusEnum.INCOMPLETE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.getDowngradeReason())
                .isEqualTo(SourceTraceEntryProductionOwnershipReviewDowngradeReasonEnum.REVIEW_BOUNDARY_UNWIRED);
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isSourceTraceEntryCompleted()).isFalse();
        assertThat(result.isCompletionReady()).isFalse();
        assertThat(result.getBlockingFields()).containsAll(PRODUCTION_WIRING_BLOCKERS);
    }

    private EdgeCase edgeCase(
            String name,
            Consumer<SourceTraceEntryProductionOwnershipReviewRequest> mutate,
            List<String> expectedUnsafeFields,
            List<String> expectedBlockingFields,
            List<String> expectedMissingFields
    ) {
        return new EdgeCase(name, mutate, expectedUnsafeFields, expectedBlockingFields, expectedMissingFields);
    }

    private boolean containsForbiddenSurfaceToken(String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        return FORBIDDEN_SURFACE_TOKENS.stream()
                .map(token -> token.toLowerCase(Locale.ROOT))
                .anyMatch(lowerName::contains);
    }

    private boolean containsGeneratedTradingValueToken(String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        return GENERATED_TRADING_VALUE_TOKENS.stream()
                .map(token -> token.toLowerCase(Locale.ROOT))
                .anyMatch(lowerName::contains);
    }

    private record EdgeCase(
            String name,
            Consumer<SourceTraceEntryProductionOwnershipReviewRequest> mutate,
            List<String> expectedUnsafeFields,
            List<String> expectedBlockingFields,
            List<String> expectedMissingFields
    ) {
    }
}
