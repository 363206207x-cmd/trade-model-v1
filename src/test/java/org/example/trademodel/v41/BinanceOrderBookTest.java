package org.example.trademodel.v41;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BinanceOrderBookTest {

    @Test
    void replaysBufferedDeltaAfterSnapshotAndCalculatesEffectiveDepth() {
        BinanceOrderBook book = new BinanceOrderBook();
        assertThat(book.apply(delta(101, 102, 100,
                levels("100", "2", "99.95", "3"), levels("100.10", "4"))))
                .isEqualTo(BinanceOrderBook.ApplyResult.BUFFERED);

        assertThat(book.applySnapshot(100, levels("99.90", "1"), levels("100.20", "1")))
                .isEqualTo(BinanceOrderBook.ApplyResult.APPLIED);
        BinanceOrderBook.Snapshot snapshot = book.snapshot(1_000L);

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.bid()).isEqualByComparingTo("100");
        assertThat(snapshot.ask()).isEqualByComparingTo("100.10");
        assertThat(snapshot.bidDepth10Bps()).isEqualByComparingTo("599.75");
        assertThat(snapshot.askDepth10Bps()).isEqualByComparingTo("500.60");
        assertThat(snapshot.sequence()).isEqualTo(102L);
    }

    @Test
    void rejectsDuplicateAndInvalidatesOnSequenceGapUntilNewSnapshot() {
        BinanceOrderBook book = new BinanceOrderBook();
        assertThat(book.applySnapshot(200, levels("100", "1"), levels("101", "1")))
                .isEqualTo(BinanceOrderBook.ApplyResult.APPLIED);
        assertThat(book.apply(delta(201, 201, 200, levels("100", "2"), List.of())))
                .isEqualTo(BinanceOrderBook.ApplyResult.APPLIED);
        assertThat(book.apply(delta(201, 201, 200, levels("100", "9"), List.of())))
                .isEqualTo(BinanceOrderBook.ApplyResult.IGNORED);

        assertThat(book.apply(delta(203, 203, 202, levels("100", "3"), List.of())))
                .isEqualTo(BinanceOrderBook.ApplyResult.GAP);
        assertThat(book.initialized()).isFalse();
        assertThat(book.snapshot(2_000L)).isNull();
    }

    private static BinanceOrderBook.DepthDelta delta(long first, long last, long previous,
                                                       List<BinanceOrderBook.Level> bids,
                                                       List<BinanceOrderBook.Level> asks) {
        return new BinanceOrderBook.DepthDelta(first, last, previous, bids, asks);
    }

    private static List<BinanceOrderBook.Level> levels(String... values) {
        java.util.ArrayList<BinanceOrderBook.Level> result = new java.util.ArrayList<>();
        for (int index = 0; index < values.length; index += 2) {
            result.add(new BinanceOrderBook.Level(new BigDecimal(values[index]),
                    new BigDecimal(values[index + 1])));
        }
        return List.copyOf(result);
    }
}
