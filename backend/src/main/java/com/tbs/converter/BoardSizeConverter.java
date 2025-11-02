package com.tbs.converter;

import com.tbs.enums.BoardSize;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BoardSizeConverter implements AttributeConverter<BoardSize, Short> {

    @Override
    public Short convertToDatabaseColumn(BoardSize boardSize) {
        if (boardSize == null) {
            return null;
        }
        return (short) boardSize.getValue();
    }

    @Override
    public BoardSize convertToEntityAttribute(Short value) {
        if (value == null) {
            return null;
        }
        return BoardSize.fromValue(value);
    }
}

