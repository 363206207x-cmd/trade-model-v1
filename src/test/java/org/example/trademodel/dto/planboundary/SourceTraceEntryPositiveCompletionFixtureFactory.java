package org.example.trademodel.dto.planboundary;

final class SourceTraceEntryPositiveCompletionFixtureFactory {

    private final SourceTraceEntryPositiveCompletionFixtureMapper mapper =
            new SourceTraceEntryPositiveCompletionFixtureMapper();

    SourceTraceEntryPositiveCompletionContractDTO defaultFixture() {
        return new SourceTraceEntryPositiveCompletionContractDTO();
    }

    SourceTraceEntryPositiveCompletionContractDTO syntheticFixture(
            SourceTraceEntryPositiveCompletionFixtureInput fixtureInput
    ) {
        return mapper.fromFixture(fixtureInput);
    }

    SourceTraceEntryPositiveCompletionContractDTO syntheticEvidenceFixture(
            SourceTraceEntryPositiveCompletionFixtureInput fixtureInput
    ) {
        return mapper.fromFixture(fixtureInput);
    }
}
