package org.example.trademodel.market.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OkxMarketKlineClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseOkxCandlesJson_mapsToBinanceStyleRowsOldestFirst() throws Exception {
        String json = """
                {"code":"0","msg":"","data":[
                  ["1700000060000","101","103","100","102","2","200","204","1"],
                  ["1700000000000","100","102","99","101","1","100","101","1"]
                ]}
                """;

        List<String[]> rows = OkxMarketKlineClient.parseOkxCandlesJson(objectMapper.readTree(json), "1m", 3);

        assertEquals(2, rows.size());
        assertEquals("1700000000000", rows.get(0)[0]);
        assertEquals("100", rows.get(0)[1]);
        assertEquals("102", rows.get(0)[2]);
        assertEquals("99", rows.get(0)[3]);
        assertEquals("101", rows.get(0)[4]);
        assertEquals("1", rows.get(0)[5]);
        assertEquals("1700000059999", rows.get(0)[6]);
        assertEquals("1700000060000", rows.get(1)[0]);
    }

    @Test
    void parseOkxCandlesJson_emptyWhenCodeFails() throws Exception {
        String json = "{\"code\":\"50011\",\"data\":[]}";

        List<String[]> rows = OkxMarketKlineClient.parseOkxCandlesJson(objectMapper.readTree(json), "1m", 3);

        assertEquals(0, rows.size());
    }

    @Test
    void intervalMillis_supportsMinuteAndHour() {
        assertEquals(60_000L, OkxMarketKlineClient.intervalMillis("1m"));
        assertEquals(300_000L, OkxMarketKlineClient.intervalMillis("5m"));
        assertEquals(3_600_000L, OkxMarketKlineClient.intervalMillis("1H"));
    }
}
