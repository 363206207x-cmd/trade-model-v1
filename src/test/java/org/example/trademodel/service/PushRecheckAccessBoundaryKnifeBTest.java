package org.example.trademodel.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PushRecheckAccessBoundaryKnifeBTest {
    private final PushRecheckAccessBoundary boundary = new PushRecheckAccessBoundary();

    @Test
    void ownerScopedPushOpenUsesNoSchedulerIdentityAndManualRemainsDenied() {
        RecheckExecutionCommand command = RecheckExecutionCommand.pushOpen(1, null);

        boundary.requireOwnerScopedPushOpenExecution(command);

        assertThat(command.getTriggerSource()).isEqualTo("PUSH_OPEN");
        assertThat(command.getDispatchBatchId()).isNull();
        assertThat(command.getDispatchInstructionId()).isNull();
        assertThat(command.getMaxAttempts()).isNull();
        assertThat(command.getRetryBackoffMinutes()).isNull();
        assertThat(boundary.evaluateUserRequest(new PushRecheckAccessBoundary.Request(
                7L, 11L, PushRecheckAccessBoundary.Operation.MUTATE_TRIGGER,
                "POSITION_RISK", "message-1", 17L)).allowed()).isFalse();
        assertThatThrownBy(() -> boundary.requireOwnerScopedPushOpenExecution(
                RecheckExecutionCommand.manual()))
                .isInstanceOf(PushRecheckAccessBoundary.PushRecheckAccessDeniedException.class);
    }

    @Test
    void retryKeepsPushOpenIdentityAndRejectsDisguisedScheduledMetadata() {
        RecheckExecutionCommand retry = RecheckExecutionCommand.pushOpen(2, 91L);
        boundary.requireOwnerScopedPushOpenExecution(retry);

        retry.setDispatchBatchId("fake-batch");
        assertThatThrownBy(() -> boundary.requireOwnerScopedPushOpenExecution(retry))
                .isInstanceOf(PushRecheckAccessBoundary.PushRecheckAccessDeniedException.class);
    }
}
