package org.example.trademodel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

class EntryCompletionProductionOwnershipFixtureMatrixTest {

    private static final List<String> PRODUCTION_CONSUMER_BLOCKERS = List.of(
            "boundaryCandidateServiceValid",
            "executionPlanReadiness",
            "dashboardMutation",
            "orderPath",
            "executionPath",
            "automationPath",
            "externalDataPath"
    );
    private static final List<String> RISK_ACTION_GUARD_BLOCKERS = List.of(
            "highRiskDoesNotMeanAction",
            "wickPinBarNotTrendReversal",
            "liquidityStressRequiresReview",
            "missingEventDataNotNoRisk",
            "multiTimeframeAgreementNotSourceTraceCompletion"
    );
    private static final List<String> POSITIVE_LOOKING_MARKERS = List.of(
            "positiveFixtureName",
            "validLookingValue",
            "completedLookingValue",
            "readyLookingValue"
    );
    private static final List<String> FORBIDDEN_METHOD_TOKENS = List.of(
            "order",
            "execution",
            "close",
            "reverse",
            "autoTrading",
            "autoTrade",
            "tradeReady",
            "readyToTrade"
    );

    @Test
    void fixtureMatrixCoversEveryOwnershipFieldAndDimension() {
        List<OwnershipFixture> fixtures = allFixtures();

        assertThat(fixtures)
                .hasSize(OwnershipField.values().length * OwnershipDimension.values().length);
        for (OwnershipField field : OwnershipField.values()) {
            for (OwnershipDimension dimension : OwnershipDimension.values()) {
                assertThat(fixtures)
                        .filteredOn(fixture -> fixture.field() == field && fixture.dimension() == dimension)
                        .hasSize(1);
            }
        }
    }

    @Test
    void ownerPresentFixturesRemainReviewOnlyAndNonInstructional() {
        assertThat(fixturesFor(OwnershipDimension.OWNER_PRESENT))
                .allSatisfy(fixture -> {
                    assertCoreSafety(fixture);
                    assertThat(fixture.outcome()).isEqualTo(FixtureOutcome.REVIEW_ONLY);
                    assertThat(fixture.blockingFields()).contains("productionWiringStillBlocked");
                    assertThat(fixture.missingFields()).isEmpty();
                    assertThat(fixture.unsafeFields()).isEmpty();
                });
    }

    @Test
    void ownerMissingFixturesFailClosed() {
        assertThat(fixturesFor(OwnershipDimension.OWNER_MISSING))
                .allSatisfy(fixture -> {
                    assertCoreSafety(fixture);
                    assertThat(fixture.outcome()).isEqualTo(FixtureOutcome.MISSING_REQUIRED_FIELD);
                    assertThat(fixture.missingFields()).containsExactly(fixture.field().fieldKey());
                    assertThat(fixture.blockingFields()).contains("ownerMissing");
                });
    }

    @Test
    void duplicateOwnerFixturesFailClosed() {
        assertUnsafeDimension(OwnershipDimension.DUPLICATE_OWNER, "duplicateOwner");
    }

    @Test
    void ambiguousOwnerFixturesFailClosed() {
        assertUnsafeDimension(OwnershipDimension.AMBIGUOUS_OWNER, "ambiguousOwner");
    }

    @Test
    void staleOwnerFixturesFailClosed() {
        assertUnsafeDimension(OwnershipDimension.STALE_OWNER, "staleOwner");
    }

    @Test
    void forbiddenSubstitutionFixturesFailClosed() {
        assertThat(fixturesFor(OwnershipDimension.FORBIDDEN_SUBSTITUTION))
                .allSatisfy(fixture -> {
                    assertCoreSafety(fixture);
                    assertThat(fixture.outcome()).isEqualTo(FixtureOutcome.UNSAFE_COMPLETION);
                    assertThat(fixture.unsafeFields())
                            .contains(
                                    fixture.field().fieldKey() + ".forbiddenSubstitution",
                                    "latestPriceOnly",
                                    "rawKlineOnly",
                                    "aiText",
                                    "dashboardText",
                                    "externalData",
                                    "orderData",
                                    "executionData"
                            );
                    assertThat(fixture.blockingFields()).contains("forbiddenSubstitution");
                });
    }

