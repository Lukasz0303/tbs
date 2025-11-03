package com.tbs.model;

import com.tbs.enums.BotDifficulty;
import com.tbs.enums.BoardSize;
import com.tbs.enums.GameStatus;
import com.tbs.enums.GameType;
import com.tbs.enums.PlayerSymbol;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Convert(converter = com.tbs.converter.GameTypeConverter.class)
    @Column(name = "game_type", nullable = false)
    private GameType gameType;

    @Convert(converter = com.tbs.converter.BoardSizeConverter.class)
    @Column(name = "board_size", nullable = false)
    private BoardSize boardSize;

    @ManyToOne
    @JoinColumn(name = "player1_id", nullable = false)
    private User player1;

    @ManyToOne
    @JoinColumn(name = "player2_id")
    private User player2;

    @Convert(converter = com.tbs.converter.BotDifficultyConverter.class)
    @Column(name = "bot_difficulty")
    private BotDifficulty botDifficulty;

    @Convert(converter = com.tbs.converter.GameStatusConverter.class)
    @Column(name = "status", nullable = false)
    private GameStatus status;

    @Convert(converter = com.tbs.converter.PlayerSymbolConverter.class)
    @Column(name = "current_player_symbol")
    private PlayerSymbol currentPlayerSymbol;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    @Column(name = "last_move_at")
    private Instant lastMoveAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    public Game() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }

    public BoardSize getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(BoardSize boardSize) {
        this.boardSize = boardSize;
    }

    public User getPlayer1() {
        return player1;
    }

    public void setPlayer1(User player1) {
        this.player1 = player1;
    }

    public User getPlayer2() {
        return player2;
    }

    public void setPlayer2(User player2) {
        this.player2 = player2;
    }

    public BotDifficulty getBotDifficulty() {
        return botDifficulty;
    }

    public void setBotDifficulty(BotDifficulty botDifficulty) {
        this.botDifficulty = botDifficulty;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public PlayerSymbol getCurrentPlayerSymbol() {
        return currentPlayerSymbol;
    }

    public void setCurrentPlayerSymbol(PlayerSymbol currentPlayerSymbol) {
        this.currentPlayerSymbol = currentPlayerSymbol;
    }

    public User getWinner() {
        return winner;
    }

    public void setWinner(User winner) {
        this.winner = winner;
    }

    public Instant getLastMoveAt() {
        return lastMoveAt;
    }

    public void setLastMoveAt(Instant lastMoveAt) {
        this.lastMoveAt = lastMoveAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}

