package com.tbs.dto.game;

import com.tbs.dto.common.BoardState;
import com.tbs.dto.move.MoveListItem;
import com.tbs.dto.user.PlayerInfo;
import com.tbs.dto.user.WinnerInfo;
import com.tbs.enums.BotDifficulty;
import com.tbs.enums.BoardSize;
import com.tbs.enums.GameStatus;
import com.tbs.enums.GameType;
import com.tbs.enums.PlayerSymbol;
import java.time.Instant;
import java.util.List;

public record GameDetailResponse(
        long gameId,
        GameType gameType,
        BoardSize boardSize,
        PlayerInfo player1,
        PlayerInfo player2,
        WinnerInfo winner,
        BotDifficulty botDifficulty,
        GameStatus status,
        PlayerSymbol currentPlayerSymbol,
        Instant lastMoveAt,
        Instant createdAt,
        Instant updatedAt,
        Instant finishedAt,
        BoardState boardState,
        int totalMoves,
        List<MoveListItem> moves
) {}

