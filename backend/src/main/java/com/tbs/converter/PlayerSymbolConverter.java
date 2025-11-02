package com.tbs.converter;

import com.tbs.enums.PlayerSymbol;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PlayerSymbolConverter implements AttributeConverter<PlayerSymbol, String> {

    @Override
    public String convertToDatabaseColumn(PlayerSymbol playerSymbol) {
        if (playerSymbol == null) {
            return null;
        }
        return playerSymbol.getValue();
    }

    @Override
    public PlayerSymbol convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return PlayerSymbol.fromValue(value);
    }
}

