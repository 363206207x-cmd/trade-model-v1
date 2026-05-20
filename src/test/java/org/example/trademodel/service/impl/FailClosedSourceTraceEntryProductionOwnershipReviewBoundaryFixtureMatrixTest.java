package org.example.trademodel.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipAuditEnvelope;
import org.example.trademodel.dto.planboundary.SourceTraceEntryProductionOwnershipConsumerIsolationEnvelope;
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

class FailClosedSourceTraceEntryProductionOwnershipReviewBoundaryFixtureMatrixTest {

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
    private static final List<String> PRODUCTION_WIRING_BLOCKERS = List.of(
            "productionOwnershipReviewBoundaryUnwired",
            "productionWiringStillBlocked",
            "failClosedImplementationSkeleton"
    );

    private final FailClosedSourceTraceEntryProductionOwnershipReviewBoundary boundary =
            new FailClosedSourceTraceEntryProductionOwnershipReviewBoundary();

    @TestFactory
    Stream<DynamicTest> p95FixtureMatrixCasesRemainFailClosedReviewOnlyAndBlockerPreserving() {
        return fixtureCases().stream()
                .map(fixtureCase -> DynamicTest.dynamicTest(fixtureCase.name(), () -> {
                    SourceTraceEntryProductionOwnershipReviewRequest request = completeFixtureRequest();
                    fixtureCase.mutate().accept(request);

                    SourceTraceEntryProductionOwnershipReviewResult result = boundary.reviewEntryOwnership(request);

                    assertMandatoryFixtureOutcome(result);
                    assertThat(result.getMissingFields()).containsAll(fixtureCase.expectedMissingFields());
                    assertThat(result.getUnsafeFields()).containsAll(fixtureCase.expectedUnsafeFields());
                    assertThat(result.getBlockingFields()).containsAll(fixtureCase.expectedBlockingFields());
                }));
    }

    @Test
    void p95FixtureMatrixIntroducesNoProductionWiringSurfaceOrMarketDataDependency() {
        Class<?> type = FailClosedSourceTraceEntryProductionOwnershipReviewBoundary.class;

        assertThat(type.getAnnotation(Service.class)).isNull();
        assertThat(type.getAnnotation(Component.class)).isNull();
        assertThat(type.getAnnotation(Repository.class)).isNull();
        assertThat(type.getAnnotation(Controller.class)).isNull();
        assertThat(type.getAnnotation(RestController.class)).isNull();
        assertThat(Arrays.stream(type.getDeclaredMethods()).map(Method::getReturnType))
                .doesNotContain(BigDecimal.class);
        assertThat(Arrays.stream(type.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getParameterTypes())))
                .doesNotContain(BigDecimal.class);
        assertThat(Arrays.stream(type.getDeclaredFields()).map(Field::getType))
                .containsOnly(List.class, String.class);
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

