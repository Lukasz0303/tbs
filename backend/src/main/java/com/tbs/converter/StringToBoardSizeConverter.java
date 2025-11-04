package com.tbs.converter;

import com.tbs.enums.BoardSize;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class StringToBoardSizeConverter implements Converter<String, BoardSize> {

    @Override
    public BoardSize convert(@NonNull String source) {
        if (source.trim().isEmpty()) {
            return null;
        }
        return BoardSize.fromValue(source);
    }
}

