package org.example.trademodel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import org.example.trademodel.dto.planboundary.EntryOwnershipRequest;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceOwnershipResult;
import org.junit.jupiter.api.Test;

class SourceTraceEntryOwnershipAdapterTest {

    @Test
    void interfaceExposesEntryOwnershipResolutionOnly() throws Exception {
        Method method = SourceTraceEntryOwnershipAdapter.class.getMethod(
                "resolveEntryOwnership",
                EntryOwnershipRequest.class
        );

        assertThat(method.getReturnType()).isEqualTo(SourceTraceEntrySourceOwnershipResult.class);
        assertThat(SourceTraceEntryOwnershipAdapter.class.getDeclaredMethods())
                .extracting(Method::getName)
                .containsExactly("resolveEntryOwnership");
    }

    @Test
    void interfaceDoesNotExposeOrderExecutionCloseReverseOrAutoTradingMethodNames() {
        assertThat(Arrays.stream(SourceTraceEntryOwnershipAdapter.class.getDeclaredMethods())
                        .map(Method::getName)
                        .map(name -> name.toLowerCase(Locale.ROOT)))
                .allSatisfy(name -> {
                    assertThat(name).doesNotContain("order");
                    assertThat(name).doesNotContain("execution");
                    assertThat(name).doesNotContain("execute");
                    assertThat(name).doesNotContain("close");
                    assertThat(name).doesNotContain("reverse");
                    assertThat(name).doesNotContain("autotrading");
                    assertThat(name).doesNotContain("auto");
                });
    }

    @Test
    void productionAdapterImplementationIsNotRequiredInSkeletonPack() {
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryOwnershipAdapter"
                ))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
