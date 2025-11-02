package com.tbs.converter;

import com.tbs.enums.BotDifficulty;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BotDifficultyConverter implements AttributeConverter<BotDifficulty, String> {

    @Override
    public String convertToDatabaseColumn(BotDifficulty botDifficulty) {
        if (botDifficulty == null) {
            return null;
        }
        return botDifficulty.getValue();
    }

    @Override
    public BotDifficulty convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return BotDifficulty.fromValue(value);
    }
}

