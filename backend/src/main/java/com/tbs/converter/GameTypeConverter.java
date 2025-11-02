package com.tbs.converter;

import com.tbs.enums.GameType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GameTypeConverter implements AttributeConverter<GameType, String> {

    @Override
    public String convertToDatabaseColumn(GameType gameType) {
        if (gameType == null) {
            return null;
        }
        return gameType.getValue();
    }

    @Override
    public GameType convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return GameType.fromValue(value);
    }
}

