package org.example.trademodel.service;

import org.example.trademodel.vo.MissedReasonViewVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("smoke")
class MissedReasonViewParserTest {

    @Test
    void parse_emptyReasonJson_returnsEmptyStatus() {
        MissedReasonViewVO vo = MissedReasonViewParser.parse(" ");

        assertThat(vo.getParseStatus()).isEqualTo("EMPTY_REASON_JSON");
        assertThat(vo.getFacts()).isEmpty();
        assertThat(vo.getRefs()).isEmpty();
    }

    @Test
    void parse_validJson_extractsMainFields() {
        String reasonJson = """
                {
                  "version": "1",
                  "rule": "WORTH_OPENING_NO_OPEN_POSITION",
                  "whyMissed": "not executed",
                  "facts": {"hotResetWouldFire": false, "confusedScore": 11},
                  "refs": {"analysisId": "a-1", "decisionId": "d-1"}
                }
                """;

        MissedReasonViewVO vo = MissedReasonViewParser.parse(reasonJson);

        assertThat(vo.getParseStatus()).isEqualTo("OK");
        assertThat(vo.getVersion()).isEqualTo("1");
        assertThat(vo.getRule()).isEqualTo("WORTH_OPENING_NO_OPEN_POSITION");
        assertThat(vo.getFacts()).containsEntry("hotResetWouldFire", false);
        assertThat(vo.getRefs()).containsEntry("analysisId", "a-1");
    }

    @Test
    void parse_invalidJson_returnsParseFailed() {
        MissedReasonViewVO vo = MissedReasonViewParser.parse("{bad-json}");

        assertThat(vo.getParseStatus()).isEqualTo("PARSE_FAILED");
        assertThat(vo.getFacts()).isEmpty();
        assertThat(vo.getRefs()).isEmpty();
    }
}
