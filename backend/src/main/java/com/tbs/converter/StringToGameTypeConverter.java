package com.tbs.converter;

import com.tbs.enums.GameType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class StringToGameTypeConverter implements Converter<String, GameType> {

    @Override
    public GameType convert(@NonNull String source) {
        if (source.trim().isEmpty()) {
            return null;
        }
        return GameType.fromValue(source);
    }
}