    private List<FixtureCase> fixtureCases() {
        List<FixtureCase> cases = new ArrayList<>();
        cases.add(new FixtureCase(
                "safe-looking complete owner evidence still fails closed",
                request -> { },
                List.of(),
                PRODUCTION_WIRING_BLOCKERS,
                List.of("auditEnvelope", "consumerIsolationEnvelope")
        ));
        cases.add(new FixtureCase(
                "unsafe substitution fixture tokens preserve blocker evidence",
                request -> request.setEntryPriceSource(String.join(" ", UNSAFE_SUBSTITUTION_TOKENS)),
                List.of("runtimeLikeSubstitution"),
                withPrefixedBlockers("runtimeLikeSubstitution", "runtimeSubstitutionToken:",
                        UNSAFE_SUBSTITUTION_TOKENS),
                List.of()
        ));
        cases.add(new FixtureCase(
                "ambiguous owner evidence fails closed",
                request -> {
                    request.setEntrySourceRef("ambiguous-fixture-source-ref");
                    request.setOwnerEvidenceFields(List.of("entryPriceSource", "entryPriceSource"));
                    request.setSourceRefs(List.of("fixture-source-ref-a", "fixture-source-ref-b"));
                },
                List.of("duplicateOwnerEvidence", "ambiguousOwnerEvidence"),
                List.of("duplicateOwnerEvidence", "ambiguousOwnerEvidence"),
                List.of()
        ));
        cases.add(new FixtureCase(
                "stale owner evidence fails closed",
                request -> request.setFreshnessOwnership("stale-fixture-owner-evidence"),
                List.of("staleOwnerEvidence"),
                List.of("staleOwnerEvidence"),
                List.of()
        ));
        cases.add(new FixtureCase(
                "missing audit fails closed",
                request -> request.setAuditEnvelope(null),
                List.of(),
                List.of("auditMetadataMissing"),
                List.of("auditEnvelope")
        ));
        cases.add(new FixtureCase(
                "incomplete audit fields preserve blocker evidence",
                request -> {
                    SourceTraceEntryProductionOwnershipAuditEnvelope auditEnvelope =
                            new SourceTraceEntryProductionOwnershipAuditEnvelope();
                    auditEnvelope.setMissingAuditFields(List.of("ownerId", "consumerIsolationProof"));
                    request.setAuditEnvelope(auditEnvelope);
                },
                List.of(),
                List.of("auditMetadataMissing", "ownerId", "consumerIsolationProof"),
                List.of("auditEnvelope")
        ));
        cases.add(new FixtureCase(
                "missing visibility fails closed and withholds payload",
                request -> request.setAuthenticationVisibility(" "),
                List.of(),
                List.of("authenticationVisibilityMissing", "payloadWithheldForReview"),
                List.of("authenticationVisibility")
        ));
        cases.add(new FixtureCase(
                "unauthorized and ambiguous visibility fails closed and withholds payload",
                request -> request.setAuthenticationVisibility("unauthorized ambiguous fixture visibility"),
                List.of("unauthorizedVisibility", "ambiguousVisibility"),
                List.of("unauthorizedVisibility", "ambiguousVisibility", "payloadWithheldForReview"),
                List.of()
        ));
        cases.add(new FixtureCase(
                "missing consumer isolation fails closed",
                request -> request.setConsumerIsolationEnvelope(null),
                List.of(),
                List.of(
                        "consumerIsolationMissing",
                        "boundaryCandidateServiceValidIsolation",
                        "executionPlanReadinessIsolation",
                        "dashboardMutationIsolation",
                        "schemaPersistenceIsolation",
                        "resolverIsolation",
                        "validatorReadinessIsolation",
                        "orderPathIsolation",
                        "executionPathIsolation",
                        "automationPathIsolation",
                        "externalDataPathIsolation"
                ),
                List.of("consumerIsolationEnvelope")
        ));
        cases.add(new FixtureCase(
                "partial consumer isolation preserves blocked consumer evidence",
                request -> {
                    SourceTraceEntryProductionOwnershipConsumerIsolationEnvelope isolationEnvelope =
                            new SourceTraceEntryProductionOwnershipConsumerIsolationEnvelope();
                    isolationEnvelope.setIsolatedConsumerFamilies(List.of("dashboardMutationIsolation"));
                    isolationEnvelope.setMissingIsolationFields(List.of(
                            "orderPathIsolation",
                            "executionPathIsolation"
                    ));
                    isolationEnvelope.setBlockedConsumerFamilies(List.of("automationPathIsolation"));
                    request.setConsumerIsolationEnvelope(isolationEnvelope);
                },
                List.of(),
                List.of(
                        "consumerIsolationMissing",
                        "orderPathIsolation",
                        "executionPathIsolation",
                        "automationPathIsolation"
                ),
                List.of("consumerIsolationEnvelope")
        ));
        cases.add(new FixtureCase(
                "Risk Action Guard fixture tokens remain review-only blockers",
                request -> request.setConflictFamilyOwnership(String.join(" ", RISK_ACTION_GUARD_TOKENS)),
                List.of("riskActionGuardReviewRequired"),
                withPrefixedBlockers("riskActionGuardReviewRequired", "riskActionGuardToken:",
                        RISK_ACTION_GUARD_TOKENS),
                List.of()
        ));
        cases.add(new FixtureCase(
                "positive-looking labels do not imply readiness or action behavior",
                request -> request.setEntrySourceReason(String.join(" ", POSITIVE_LOOKING_TOKENS)
                        + " close reverse fixture labels"),
                List.of("positiveLookingLabel"),
                withPrefixedBlockers("positiveLookingLabel", "positiveLookingLabelToken:",
                        POSITIVE_LOOKING_TOKENS),
                List.of()
        ));
        cases.add(new FixtureCase(
                "downgrade and rollback fixture output preserves fail-closed blocker evidence",
                request -> {
                    SourceTraceEntryProductionOwnershipAuditEnvelope auditEnvelope =
                            new SourceTraceEntryProductionOwnershipAuditEnvelope();
                    auditEnvelope.setDowngradeReason("downgrade-required");
                    auditEnvelope.setRollbackReason("rollback-required");
                    request.setAuditEnvelope(auditEnvelope);
                },
                List.of("downgradeRequired", "rollbackRequired"),
                List.of("downgradeRequired", "rollbackRequired"),
                List.of()
        ));
        return cases;
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

    private void assertMandatoryFixtureOutcome(SourceTraceEntryProductionOwnershipReviewResult result) {
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

    private List<String> withPrefixedBlockers(String genericBlocker, String tokenPrefix, List<String> tokens) {
        List<String> blockers = new ArrayList<>();
        blockers.add(genericBlocker);
        tokens.stream()
                .map(token -> tokenPrefix + token)
                .forEach(blockers::add);
        return blockers;
    }

    private record FixtureCase(
            String name,
            Consumer<SourceTraceEntryProductionOwnershipReviewRequest> mutate,
            List<String> expectedUnsafeFields,
            List<String> expectedBlockingFields,
            List<String> expectedMissingFields
    ) {
    }
}
