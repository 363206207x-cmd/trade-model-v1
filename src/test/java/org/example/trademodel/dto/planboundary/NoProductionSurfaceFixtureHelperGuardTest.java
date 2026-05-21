package org.example.trademodel.dto.planboundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.planboundary.BoundaryCandidateFixtureAssemblerHelper.AssemblerStatus;
import org.example.trademodel.dto.planboundary.BoundaryCandidateFixtureAssemblerHelper.BoundaryCandidateFixture;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.EntryFamily;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.EntryFixture;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.RrFixture;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.StopFixture;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.TpFamily;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.TpFixture;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class NoProductionSurfaceFixtureHelperGuardTest {

    private static final List<Path> HELPER_SOURCE_PATHS = List.of(
            Path.of("src/test/java/org/example/trademodel/dto/planboundary/EntrySourceOwnedCandidateFixtureHelper.java"),
            Path.of("src/test/java/org/example/trademodel/dto/planboundary/StopTpRrSourceOwnedCandidateFixtureHelper.java"),
            Path.of("src/test/java/org/example/trademodel/dto/planboundary/BoundaryCandidateFixtureAssemblerHelper.java")
    );
    private static final List<String> RUNTIME_LIVE_EXTERNAL_TERMS = List.of(
            "runtimeData",
            "liveMarket",
            "externalFetch",
            "exchangeClient",
            "binance",
            "okx",
            "coinglass",
            "restTemplate",
            "webClient"
    );
    private static final List<String> FORBIDDEN_SURFACE_TERMS = List.of(
            "tradeReady",
            "readyToTrade",
            "order",
            "execution",
            "automation",
            "autoTrading",
            "autoTrade",
            "open",
            "close",
            "reverse",
            "signal",
            "buy",
            "sell"
    );
    private static final List<Class<? extends Annotation>> SPRING_ANNOTATIONS = List.of(
            Service.class,
            Component.class,
            Repository.class,
            Controller.class,
            RestController.class,
            Configuration.class
    );
    private static final List<Class<? extends Annotation>> ENDPOINT_ANNOTATIONS = List.of(
            RequestMapping.class,
            GetMapping.class,
            PostMapping.class,
            PutMapping.class,
            DeleteMapping.class,
            PatchMapping.class
    );

    @Test
    void fixtureHelperSourcesDoNotCallProductionFactoriesOrRuntimeApis() throws Exception {
        for (Path path : HELPER_SOURCE_PATHS) {
            String source = Files.readString(path);
            String normalizedSource = source.toLowerCase(Locale.ROOT);

            assertThat(source).doesNotContain("BoundaryCandidateDTO.valid(");
            assertThat(source).doesNotContain("BoundaryStatusEnum.VALID");
            for (String term : RUNTIME_LIVE_EXTERNAL_TERMS) {
                assertThat(normalizedSource).doesNotContain(term.toLowerCase(Locale.ROOT));
            }
        }
    }

    @Test
    void fixtureHelpersDoNotExposeProductionDtoOrRealValueTypes() {
        for (Class<?> type : guardedTypes()) {
            assertThat(returnTypesOf(type)).doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
            assertThat(parameterTypesOf(type)).doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
            assertThat(fieldTypesOf(type)).doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
        }
    }

    @Test
    void fixtureHelpersHaveNoSpringOrEndpointAnnotations() {
        for (Class<?> type : guardedTypes()) {
            for (Class<? extends Annotation> annotation : SPRING_ANNOTATIONS) {
                assertThat(type.getAnnotation(annotation)).isNull();
            }
            for (Class<? extends Annotation> annotation : ENDPOINT_ANNOTATIONS) {
                assertThat(type.getAnnotation(annotation)).isNull();
            }
        }
    }

    @Test
    void fixtureOutputSurfacesDoNotExposeTradingActionNames() {
        for (Class<?> type : guardedOutputTypes()) {
            assertThat(publicSurfaceOf(type)).noneMatch(this::containsForbiddenSurfaceTerm);
        }
    }

    @Test
    void fixtureOutputsRemainReviewOnlyForValidIncompleteAndBlockedResults() {
        EntryFixture entry = validEntry();
        StopFixture stop = validStop();
        TpFixture tp = validTp();
        RrFixture rr = validRr(entry, stop, tp);
        BoundaryCandidateFixture validOutput = BoundaryCandidateFixtureAssemblerHelper.assemble(entry, stop, tp, rr);

        EntryFixture incompleteEntry = EntrySourceOwnedCandidateFixtureHelper
                .forFamily(EntryFamily.STRUCTURE_CONFIRMATION_ZONE)
                .missingSourceOwner()
                .build();
        BoundaryCandidateFixture incompleteOutput = BoundaryCandidateFixtureAssemblerHelper.assemble(
                incompleteEntry,
                stop,
                tp,
                rr
        );

        RrFixture blockedRr = StopTpRrSourceOwnedCandidateFixtureHelper
                .rr(entry, stop, tp)
                .entryTpDirectionConflict()
                .build();
        BoundaryCandidateFixture blockedOutput = BoundaryCandidateFixtureAssemblerHelper.assemble(
                entry,
                stop,
                tp,
                blockedRr
        );

        assertFixtureOutput(validOutput, AssemblerStatus.FIXTURE_VALID_CANDIDATE);
        assertFixtureOutput(incompleteOutput, AssemblerStatus.INCOMPLETE);
        assertFixtureOutput(blockedOutput, AssemblerStatus.BLOCKED);

        assertEntryFixture(entry);
        assertStopFixture(stop);
        assertTpFixture(tp);
        assertRrFixture(rr);
    }

    private void assertFixtureOutput(BoundaryCandidateFixture output, AssemblerStatus expectedStatus) {
        assertThat(output.assemblerStatus()).isEqualTo(expectedStatus);
        assertThat(output.assemblerStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
        assertThat(output.manualReviewRequired()).isTrue();
        assertThat(output.notTradeInstruction()).isTrue();
        assertThat(output.reviewMode()).isEqualTo(BoundaryCandidateFixtureAssemblerHelper.REVIEW_ONLY);
    }

    private void assertEntryFixture(EntryFixture fixture) {
        assertThat(fixture.manualReviewRequired()).isTrue();
        assertThat(fixture.notTradeInstruction()).isTrue();
        assertThat(fixture.reviewMode()).isEqualTo(EntrySourceOwnedCandidateFixtureHelper.REVIEW_ONLY);
        assertThat(fixture.fixtureStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
    }

    private void assertStopFixture(StopFixture fixture) {
        assertThat(fixture.manualReviewRequired()).isTrue();
        assertThat(fixture.notTradeInstruction()).isTrue();
        assertThat(fixture.reviewMode()).isEqualTo(StopTpRrSourceOwnedCandidateFixtureHelper.REVIEW_ONLY);
        assertThat(fixture.fixtureStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
    }

    private void assertTpFixture(TpFixture fixture) {
        assertThat(fixture.manualReviewRequired()).isTrue();
        assertThat(fixture.notTradeInstruction()).isTrue();
        assertThat(fixture.reviewMode()).isEqualTo(StopTpRrSourceOwnedCandidateFixtureHelper.REVIEW_ONLY);
        assertThat(fixture.fixtureStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
    }

    private void assertRrFixture(RrFixture fixture) {
        assertThat(fixture.manualReviewRequired()).isTrue();
        assertThat(fixture.notTradeInstruction()).isTrue();
        assertThat(fixture.reviewMode()).isEqualTo(StopTpRrSourceOwnedCandidateFixtureHelper.REVIEW_ONLY);
        assertThat(fixture.fixtureStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
    }

    private EntryFixture validEntry() {
        return EntrySourceOwnedCandidateFixtureHelper.completeFixture(EntryFamily.STRUCTURE_CONFIRMATION_ZONE);
    }

    private StopFixture validStop() {
        return StopTpRrSourceOwnedCandidateFixtureHelper.completeStopFixture();
    }

    private TpFixture validTp() {
        return StopTpRrSourceOwnedCandidateFixtureHelper.completeTpFixture(TpFamily.STRUCTURE_TARGET);
    }

    private RrFixture validRr(EntryFixture entry, StopFixture stop, TpFixture tp) {
        return StopTpRrSourceOwnedCandidateFixtureHelper.rr(entry, stop, tp).build();
    }

    private List<Class<?>> guardedTypes() {
        List<Class<?>> types = new ArrayList<>();
        addWithNested(types, EntrySourceOwnedCandidateFixtureHelper.class);
        addWithNested(types, StopTpRrSourceOwnedCandidateFixtureHelper.class);
        addWithNested(types, BoundaryCandidateFixtureAssemblerHelper.class);
        return types;
    }

    private List<Class<?>> guardedOutputTypes() {
        return List.of(
                EntrySourceOwnedCandidateFixtureHelper.EntryFixture.class,
                EntrySourceOwnedCandidateFixtureHelper.NumericSource.class,
                EntrySourceOwnedCandidateFixtureHelper.SourceWindow.class,
                StopTpRrSourceOwnedCandidateFixtureHelper.StopFixture.class,
                StopTpRrSourceOwnedCandidateFixtureHelper.TpFixture.class,
                StopTpRrSourceOwnedCandidateFixtureHelper.RrFixture.class,
                StopTpRrSourceOwnedCandidateFixtureHelper.NumericSource.class,
                StopTpRrSourceOwnedCandidateFixtureHelper.SourceWindow.class,
                BoundaryCandidateFixtureAssemblerHelper.BoundaryCandidateFixture.class,
                BoundaryCandidateFixtureAssemblerHelper.ReviewField.class,
                BoundaryCandidateFixtureAssemblerHelper.SourceOwnerSummary.class,
                BoundaryCandidateFixtureAssemblerHelper.SourceFamilySummary.class,
                BoundaryCandidateFixtureAssemblerHelper.NumericSourceTokenSummary.class
        );
    }

    private void addWithNested(List<Class<?>> types, Class<?> type) {
        types.add(type);
        for (Class<?> nestedType : type.getDeclaredClasses()) {
            addWithNested(types, nestedType);
        }
    }

    private List<String> publicSurfaceOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .map(Method::getName)
                .toList();
    }

    private List<Class<?>> returnTypesOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .map(Method::getReturnType)
                .toList();
    }

    private List<Class<?>> parameterTypesOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getParameterTypes()))
                .toList();
    }

    private List<Class<?>> fieldTypesOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(Field::getType)
                .toList();
    }

    private boolean containsForbiddenSurfaceTerm(String surfaceName) {
        String normalizedSurfaceName = surfaceName.toLowerCase(Locale.ROOT);
        return FORBIDDEN_SURFACE_TERMS.stream()
                .map(term -> term.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedSurfaceName::contains);
    }
}
