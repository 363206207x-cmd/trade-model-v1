package org.example.trademodel.service;

import org.example.trademodel.enums.RecheckStatusEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PushRecheckStatusContractTest {

    @Test
    void toPushStatus_shouldMatchCanonicalMapping() {
        assertThat(PushRecheckStatusContract.toPushStatus(RecheckStatusEnum.VALID_EXECUTABLE))
                .isEqualTo("RECHECK_VALID_EXECUTABLE");
        assertThat(PushRecheckStatusContract.toPushStatus(RecheckStatusEnum.VALID_WAITING))
                .isEqualTo("RECHECK_VALID_WAITING");
        assertThat(PushRecheckStatusContract.toPushStatus(RecheckStatusEnum.EXPIRED))
                .isEqualTo("RECHECK_EXPIRED");
    }

    @Test
    void toReviewTag_shouldRespectStepOneCategories() {
        assertThat(PushRecheckStatusContract.toReviewTag(RecheckStatusEnum.VALID_EXECUTABLE))
                .isEqualTo(PushRecheckStatusContract.ReviewTag.PASS);
        assertThat(PushRecheckStatusContract.toReviewTag(RecheckStatusEnum.VALID_WAITING))
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
}
