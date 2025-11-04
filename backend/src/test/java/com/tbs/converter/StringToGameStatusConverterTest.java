package com.tbs.converter;

import com.tbs.enums.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StringToGameStatusConverterTest {

    private StringToGameStatusConverter converter;

    @BeforeEach
    void setUp() {
        converter = new StringToGameStatusConverter();
    }

    @Test
    void convert_shouldConvertWaitingStatus() {
        GameStatus result = converter.convert("waiting");

        assertThat(result).isEqualTo(GameStatus.WAITING);
    }

    @Test
    void convert_shouldConvertInProgressStatus() {
        GameStatus result = converter.convert("in_progress");

        assertThat(result).isEqualTo(GameStatus.IN_PROGRESS);
    }

    @Test
    void convert_shouldConvertFinishedStatus() {
        GameStatus result = converter.convert("finished");

        assertThat(result).isEqualTo(GameStatus.FINISHED);
    }

    @Test
    void convert_shouldConvertAbandonedStatus() {
        GameStatus result = converter.convert("abandoned");

        assertThat(result).isEqualTo(GameStatus.ABANDONED);
    }

    @Test
    void convert_shouldConvertDrawStatus() {
        GameStatus result = converter.convert("draw");

        assertThat(result).isEqualTo(GameStatus.DRAW);
    }

    @Test
    void convert_shouldReturnNullForEmptyString() {
        GameStatus result = converter.convert("");

        assertThat(result).isNull();
    }

    @Test
    void convert_shouldReturnNullForWhitespaceString() {
        GameStatus result = converter.convert("   ");

        assertThat(result).isNull();
    }

    @Test
    void convert_shouldThrowExceptionForInvalidStatus() {
        assertThatThrownBy(() -> converter.convert("invalid_status"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown game status");
    }

    @Test
    void convert_shouldHandleCaseSensitiveInput() {
        assertThatThrownBy(() -> converter.convert("WAITING"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown game status");
    }

    @Test
    void convert_shouldHandleTrimmedInput() {
        assertThatThrownBy(() -> converter.convert("  waiting  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown game status");
    }
}

