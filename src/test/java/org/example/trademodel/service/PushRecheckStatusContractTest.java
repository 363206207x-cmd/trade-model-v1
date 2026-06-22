package org.example.trademodel.service;

import org.example.trademodel.enums.RecheckStatusEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PushRecheckStatusContractTest {

    @Test
    void toPushStatus_shouldMatchCanonicalMapping() {
        assertThat(PushRecheckStatusContract.toPushStatus(RecheckStatusEnum.REVIEW_PASSED))
                .isEqualTo("RECHECK_REVIEW_PASSED");
        assertThat(PushRecheckStatusContract.toPushStatus(RecheckStatusEnum.REVIEW_WAITING))
                .isEqualTo("RECHECK_REVIEW_WAITING");
        assertThat(PushRecheckStatusContract.toPushStatus(RecheckStatusEnum.DRIFTED_FROM_ENTRY_ZONE))
                .isEqualTo("RECHECK_DRIFTED_FROM_ENTRY_ZONE");
        assertThat(PushRecheckStatusContract.toPushStatus(RecheckStatusEnum.EXPIRED))
                .isEqualTo("RECHECK_EXPIRED");
    }

    @Test
    void toReviewTag_shouldRespectStepOneCategories() {
        assertThat(PushRecheckStatusContract.toReviewTag(RecheckStatusEnum.REVIEW_PASSED))
                .isEqualTo(PushRecheckStatusContract.ReviewTag.PASS);
        assertThat(PushRecheckStatusContract.toReviewTag(RecheckStatusEnum.REVIEW_WAITING))
                .isEqualTo(PushRecheckStatusContract.ReviewTag.WAITING);
        assertThat(PushRecheckStatusContract.toReviewTag(RecheckStatusEnum.RISK_BLOCKED))
                .isEqualTo(PushRecheckStatusContract.ReviewTag.BLOCKED);
        assertThat(PushRecheckStatusContract.toReviewTag(RecheckStatusEnum.EXPIRED))
                .isEqualTo(PushRecheckStatusContract.ReviewTag.TERMINATED);
    }

    @Test
    void toReviewTagByPushStatus_shouldMapCapturedAsWaiting() {
        assertThat(PushRecheckStatusContract.toReviewTagByPushStatus("CAPTURED"))
                .isEqualTo(PushRecheckStatusContract.ReviewTag.WAITING);
        assertThat(PushRecheckStatusContract.toReviewTagByPushStatus("RECHECK_INVALIDATED"))
                .isEqualTo(PushRecheckStatusContract.ReviewTag.BLOCKED);
    }

    @Test
    void legacyStatuses_shouldReadAsCanonicalReviewOnlyStatuses() {
        assertThat(PushRecheckStatusContract.tryParseRecheckStatus("VALID_EXECUTABLE"))
                .isEqualTo(RecheckStatusEnum.REVIEW_PASSED);
        assertThat(PushRecheckStatusContract.tryParseRecheckStatus("VALID_WAITING"))
                .isEqualTo(RecheckStatusEnum.REVIEW_WAITING);
        assertThat(PushRecheckStatusContract.canonicalizeRecheckStatusName("DRIFTED"))
                .isEqualTo("DRIFTED_FROM_ENTRY_ZONE");
        assertThat(PushRecheckStatusContract.canonicalizePushStatus("RECHECK_VALID_EXECUTABLE"))
                .isEqualTo("RECHECK_REVIEW_PASSED");
        assertThat(PushRecheckStatusContract.canonicalizePushStatus("RECHECK_VALID_WAITING"))
                .isEqualTo("RECHECK_REVIEW_WAITING");
        assertThat(PushRecheckStatusContract.canonicalizePushStatus("RECHECK_DRIFTED"))
                .isEqualTo("RECHECK_DRIFTED_FROM_ENTRY_ZONE");
    }

    @Test
    void schedulerPendingStatuses_shouldAcceptNewAndLegacyWaitingOnly() {
        assertThat(PushRecheckStatusContract.isPendingPushStatusForScheduler("CAPTURED")).isTrue();
        assertThat(PushRecheckStatusContract.isPendingPushStatusForScheduler("RECHECK_REVIEW_WAITING")).isTrue();
        assertThat(PushRecheckStatusContract.isPendingPushStatusForScheduler("RECHECK_VALID_WAITING")).isTrue();
        assertThat(PushRecheckStatusContract.isPendingPushStatusForScheduler("RECHECK_REVIEW_PASSED")).isFalse();
        assertThat(PushRecheckStatusContract.isPendingPushStatusForScheduler("RECHECK_VALID_EXECUTABLE")).isFalse();
    }
}