    @Test
    void latestPriceOnlySubstitutionFailsClosedForEveryOwnershipField() {
        assertSubstitutionSourceFailsClosedForEveryField(RuntimeSubstitutionSource.LATEST_PRICE_ONLY);
    }

    @Test
    void rawKlineOnlySubstitutionFailsClosedForEveryOwnershipField() {
        assertSubstitutionSourceFailsClosedForEveryField(RuntimeSubstitutionSource.RAW_KLINE_ONLY);
    }

    @Test
    void aiTextSubstitutionFailsClosedForEveryOwnershipField() {
        assertSubstitutionSourceFailsClosedForEveryField(RuntimeSubstitutionSource.AI_TEXT);
    }

    @Test
    void dashboardTextSubstitutionFailsClosedForEveryOwnershipField() {
        assertSubstitutionSourceFailsClosedForEveryField(RuntimeSubstitutionSource.DASHBOARD_TEXT);
    }

    @Test
    void externalDataSubstitutionFailsClosedForEveryOwnershipField() {
        assertSubstitutionSourceFailsClosedForEveryField(RuntimeSubstitutionSource.EXTERNAL_DATA);
    }

    @Test
    void orderExecutionDataSubstitutionFailsClosedForEveryOwnershipField() {
        assertSubstitutionSourceFailsClosedForEveryField(RuntimeSubstitutionSource.ORDER_EXECUTION_DATA);
    }

    @Test
    void downgradeRequiredFixturesRemainFailClosed() {
        assertThat(fixturesFor(OwnershipDimension.DOWNGRADE_REQUIRED))
                .allSatisfy(fixture -> {
                    assertCoreSafety(fixture);
                    assertThat(fixture.outcome()).isEqualTo(FixtureOutcome.DOWNGRADED_FAIL_CLOSED);
                    assertThat(fixture.blockingFields()).contains("downgradeRequired");
                    assertThat(fixture.completionStatus()).isEqualTo("INCOMPLETE");
                    assertThat(fixture.completionTransition()).isEqualTo("NONE");
                });
    }

    @Test
    void rollbackRequiredFixturesReturnFailClosedReviewOutput() {
        assertThat(fixturesFor(OwnershipDimension.ROLLBACK_REQUIRED))
                .allSatisfy(fixture -> {
                    assertCoreSafety(fixture);
                    assertThat(fixture.outcome()).isEqualTo(FixtureOutcome.ROLLED_BACK_FAIL_CLOSED);
                    assertThat(fixture.blockingFields()).contains("rollbackRequired");
                    assertThat(fixture.completionStatus()).isEqualTo("INCOMPLETE");
                    assertThat(fixture.completionTransition()).isEqualTo("NONE");
                });
    }

    @Test
    void auditRequiredFixturesBlockCompletion() {
        assertThat(fixturesFor(OwnershipDimension.AUDIT_REQUIRED))
                .allSatisfy(fixture -> {
                    assertCoreSafety(fixture);
                    assertThat(fixture.outcome()).isEqualTo(FixtureOutcome.AUDIT_BLOCKED);
                    assertThat(fixture.blockingFields())
                            .contains("auditRequired", fixture.field().fieldKey() + ".audit");
                });
    }

    @Test
    void missingAuditMetadataFailsClosedForEveryOwnerPresentFixture() {
        assertThat(ownerPresentFixturesWithMissingAuditMetadata())
                .hasSize(OwnershipField.values().length)
                .allSatisfy(fixture -> {
                    assertCoreSafety(fixture);
                    assertThat(fixture.outcome()).isEqualTo(FixtureOutcome.AUDIT_BLOCKED);
                    assertThat(fixture.blockingFields())
                            .contains("auditMetadataMissing", fixture.field().fieldKey() + ".auditMetadata");
                });
    }

    @Test
    void consumerIsolationRequiredFixturesBlockReadinessValidMutationAndRuntimeConsumers() {
        assertThat(fixturesFor(OwnershipDimension.CONSUMER_ISOLATION_REQUIRED))
                .allSatisfy(fixture -> {
                    assertCoreSafety(fixture);
                    assertThat(fixture.outcome()).isEqualTo(FixtureOutcome.CONSUMER_ISOLATION_BLOCKED);
                    assertThat(fixture.blockingFields()).containsAll(PRODUCTION_CONSUMER_BLOCKERS);
                });
    }

