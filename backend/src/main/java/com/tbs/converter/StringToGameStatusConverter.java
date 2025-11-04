package com.tbs.converter;

import com.tbs.enums.GameStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class StringToGameStatusConverter implements Converter<String, GameStatus> {

    @Override
    public GameStatus convert(@NonNull String source) {
        if (source.trim().isEmpty()) {
            return null;
        }
        return GameStatus.fromValue(source);
    }
}

