export namespace WebSocketDTOs {
  export type WebSocketMessageType =
    | 'MOVE'
    | 'SURRENDER'
    | 'PING'
    | 'PONG'
    | 'MOVE_ACCEPTED'
    | 'MOVE_REJECTED'
    | 'OPPONENT_MOVE'
    | 'GAME_UPDATE'
    | 'TIMER_UPDATE'
    | 'GAME_ENDED';

  type Json =
    | string
    | number
    | boolean
    | null
    | { [key: string]: Json | undefined }
    | Json[];

  export interface BaseWebSocketMessage<T extends WebSocketMessageType> {
    type: T;
    payload: Json;
  }

  export interface MoveMessage extends BaseWebSocketMessage<'MOVE'> {
    payload: {
      row: number;
      col: number;
      playerSymbol: 'x' | 'o';
    };
  }

  export interface SurrenderMessage extends BaseWebSocketMessage<'SURRENDER'> {
    payload: Record<string, never>;
  }

  export interface PingMessage extends BaseWebSocketMessage<'PING'> {
    payload: {
      timestamp: string;
    };
  }

  export interface PongMessage extends BaseWebSocketMessage<'PONG'> {
    payload: {
      timestamp: string;
    };
  }

  export interface MoveAcceptedMessage extends BaseWebSocketMessage<'MOVE_ACCEPTED'> {
    payload: {
      moveId: number;
      row: number;
      col: number;
      playerSymbol: 'x' | 'o';
      boardState: ('x' | 'o' | null)[][];
      currentPlayerSymbol: 'x' | 'o' | null;
      nextMoveAt: string;
    };
  }

  export interface MoveRejectedMessage extends BaseWebSocketMessage<'MOVE_REJECTED'> {
    payload: {
      reason: string;
      code: string;
    };
  }

  export interface OpponentMoveMessage extends BaseWebSocketMessage<'OPPONENT_MOVE'> {
    payload: {
      row: number;
      col: number;
      playerSymbol: 'x' | 'o';
      boardState: ('x' | 'o' | null)[][];
      currentPlayerSymbol: 'x' | 'o' | null;
      nextMoveAt: string;
    };
  }

  export interface GameUpdateMessage extends BaseWebSocketMessage<'GAME_UPDATE'> {
    payload: {
      gameId: number;
      status: 'waiting' | 'in_progress' | 'finished' | 'abandoned' | 'draw';
      winner: {
        userId: number;
        username: string | null;
      } | null;
      boardState: ('x' | 'o' | null)[][];
    };
  }

  export interface TimerUpdateMessage extends BaseWebSocketMessage<'TIMER_UPDATE'> {
    payload: {
      remainingSeconds: number;
      currentPlayerSymbol: 'x' | 'o' | null;
    };
  }

  export interface GameEndedMessage extends BaseWebSocketMessage<'GAME_ENDED'> {
    payload: {
      gameId: number;
      status: 'waiting' | 'in_progress' | 'finished' | 'abandoned' | 'draw';
      winner: {
        userId: number;
        username: string | null;
      } | null;
      finalBoardState: ('x' | 'o' | null)[][];
      totalMoves: number;
    };
  }

  export type WebSocketMessage =
    | MoveMessage
    | SurrenderMessage
    | PingMessage
    | PongMessage
    | MoveAcceptedMessage
    | MoveRejectedMessage
    | OpponentMoveMessage
    | GameUpdateMessage
    | TimerUpdateMessage
    | GameEndedMessage;
}