    @Test
    void missingConsumerIsolationFailsClosedForEveryOwnerPresentFixture() {
        assertThat(ownerPresentFixturesWithMissingConsumerIsolation())
                .hasSize(OwnershipField.values().length)
                .allSatisfy(fixture -> {
                    assertCoreSafety(fixture);
                    assertThat(fixture.outcome()).isEqualTo(FixtureOutcome.CONSUMER_ISOLATION_BLOCKED);
                    assertThat(fixture.blockingFields())
                            .contains("consumerIsolationMissing")
                            .containsAll(PRODUCTION_CONSUMER_BLOCKERS);
                });
    }

    @Test
    void authenticationVisibilityRequiredFixturesBlockOrWithholdPayload() {
        assertThat(fixturesFor(OwnershipDimension.AUTHENTICATION_VISIBILITY_REQUIRED))
                .allSatisfy(fixture -> {
                    assertCoreSafety(fixture);
                    assertThat(fixture.outcome()).isEqualTo(FixtureOutcome.VISIBILITY_BLOCKED);
                    assertThat(fixture.blockingFields()).contains("authenticationVisibilityRequired");
                    assertThat(fixture.payloadWithheld()).isTrue();
                });
    }

    @Test
    void missingAuthenticationVisibilityFailsClosedOrWithholdsEveryOwnerPresentPayload() {
        assertThat(ownerPresentFixturesWithMissingAuthenticationVisibility())
                .hasSize(OwnershipField.values().length)
                .allSatisfy(fixture -> {
                    assertCoreSafety(fixture);
                    assertThat(fixture.outcome()).isEqualTo(FixtureOutcome.VISIBILITY_BLOCKED);
                    assertThat(fixture.blockingFields())
                            .contains("authenticationMissing", "visibilityMissing");
                    assertThat(fixture.payloadWithheld()).isTrue();
                });
    }

    @Test
    void riskActionGuardRequiredFixturesBlockCompletionAndRequireReview() {
        assertThat(fixturesFor(OwnershipDimension.RISK_ACTION_GUARD_REQUIRED))
                .allSatisfy(fixture -> {
                    assertCoreSafety(fixture);
                    assertThat(fixture.outcome()).isEqualTo(FixtureOutcome.RISK_GUARD_BLOCKED);
                    assertThat(fixture.blockingFields()).containsAll(RISK_ACTION_GUARD_BLOCKERS);
                    assertThat(fixture.manualReviewRequired()).isTrue();
                });
    }

    @Test
    void riskActionGuardCasesStayReviewOnlyAndBlockCompletion() {
        assertThat(riskActionGuardFixtures())
                .hasSize(RISK_ACTION_GUARD_BLOCKERS.size())
                .allSatisfy(fixture -> {
                    assertCoreSafety(fixture);
                    assertThat(fixture.outcome()).isEqualTo(FixtureOutcome.RISK_GUARD_BLOCKED);
                    assertThat(fixture.reviewMode()).isEqualTo("REVIEW_ONLY");
                    assertThat(fixture.manualReviewRequired()).isTrue();
                    assertThat(fixture.notTradeInstruction()).isTrue();
                    assertThat(fixture.blockingFields()).contains("riskActionGuardRequired");
                });
    }

    @Test
    void positiveLookingFixtureNamesOrValuesDoNotBecomeReadinessValidOrderOrExecutionBehavior() {
        assertThat(positiveLookingFixtures())
                .hasSize(OwnershipField.values().length)
                .allSatisfy(fixture -> {
                    assertCoreSafety(fixture);
                    assertThat(fixture.outcome()).isEqualTo(FixtureOutcome.UNSAFE_COMPLETION);
                    assertThat(fixture.blockingFields()).containsAll(POSITIVE_LOOKING_MARKERS);
                    assertThat(fixture.unsafeFields())
                            .contains(fixture.field().fieldKey() + ".positiveLookingRuntimeInterpretation");
                });
    }

    @Test
    void noFixtureCanGenerateRealEntryStopTakeProfitOrRiskRewardValues() {
        assertThat(allGuardFixtures())
                .allSatisfy(fixture -> {
                    assertThat(fixture.generatedEntryValue()).isNull();
                    assertThat(fixture.generatedStopValue()).isNull();
                    assertThat(fixture.generatedTakeProfitValue()).isNull();
                    assertThat(fixture.generatedRiskRewardValue()).isNull();
                });
    }

