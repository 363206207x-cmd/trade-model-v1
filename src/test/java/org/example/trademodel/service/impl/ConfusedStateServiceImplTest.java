package org.example.trademodel.service.impl;

import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.service.ConfusedResult;
import org.example.trademodel.service.ConfusedStatePolicy;
import org.example.trademodel.service.DecisionContext;
import org.example.trademodel.service.OpportunityStateIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfusedStateServiceImplTest {

    @Mock
    private AssetStateMapper assetStateMapper;

    private ConfusedStateServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ConfusedStateServiceImpl(assetStateMapper);
    }

    @Test
    void confusedScore0_isNeitherConfusedNorDirectionallyBlocked() {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(null);

        ConfusedResult result = service.calculateConfused("BTCUSDT", contextForScore(0, false));

        assertThat(result.isShouldEnter()).isFalse();
        assertThat(result.isDirectionalPushBlocked()).isFalse();
    }

    @Test
    void confusedScore1_isConflictEvidenceButNotAStateOrDirectionalBlock() {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(null);

        ConfusedResult result = service.calculateConfused("BTCUSDT", contextForScore(1, false));

        assertThat(result.getConfusedScore()).isEqualTo(1);
        assertThat(result.isShouldEnter()).isFalse();
        assertThat(result.isDirectionalPushBlocked()).isFalse();
    }

    @Test
    void confusedScore69_doesNotEnterConfused() {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(null);

        ConfusedResult result = service.calculateConfused("BTCUSDT", contextForScore(69, false));

        assertThat(result.getConfusedScore()).isEqualTo(69);
        assertThat(result.isShouldEnter()).isFalse();
        assertThat(result.getNextState()).isEqualTo(AssetStateEnum.OBSERVING.name());
        assertThat(result.isDirectionalPushBlocked()).isFalse();
    }

    @Test
    void confusedScore70_entersConfusedAndResetsStreak() {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(null);

        ConfusedResult result = service.calculateConfused("BTCUSDT", contextForScore(70, true));

        assertThat(result.getConfusedScore()).isEqualTo(70);
        assertThat(result.isShouldEnter()).isTrue();
        assertThat(result.getNextState()).isEqualTo(AssetStateEnum.CONFUSED.name());
        assertThat(result.getConfusedLowStreak()).isZero();
    }

    @Test
    void confusedScore84_entersConfusedButDoesNotBlockDirectionalPush() {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(null);

        ConfusedResult result = service.calculateConfused("BTCUSDT", contextForScore(84, true));

        assertThat(result.getNextState()).isEqualTo(AssetStateEnum.CONFUSED.name());
        assertThat(result.isDirectionalPushBlocked()).isFalse();
    }

    @Test
    void confusedScore85_entersConfusedAndBlocksDirectionalPush() {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(null);

        ConfusedResult result = service.calculateConfused("BTCUSDT", contextForScore(85, true));

        assertThat(result.getNextState()).isEqualTo(AssetStateEnum.CONFUSED.name());
        assertThat(result.isDirectionalPushBlocked()).isTrue();
    }

    @Test
    void firstLowCycleAfterConfusedKeepsConfusedWithStreakOne() {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(row(AssetStateEnum.CONFUSED, 0));

        ConfusedResult result = service.calculateConfused("BTCUSDT", contextForScore(54, true));

        assertThat(result.getNextState()).isEqualTo(AssetStateEnum.CONFUSED.name());
        assertThat(result.isShouldExit()).isFalse();
        assertThat(result.getConfusedLowStreak()).isEqualTo(1);
    }

    @Test
    void secondConsecutiveLowCycleWithRecoveredSignalsExitsToCandidateOnly() {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(row(AssetStateEnum.CONFUSED, 1));

        ConfusedResult result = service.calculateConfused("BTCUSDT", contextForScore(54, true));

        assertThat(result.isShouldExit()).isTrue();
        assertThat(result.getNextState()).isEqualTo(AssetStateEnum.CANDIDATE.name());
        assertThat(result.getNextState()).isNotEqualTo(AssetStateEnum.TRIGGERED.name());
        assertThat(result.getNextState()).isNotEqualTo(AssetStateEnum.WAITING_TRIGGER.name());
        assertThat(result.getConfusedLowStreak()).isZero();
    }

    @Test
    void secondLowCycleWithoutRecoveredDriverSignalsRemainsConfused() {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(row(AssetStateEnum.CONFUSED, 1));
        DecisionContext context = contextForScore(54, true);
        context.setMultiTimeframeAligned(false);

        ConfusedResult result = service.calculateConfused("BTCUSDT", context);

        assertThat(result.isShouldExit()).isFalse();
        assertThat(result.getNextState()).isEqualTo(AssetStateEnum.CONFUSED.name());
        assertThat(result.getTransitionReason()).isEqualTo("CONFUSED_EXIT_SIGNALS_NOT_RECOVERED");
    }

    @Test
    void recoveredSignalsWithoutOpeningValueExitToObserving() {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(row(AssetStateEnum.CONFUSED, 1));

        ConfusedResult result = service.calculateConfused("BTCUSDT", contextForScore(54, false));

        assertThat(result.isShouldExit()).isTrue();
        assertThat(result.getNextState()).isEqualTo(AssetStateEnum.OBSERVING.name());
    }

    @Test
    void confusedScore55DoesNotExitAndResetsLowStreak() {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(row(AssetStateEnum.CONFUSED, 1));

        ConfusedResult result = service.calculateConfused("BTCUSDT", contextForScore(55, true));

        assertThat(result.getNextState()).isEqualTo(AssetStateEnum.CONFUSED.name());
        assertThat(result.isShouldExit()).isFalse();
        assertThat(result.getConfusedLowStreak()).isZero();
    }

    @Test
    void interruptedLowCyclesDoNotExit() {
        when(assetStateMapper.selectBySymbol("BTCUSDT"))
                .thenReturn(row(AssetStateEnum.CONFUSED, 1))
                .thenReturn(row(AssetStateEnum.CONFUSED, 0));

        ConfusedResult reset = service.calculateConfused("BTCUSDT", contextForScore(55, true));
        ConfusedResult lowAgain = service.calculateConfused("BTCUSDT", contextForScore(54, true));

        assertThat(reset.getConfusedLowStreak()).isZero();
        assertThat(lowAgain.getNextState()).isEqualTo(AssetStateEnum.CONFUSED.name());
        assertThat(lowAgain.isShouldExit()).isFalse();
        assertThat(lowAgain.getConfusedLowStreak()).isEqualTo(1);
    }

    @Test
    void enteringConfusedResetsExistingLowStreak() {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(row(AssetStateEnum.CONFUSED, 1));

        ConfusedResult result = service.calculateConfused("BTCUSDT", contextForScore(70, true));

        assertThat(result.getNextState()).isEqualTo(AssetStateEnum.CONFUSED.name());
        assertThat(result.getConfusedLowStreak()).isZero();
    }

    @Test
    void persistedStreakSurvivesAcrossServiceInvocations() {
        when(assetStateMapper.selectBySymbol("BTCUSDT"))
                .thenReturn(row(AssetStateEnum.CONFUSED, 0))
                .thenReturn(row(AssetStateEnum.CONFUSED, 1));

        ConfusedResult first = service.calculateConfused("BTCUSDT", contextForScore(54, true));
        ConfusedResult second = service.calculateConfused("BTCUSDT", contextForScore(54, true));

        assertThat(first.getConfusedLowStreak()).isEqualTo(1);
        assertThat(second.getNextState()).isEqualTo(AssetStateEnum.CANDIDATE.name());
    }

    @Test
    void differentSymbolsReadIndependentPersistedRows() {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(row(AssetStateEnum.CONFUSED, 1));
        when(assetStateMapper.selectBySymbol("ETHUSDT")).thenReturn(row(AssetStateEnum.CONFUSED, 0));

        ConfusedResult btc = service.calculateConfused("BTCUSDT", contextForScore(54, true));
        ConfusedResult eth = service.calculateConfused("ETHUSDT", contextForScore(54, true));

        assertThat(btc.getNextState()).isEqualTo(AssetStateEnum.CANDIDATE.name());
        assertThat(eth.getNextState()).isEqualTo(AssetStateEnum.CONFUSED.name());
        verify(assetStateMapper).selectBySymbol("BTCUSDT");
        verify(assetStateMapper).selectBySymbol("ETHUSDT");
    }

    @Test
    void userOwnedOpportunityReadsOnlyItsExactOwnerAndTimeframeIdentity() {
        OpportunityStateIdentity identity = new OpportunityStateIdentity(
                "USER", 42L, 9001L, "BTCUSDT", "5m");
        when(assetStateMapper.selectByIdentity("USER", 42L, "BTCUSDT", "5m"))
                .thenReturn(row(AssetStateEnum.CONFUSED, 1));

        ConfusedResult result = service.calculateConfused(identity, contextForScore(54, true));

        assertThat(result.isShouldExit()).isTrue();
        assertThat(result.getNextState()).isEqualTo(AssetStateEnum.CANDIDATE.name());
        verify(assetStateMapper).selectByIdentity("USER", 42L, "BTCUSDT", "5m");
    }

    @Test
    void assetStateReadFailureFailsClosedToConfused() {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenThrow(new IllegalStateException("db down"));

        ConfusedResult result = service.calculateConfused("BTCUSDT", contextForScore(10, true));

        assertThat(result.getNextState()).isEqualTo(AssetStateEnum.CONFUSED.name());
        assertThat(result.getConfusedScore()).isEqualTo(ConfusedStatePolicy.CONFUSED_ENTER_THRESHOLD);
        assertThat(result.getTransitionReason()).isEqualTo("ASSET_STATE_READ_FAILED_FAIL_CLOSED");
    }

    @Test
    void resultSafetyFieldsAreAlwaysReviewOnly() {
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(null);

        ConfusedResult result = service.calculateConfused("BTCUSDT", contextForScore(85, true));

        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isManualReviewOnly()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isNotExecutable()).isTrue();
        assertThat(result.isNotAutoTrading()).isTrue();
        assertThat(result.isNotOrderExecution()).isTrue();
        assertThat(result.isNotStateMachineExecution()).isTrue();
    }

    private static DecisionContext contextForScore(int score, boolean worthOpening) {
        DecisionContext context = new DecisionContext();
        context.setDriverConflictScore(score);
        context.setExecutionInstabilityScore(score);
        context.setMicrostructureTrapScore(score);
        context.setCauseEffectDivergenceScore(score);
        context.setAiConflictScore(score);
        context.setWorthOpening(worthOpening);
        context.setMultiTimeframeAligned(true);
        return context;
    }

    private static AssetStateDO row(AssetStateEnum state, int lowStreak) {
        AssetStateDO row = new AssetStateDO();
        row.setState(state);
        row.setConfusedLowStreak(lowStreak);
        return row;
    }
}
