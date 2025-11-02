package com.tbs.model;

import com.tbs.enums.PlayerSymbol;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "moves")
public class Move {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private User player;

    @Column(name = "row", nullable = false, columnDefinition = "SMALLINT")
    private Short row;

    @Column(name = "col", nullable = false, columnDefinition = "SMALLINT")
    private Short col;

    @Convert(converter = com.tbs.converter.PlayerSymbolConverter.class)
    @Column(name = "player_symbol", nullable = false)
    private PlayerSymbol playerSymbol;

    @Column(name = "move_order", nullable = false, columnDefinition = "SMALLINT")
    private Short moveOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Move() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public User getPlayer() {
        return player;
    }

    public void setPlayer(User player) {
        this.player = player;
    }

    public Short getRow() {
        return row;
    }

    public void setRow(Short row) {
        this.row = row;
    }

    public Short getCol() {
        return col;
    }

    public void setCol(Short col) {
        this.col = col;
    }

    public PlayerSymbol getPlayerSymbol() {
        return playerSymbol;
    }

    public void setPlayerSymbol(PlayerSymbol playerSymbol) {
        this.playerSymbol = playerSymbol;
    }

    public Short getMoveOrder() {
        return moveOrder;
    }

    public void setMoveOrder(Short moveOrder) {
        this.moveOrder = moveOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