    @Test
    void fixturesCannotCreateProductionWiringOrServiceRegistration() {
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryCompletionContract"
                ))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryOwnershipAdapter"
                ))
                .isInstanceOf(ClassNotFoundException.class);

        List<Class<?>> fixtureTypes = List.of(
                OwnershipFixture.class,
                OwnershipField.class,
                OwnershipDimension.class,
                FixtureOutcome.class,
                RuntimeSubstitutionSource.class
        );
        for (Class<?> fixtureType : fixtureTypes) {
            assertThat(fixtureType.getAnnotation(Service.class)).isNull();
            assertThat(fixtureType.getAnnotation(Component.class)).isNull();
            assertThat(fixtureType.getAnnotation(Repository.class)).isNull();
            assertThat(fixtureType.getAnnotation(Controller.class)).isNull();
            assertThat(fixtureType.getAnnotation(RestController.class)).isNull();
            assertThat(Arrays.stream(fixtureType.getDeclaredMethods()).map(Method::getName))
                    .noneMatch(this::containsForbiddenMethodToken);
        }
    }

    private void assertSubstitutionSourceFailsClosedForEveryField(RuntimeSubstitutionSource source) {
        List<OwnershipFixture> fixtures = substitutionFixturesFor(source);

        assertThat(fixtures)
                .hasSize(OwnershipField.values().length)
                .allSatisfy(fixture -> {
                    assertCoreSafety(fixture);
                    assertThat(fixture.outcome()).isEqualTo(FixtureOutcome.UNSAFE_COMPLETION);
                    assertThat(fixture.unsafeFields())
                            .contains(
                                    fixture.field().fieldKey() + ".forbiddenSubstitution",
                                    source.blocker()
                            );
                    assertThat(fixture.blockingFields())
                            .contains("forbiddenSubstitution", source.blocker());
                });
    }

    private void assertUnsafeDimension(OwnershipDimension dimension, String blocker) {
        assertThat(fixturesFor(dimension))
                .allSatisfy(fixture -> {
                    assertCoreSafety(fixture);
                    assertThat(fixture.outcome()).isEqualTo(FixtureOutcome.UNSAFE_COMPLETION);
                    assertThat(fixture.unsafeFields()).contains(fixture.field().fieldKey() + "." + blocker);
                    assertThat(fixture.blockingFields()).contains(blocker);
                });
    }

    private void assertCoreSafety(OwnershipFixture fixture) {
        assertThat(fixture.completionStatus()).isEqualTo("INCOMPLETE");
        assertThat(fixture.completionTransition()).isEqualTo("NONE");
        assertThat(fixture.reviewMode()).isEqualTo("REVIEW_ONLY");
        assertThat(fixture.manualReviewRequired()).isTrue();
        assertThat(fixture.notTradeInstruction()).isTrue();
        assertThat(fixture.sourceTraceEntryCompleted()).isFalse();
        assertThat(fixture.completionReady()).isFalse();
        assertThat(fixture.boundaryCandidateServiceValid()).isFalse();
        assertThat(fixture.planReadinessUnlocked()).isFalse();
        assertThat(fixture.dashboardMutationAllowed()).isFalse();
        assertThat(fixture.productionWiringCreated()).isFalse();
        assertThat(fixture.serviceRegistered()).isFalse();
    }

    private List<OwnershipFixture> fixturesFor(OwnershipDimension dimension) {
        return allFixtures().stream()
                .filter(fixture -> fixture.dimension() == dimension)
                .toList();
    }

    private List<OwnershipFixture> substitutionFixturesFor(RuntimeSubstitutionSource source) {
        return Arrays.stream(OwnershipField.values())
                .map(field -> fixtureWithSingleSubstitution(field, source))
                .toList();
    }

    private List<OwnershipFixture> ownerPresentFixturesWithMissingAuditMetadata() {
        return Arrays.stream(OwnershipField.values())
                .map(this::ownerPresentFixtureWithMissingAuditMetadata)
                .toList();
    }

    private List<OwnershipFixture> ownerPresentFixturesWithMissingConsumerIsolation() {
        return Arrays.stream(OwnershipField.values())
                .map(this::ownerPresentFixtureWithMissingConsumerIsolation)
                .toList();
    }

    private List<OwnershipFixture> ownerPresentFixturesWithMissingAuthenticationVisibility() {
        return Arrays.stream(OwnershipField.values())
                .map(this::ownerPresentFixtureWithMissingAuthenticationVisibility)
                .toList();
    }

    private List<OwnershipFixture> riskActionGuardFixtures() {
        return RISK_ACTION_GUARD_BLOCKERS.stream()
                .map(this::riskActionGuardFixture)
                .toList();
    }

    private List<OwnershipFixture> positiveLookingFixtures() {
        return Arrays.stream(OwnershipField.values())
                .map(this::positiveLookingFixture)
                .toList();
    }

    private List<OwnershipFixture> allFixtures() {
        List<OwnershipFixture> fixtures = new ArrayList<>();
        for (OwnershipField field : OwnershipField.values()) {
            for (OwnershipDimension dimension : OwnershipDimension.values()) {
                fixtures.add(fixture(field, dimension));
            }
        }
        return fixtures;
    }

    private List<OwnershipFixture> allGuardFixtures() {
        List<OwnershipFixture> fixtures = new ArrayList<>(allFixtures());
        for (RuntimeSubstitutionSource source : RuntimeSubstitutionSource.values()) {
            fixtures.addAll(substitutionFixturesFor(source));
        }
        fixtures.addAll(ownerPresentFixturesWithMissingAuditMetadata());
        fixtures.addAll(ownerPresentFixturesWithMissingConsumerIsolation());
        fixtures.addAll(ownerPresentFixturesWithMissingAuthenticationVisibility());
        fixtures.addAll(riskActionGuardFixtures());
        fixtures.addAll(positiveLookingFixtures());
        return fixtures;
    }

    private OwnershipFixture fixtureWithSingleSubstitution(OwnershipField field, RuntimeSubstitutionSource source) {
        return failClosedFixture(
                field,
                OwnershipDimension.FORBIDDEN_SUBSTITUTION,
                FixtureOutcome.UNSAFE_COMPLETION,
                List.of(),
                List.of(field.fieldKey() + ".forbiddenSubstitution", source.blocker()),
                List.of("forbiddenSubstitution", source.blocker()),
                false
        );
    }

    private OwnershipFixture ownerPresentFixtureWithMissingAuditMetadata(OwnershipField field) {
        return failClosedFixture(
                field,
                OwnershipDimension.AUDIT_REQUIRED,
                FixtureOutcome.AUDIT_BLOCKED,
                List.of(),
                List.of(),
                List.of("auditMetadataMissing", field.fieldKey() + ".auditMetadata"),
                false
        );
    }

    private OwnershipFixture ownerPresentFixtureWithMissingConsumerIsolation(OwnershipField field) {
        List<String> blockers = new ArrayList<>();
        blockers.add("consumerIsolationMissing");
        blockers.addAll(PRODUCTION_CONSUMER_BLOCKERS);
        return failClosedFixture(
                field,
                OwnershipDimension.CONSUMER_ISOLATION_REQUIRED,
                FixtureOutcome.CONSUMER_ISOLATION_BLOCKED,
                List.of(),
                List.of(),
                blockers,
                false
        );
    }

    private OwnershipFixture ownerPresentFixtureWithMissingAuthenticationVisibility(OwnershipField field) {
        return failClosedFixture(
                field,
                OwnershipDimension.AUTHENTICATION_VISIBILITY_REQUIRED,
                FixtureOutcome.VISIBILITY_BLOCKED,
                List.of(),
                List.of(),
                List.of("authenticationMissing", "visibilityMissing", field.fieldKey() + ".visibility"),
                true
        );
    }

    private OwnershipFixture riskActionGuardFixture(String blocker) {
        return failClosedFixture(
                OwnershipField.CONFLICT_FAMILY,
                OwnershipDimension.RISK_ACTION_GUARD_REQUIRED,
                FixtureOutcome.RISK_GUARD_BLOCKED,
                List.of(),
                List.of(),
                List.of("riskActionGuardRequired", blocker),
                false
        );
    }

    private OwnershipFixture positiveLookingFixture(OwnershipField field) {
        List<String> blockers = new ArrayList<>(POSITIVE_LOOKING_MARKERS);
        blockers.add("productionWiringStillBlocked");
        return failClosedFixture(
                field,
                OwnershipDimension.FORBIDDEN_SUBSTITUTION,
                FixtureOutcome.UNSAFE_COMPLETION,
                List.of(),
                List.of(field.fieldKey() + ".positiveLookingRuntimeInterpretation"),
                blockers,
                false
        );
    }

    private OwnershipFixture fixture(OwnershipField field, OwnershipDimension dimension) {
        List<String> missingFields = new ArrayList<>();
        List<String> unsafeFields = new ArrayList<>();
        List<String> blockingFields = new ArrayList<>();
        FixtureOutcome outcome;
        boolean payloadWithheld = false;

        switch (dimension) {
            case OWNER_PRESENT -> {
                outcome = FixtureOutcome.REVIEW_ONLY;
                blockingFields.add("productionWiringStillBlocked");
            }
            case OWNER_MISSING -> {
                outcome = FixtureOutcome.MISSING_REQUIRED_FIELD;
                missingFields.add(field.fieldKey());
                blockingFields.add("ownerMissing");
            }
            case DUPLICATE_OWNER -> {
                outcome = FixtureOutcome.UNSAFE_COMPLETION;
                unsafeFields.add(field.fieldKey() + ".duplicateOwner");
                blockingFields.add("duplicateOwner");
            }
            case AMBIGUOUS_OWNER -> {
                outcome = FixtureOutcome.UNSAFE_COMPLETION;
                unsafeFields.add(field.fieldKey() + ".ambiguousOwner");
                blockingFields.add("ambiguousOwner");
            }
            case STALE_OWNER -> {
                outcome = FixtureOutcome.UNSAFE_COMPLETION;
                unsafeFields.add(field.fieldKey() + ".staleOwner");
                blockingFields.add("staleOwner");
            }
            case FORBIDDEN_SUBSTITUTION -> {
                outcome = FixtureOutcome.UNSAFE_COMPLETION;
                unsafeFields.add(field.fieldKey() + ".forbiddenSubstitution");
                unsafeFields.addAll(List.of(
                        "latestPriceOnly",
                        "rawKlineOnly",
                        "aiText",
                        "dashboardText",
                        "externalData",
                        "orderData",
                        "executionData"
                ));
                blockingFields.add("forbiddenSubstitution");
            }
            case DOWNGRADE_REQUIRED -> {
                outcome = FixtureOutcome.DOWNGRADED_FAIL_CLOSED;
                blockingFields.add("downgradeRequired");
            }
            case ROLLBACK_REQUIRED -> {
                outcome = FixtureOutcome.ROLLED_BACK_FAIL_CLOSED;
                blockingFields.add("rollbackRequired");
            }
            case AUDIT_REQUIRED -> {
                outcome = FixtureOutcome.AUDIT_BLOCKED;
                blockingFields.add("auditRequired");
                blockingFields.add(field.fieldKey() + ".audit");
            }
            case CONSUMER_ISOLATION_REQUIRED -> {
                outcome = FixtureOutcome.CONSUMER_ISOLATION_BLOCKED;
                blockingFields.addAll(PRODUCTION_CONSUMER_BLOCKERS);
            }
            case AUTHENTICATION_VISIBILITY_REQUIRED -> {
                outcome = FixtureOutcome.VISIBILITY_BLOCKED;
                blockingFields.add("authenticationVisibilityRequired");
                payloadWithheld = true;
            }
            case RISK_ACTION_GUARD_REQUIRED -> {
                outcome = FixtureOutcome.RISK_GUARD_BLOCKED;
                blockingFields.addAll(RISK_ACTION_GUARD_BLOCKERS);
            }
            default -> throw new IllegalStateException("Unhandled dimension " + dimension);
        }

        return new OwnershipFixture(
                field,
                dimension,
                outcome,
                "INCOMPLETE",
                "NONE",
                "REVIEW_ONLY",
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                missingFields,
                unsafeFields,
                blockingFields,
                payloadWithheld,
                null,
                null,
                null,
                null
        );
    }

    private OwnershipFixture failClosedFixture(
            OwnershipField field,
            OwnershipDimension dimension,
            FixtureOutcome outcome,
            List<String> missingFields,
            List<String> unsafeFields,
            List<String> blockingFields,
            boolean payloadWithheld
    ) {
        return new OwnershipFixture(
                field,
                dimension,
                outcome,
                "INCOMPLETE",
                "NONE",
                "REVIEW_ONLY",
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                missingFields,
                unsafeFields,
                blockingFields,
                payloadWithheld,
                null,
                null,
                null,
                null
        );
    }

    private boolean containsForbiddenMethodToken(String methodName) {
        String lowerMethodName = methodName.toLowerCase(Locale.ROOT);
        return FORBIDDEN_METHOD_TOKENS.stream()
                .map(token -> token.toLowerCase(Locale.ROOT))
                .anyMatch(lowerMethodName::contains);
    }

    private enum OwnershipField {
        COMPLETION_PATH("sourceTraceEntryOwnershipCompletionPath"),
        ENTRY_PRICE_SOURCE("entryPriceSource"),
        ENTRY_SOURCE_TYPE("entrySourceType"),
        ENTRY_SOURCE_TIMEFRAME("entrySourceTimeframe"),
        ENTRY_SOURCE_REASON("entrySourceReason"),
        ENTRY_SOURCE_REF("entrySourceRef"),
        SOURCE_WINDOW("sourceWindow"),
        RULE_METADATA("ruleIdRuleVersion"),
        FRESHNESS("freshnessOwnership"),
        CONFLICT_FAMILY("conflictFamilyOwnership");

        private final String fieldKey;

        OwnershipField(String fieldKey) {
            this.fieldKey = fieldKey;
        }

        private String fieldKey() {
            return fieldKey;
        }
    }

    private enum OwnershipDimension {
        OWNER_PRESENT,
        OWNER_MISSING,
        DUPLICATE_OWNER,
        AMBIGUOUS_OWNER,
        STALE_OWNER,
        FORBIDDEN_SUBSTITUTION,
        DOWNGRADE_REQUIRED,
        ROLLBACK_REQUIRED,
        AUDIT_REQUIRED,
        CONSUMER_ISOLATION_REQUIRED,
        AUTHENTICATION_VISIBILITY_REQUIRED,
        RISK_ACTION_GUARD_REQUIRED
    }

    private enum FixtureOutcome {
        REVIEW_ONLY,
        MISSING_REQUIRED_FIELD,
        UNSAFE_COMPLETION,
        DOWNGRADED_FAIL_CLOSED,
        ROLLED_BACK_FAIL_CLOSED,
        AUDIT_BLOCKED,
        CONSUMER_ISOLATION_BLOCKED,
        VISIBILITY_BLOCKED,
        RISK_GUARD_BLOCKED
    }

    private enum RuntimeSubstitutionSource {
        LATEST_PRICE_ONLY("latestPriceOnly"),
        RAW_KLINE_ONLY("rawKlineOnly"),
        AI_TEXT("aiText"),
        DASHBOARD_TEXT("dashboardText"),
        EXTERNAL_DATA("externalData"),
        ORDER_EXECUTION_DATA("orderExecutionData");

        private final String blocker;

        RuntimeSubstitutionSource(String blocker) {
            this.blocker = blocker;
        }

        private String blocker() {
            return blocker;
        }
    }

    private record OwnershipFixture(
            OwnershipField field,
            OwnershipDimension dimension,
            FixtureOutcome outcome,
            String completionStatus,
            String completionTransition,
            String reviewMode,
            boolean manualReviewRequired,
            boolean notTradeInstruction,
            boolean sourceTraceEntryCompleted,
            boolean completionReady,
            boolean boundaryCandidateServiceValid,
            boolean planReadinessUnlocked,
            boolean dashboardMutationAllowed,
            boolean productionWiringCreated,
            boolean serviceRegistered,
            List<String> missingFields,
            List<String> unsafeFields,
            List<String> blockingFields,
            boolean payloadWithheld,
            String generatedEntryValue,
            String generatedStopValue,
            String generatedTakeProfitValue,
            String generatedRiskRewardValue
    ) {
        private OwnershipFixture {
            missingFields = List.copyOf(missingFields);
            unsafeFields = List.copyOf(unsafeFields);
            blockingFields = List.copyOf(blockingFields);
        }
    }
}
