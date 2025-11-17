import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { HomeComponent } from './home.component';
import { AuthService } from '../../services/auth.service';
import { GameService } from '../../services/game.service';
import { TranslateService } from '../../services/translate.service';
import { Game } from '../../models/game.model';
import { MessageService } from 'primeng/api';

describe('HomeComponent', () => {
  let fixture: ComponentFixture<HomeComponent>;
  let component: HomeComponent;
  let authService: jasmine.SpyObj<AuthService>;
  let gameService: jasmine.SpyObj<GameService>;
  let router: Router;
  let messageService: MessageService;

  const guestResponse = {
    userId: 1,
    isGuest: true,
    totalPoints: 0,
    gamesPlayed: 0,
    gamesWon: 0,
    createdAt: new Date().toISOString(),
  };

  const savedGame: Game = {
    gameId: 42,
    gameType: 'vs_bot',
    boardSize: 3,
    status: 'in_progress',
    player1Id: 1,
    player2Id: null,
    botDifficulty: 'easy',
    currentPlayerSymbol: 'x',
    winnerId: null,
    lastMoveAt: new Date().toISOString(),
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    finishedAt: null,
    totalMoves: 5,
  };

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>(
      'AuthService',
      ['loadCurrentUser', 'getCurrentUser', 'isGuest', 'createGuestSession']
    );
    gameService = jasmine.createSpyObj<GameService>('GameService', ['getSavedGame']);

    authService.loadCurrentUser.and.returnValue(of(null));
    authService.getCurrentUser.and.returnValue(of(null));
    authService.isGuest.and.returnValue(of(true));
    authService.createGuestSession.and.returnValue(of(guestResponse));
    gameService.getSavedGame.and.returnValue(of(savedGame));

    await TestBed.configureTestingModule({
      imports: [HomeComponent, RouterTestingModule, NoopAnimationsModule],
      providers: [
        TranslateService,
        { provide: AuthService, useValue: authService },
        { provide: GameService, useValue: gameService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    messageService = TestBed.inject(MessageService);
  });

  function initializeComponent(): void {
    fixture.detectChanges();
  }

  it('powinien utworzyć komponent', () => {
    initializeComponent();
    expect(component).toBeTruthy();
  });

  it('powinien wyświetlić banner zapisanej gry', fakeAsync(() => {
    initializeComponent();
    tick();
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('[data-testid="game-banner"]');
    expect(banner).toBeTruthy();
  }));

  it('powinien nawigować do trybu bot przy wyborze karty', () => {
    initializeComponent();
    const navigateSpy = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));

    component.onGameModeSelected(component.gameModes[1]);

    expect(navigateSpy).toHaveBeenCalledWith(['/game/mode-selection']);
  });

  it('powinien wyświetlić toast przy błędzie utworzenia sesji gościa', fakeAsync(() => {
    initializeComponent();
    authService.createGuestSession.and.returnValue(throwError(() => new Error('fail')));
    const toastSpy = spyOn(messageService, 'add');
    const navigateSpy = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));

    component.onGameModeSelected(component.gameModes[0]);
    tick();

    expect(toastSpy).toHaveBeenCalled();
    expect(navigateSpy).not.toHaveBeenCalled();
  }));
});

