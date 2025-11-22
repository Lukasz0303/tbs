import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { GameOptionsComponent } from './game-options.component';
import { AuthService } from '../../services/auth.service';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '../../services/translate.service';
import { MessageService } from 'primeng/api';
import { LoggerService } from '../../services/logger.service';
import { GameService } from '../../services/game.service';
import { Game } from '../../models/game.model';

describe('GameOptionsComponent', () => {
  let fixture: ComponentFixture<GameOptionsComponent>;
  let component: GameOptionsComponent;
  let createGameSpy: jasmine.Spy;

  const authServiceStub = {
    getCurrentUser: () => of(null),
    isAuthenticated: () => false,
    createGuestSession: () => of(null),
    logout: () => of(undefined),
    forceLogout: () => {},
  };

  const routerStub = {
    navigate: () => Promise.resolve(true),
  };

  const routeStub = {
    snapshot: { queryParams: {} },
  };

  const translateStub = {
    translate: (key: string) => key,
  };

  const messageServiceStub = {
    add: () => {},
  };

  const loggerStub = {
    error: () => {},
    warn: () => {},
    debug: () => {},
  };

  const mockGame: Game = {
    gameId: 1,
    boardSize: 3,
    boardState: Array(3).fill(null).map(() => Array(3).fill(null)),
    currentPlayerSymbol: 'x',
    gameType: 'vs_bot',
    status: 'waiting',
    totalMoves: 0,
    player1Id: 1,
    player2Id: null,
    botDifficulty: 'easy',
    winnerId: null,
    lastMoveAt: null,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    finishedAt: null,
  };

  const gameServiceStub = {
    getSavedGame: () => of(null),
    createBotGame: () => of(mockGame),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GameOptionsComponent],
      providers: [
        { provide: AuthService, useValue: authServiceStub },
        { provide: Router, useValue: routerStub },
        { provide: ActivatedRoute, useValue: routeStub },
        { provide: TranslateService, useValue: translateStub },
        { provide: MessageService, useValue: messageServiceStub },
        { provide: LoggerService, useValue: loggerStub },
        { provide: GameService, useValue: gameServiceStub },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GameOptionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    createGameSpy = spyOn(component as any, 'createGame').and.returnValue(of(mockGame));
  });

  it('should retry creating a new game when 404 occurs', (done) => {
    const error = new HttpErrorResponse({ status: 404 });

    (component as any)
      .handleGameCreationError(error, 'easy', 3)
      .subscribe({
        next: () => {
          expect(createGameSpy).toHaveBeenCalled();
          done();
        },
        error: done.fail,
      });
  });

  it('should not treat 404 as a connection error', () => {
    const notFoundError = new HttpErrorResponse({ status: 404 });
    const disconnectError = new HttpErrorResponse({ status: 0 });

    const isNotFoundConnection = (component as any).isConnectionError(notFoundError);
    const isDisconnectConnection = (component as any).isConnectionError(disconnectError);

    expect(isNotFoundConnection).toBeFalse();
    expect(isDisconnectConnection).toBeTrue();
  });
});

