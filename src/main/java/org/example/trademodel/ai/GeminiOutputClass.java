package org.example.trademodel.ai;

public enum GeminiOutputClass {
    EMPTY_TEXT,
    EMPTY_JSON_OBJECT,
    EMPTY_JSON_ARRAY,
    VALID_JSON_OBJECT,
    VALID_JSON_ARRAY,
    PLAIN_TEXT_SHORT,
    MARKDOWN_WRAPPER,
    REFUSAL_PATTERN,
    MALFORMED_JSON
}
