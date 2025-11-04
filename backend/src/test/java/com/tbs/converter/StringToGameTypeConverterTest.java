package com.tbs.converter;

import com.tbs.enums.GameType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StringToGameTypeConverterTest {

    private StringToGameTypeConverter converter;

    @BeforeEach
    void setUp() {
        converter = new StringToGameTypeConverter();
    }

    @Test
    void convert_shouldConvertVsBotType() {
        GameType result = converter.convert("vs_bot");

        assertThat(result).isEqualTo(GameType.VS_BOT);
    }

    @Test
    void convert_shouldConvertPvpType() {
        GameType result = converter.convert("pvp");

        assertThat(result).isEqualTo(GameType.PVP);
    }

    @Test
    void convert_shouldReturnNullForEmptyString() {
        GameType result = converter.convert("");

        assertThat(result).isNull();
    }

    @Test
    void convert_shouldReturnNullForWhitespaceString() {
        GameType result = converter.convert("   ");

        assertThat(result).isNull();
    }

    @Test
    void convert_shouldThrowExceptionForInvalidType() {
        assertThatThrownBy(() -> converter.convert("invalid_type"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown game type");
    }

    @Test
    void convert_shouldHandleCaseSensitiveInput() {
        assertThatThrownBy(() -> converter.convert("VS_BOT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown game type");
    }

    @Test
    void convert_shouldHandleTrimmedInput() {
        assertThatThrownBy(() -> converter.convert("  vs_bot  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown game type");
    }
}

