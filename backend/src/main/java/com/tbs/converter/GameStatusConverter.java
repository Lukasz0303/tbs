package com.tbs.converter;

import com.tbs.enums.GameStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GameStatusConverter implements AttributeConverter<GameStatus, String> {

    @Override
    public String convertToDatabaseColumn(GameStatus gameStatus) {
        if (gameStatus == null) {
            return null;
        }
        return gameStatus.getValue();
    }

    @Override
    public GameStatus convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return GameStatus.fromValue(value);
    }
}

