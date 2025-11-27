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
  let authService: jest.Mocked<AuthService>;
  let gameService: jest.Mocked<GameService>;
  let router: Router;
  let messageService: MessageService;

  const guestResponse = {
    userId: 1,
    isGuest: true,
    avatar: 1,
    totalPoints: 0,
    gamesPlayed: 0,
    gamesWon: 0,
    createdAt: new Date().toISOString(),
  };

  const registeredUserResponse = {
    userId: 1,
    isGuest: false,
    avatar: 1,
    totalPoints: 100,
    gamesPlayed: 5,
    gamesWon: 3,
    createdAt: new Date().toISOString(),
  };

  const savedGame: Game = {
    gameId: 42,
    gameType: 'vs_bot',
    boardSize: 3,
    status: 'in_progress',
    boardState: Array(3).fill(null).map(() => Array(3).fill(null)),
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
    authService = {
      loadCurrentUser: jest.fn().mockReturnValue(of(null)),
      getCurrentUser: jest.fn().mockReturnValue(of(null)),
      isGuest: jest.fn().mockReturnValue(of(true)),
      isAuthenticated: jest.fn().mockReturnValue(false),
      createGuestSession: jest.fn().mockReturnValue(of(guestResponse)),
    } as unknown as jest.Mocked<AuthService>;
    
    gameService = {
      getSavedGame: jest.fn().mockReturnValue(of(savedGame)),
    } as unknown as jest.Mocked<GameService>;

    await TestBed.configureTestingModule({
      imports: [HomeComponent, RouterTestingModule, NoopAnimationsModule],
      providers: [
        TranslateService,
        MessageService,
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

  it('powinien wyświetlić przycisk kontynuacji dla zapisanej gry', fakeAsync(() => {
    authService.getCurrentUser = jest.fn().mockReturnValue(of(registeredUserResponse));
    authService.isAuthenticated = jest.fn().mockReturnValue(true);
    
    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const continueButton = fixture.nativeElement.querySelector('button.home-action.primary');
    expect(continueButton).toBeTruthy();
    expect(continueButton?.textContent?.trim()).toContain('Kontynuuj grę');
  }));

  it('powinien nawigować do opcji gry przy wyborze gry jako gość', () => {
    initializeComponent();
    const navigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);

    component.playAsGuest();

    expect(navigateSpy).toHaveBeenCalledWith(['/game-options']);
  });

  it('powinien nawigować do opcji gry przy starcie nowej gry', () => {
    initializeComponent();
    const navigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);

    component.startNewGame();

    expect(navigateSpy).toHaveBeenCalledWith(['/game-options'], { queryParams: { new: true } });
  });
});

