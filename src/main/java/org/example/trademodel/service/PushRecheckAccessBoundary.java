package org.example.trademodel.service;

import org.springframework.stereotype.Service;

@Service
public class PushRecheckAccessBoundary {

    public Decision evaluateUserRequest(Request request) {
        if (request == null || request.userId() == null || request.userId() <= 0) {
            return Decision.denied("AUTHENTICATED_USER_REQUIRED");
        }
        if (request.operation() == null) {
            return Decision.denied("PUSH_RECHECK_OPERATION_REQUIRED");
        }
        if (!"POSITION_RISK".equals(request.sourceIdentity())) {
            return Decision.denied("POSITION_RISK_SOURCE_REQUIRED");
        }
        if (request.pushId() == null || request.pushId() <= 0
                || request.messageId() == null || request.messageId().isBlank()
                || request.positionId() == null || request.positionId() <= 0) {
            return Decision.denied("EXACT_PRIVATE_IDENTITY_REQUIRED");
        }

        // Persisted Push/Recheck rows currently have no authoritative message-position-owner relation.
        return Decision.denied("AUTHORITATIVE_OWNER_RELATIONSHIP_UNAVAILABLE");
    }

    public void requireInternalScheduledExecution(RecheckExecutionCommand command) {
        if (command == null
                || !"SCHEDULED".equals(command.getTriggerSource())
                || blank(command.getDispatchBatchId())
                || blank(command.getDispatchInstructionId())
                || command.getRetryAttempt() == null
                || command.getRetryAttempt() <= 0
                || command.getMaxAttempts() == null
                || command.getMaxAttempts() <= 0
                || command.getRetryBackoffMinutes() == null
                || command.getRetryBackoffMinutes() < 0
                || command.getReplayFromLogId() != null) {
            throw new PushRecheckAccessDeniedException("USER_TRIGGER_AND_REPLAY_DISABLED");
        }
    }

    public PushRecheckAccessDeniedException disabledGlobalOperation(Operation operation) {
        return new PushRecheckAccessDeniedException(
                operation == null
                        ? "PUSH_RECHECK_GLOBAL_OPERATION_DISABLED"
                        : operation.name() + "_DISABLED");
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public enum Operation {
        READ_LATEST,
        READ_LOGS,
        READ_PREVIEW,
        READ_CONFIG,
        READ_CONFIG_AUDIT,
        READ_REPLAY_SUMMARY,
        READ_OPS,
        MUTATE_TRIGGER,
        MUTATE_REPLAY,
        MUTATE_CONFIG
    }

    public record Request(
            Long userId,
            Long pushId,
            Operation operation,
            String sourceIdentity,
            String messageId,
            Long positionId) {
    }

    public record Decision(boolean allowed, String reason) {
        private static Decision denied(String reason) {
            return new Decision(false, reason);
        }
    }

    public static class PushRecheckAccessDeniedException extends SecurityException {
        public PushRecheckAccessDeniedException(String message) {
            super(message);
        }
    }
}
