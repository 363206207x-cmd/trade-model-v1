package org.example.trademodel.position;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class SimulatedPositionProvider implements PositionProvider {

    @Override
    public PositionProviderResult fetchOpenPositions() {
        return new PositionProviderResult(
                "SIMULATED",
                "simulated-provider-disabled",
                List.of(),
                "SIMULATED",
                false,
                "SIMULATED_POSITION_SOURCE_DISABLED_BY_PRODUCT_CONTRACT",
                false
        );
    }
}
