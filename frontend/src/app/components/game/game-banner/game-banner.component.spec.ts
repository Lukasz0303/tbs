import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GameBannerComponent } from './game-banner.component';
import { TranslateService } from '../../../services/translate.service';
import { Game } from '../../../models/game.model';

describe('GameBannerComponent', () => {
  let fixture: ComponentFixture<GameBannerComponent>;
  let component: GameBannerComponent;

  const game: Game = {
    gameId: 12,
    gameType: 'vs_bot',
    boardSize: 4,
    status: 'in_progress',
    boardState: Array(4).fill(null).map(() => Array(4).fill(null)),
    player1Id: 7,
    player2Id: null,
    botDifficulty: 'medium',
    currentPlayerSymbol: 'o',
    winnerId: null,
    lastMoveAt: new Date().toISOString(),
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    finishedAt: null,
    totalMoves: 9,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GameBannerComponent],
      providers: [TranslateService],
    }).compileComponents();

    fixture = TestBed.createComponent(GameBannerComponent);
    component = fixture.componentInstance;
    component.game = game;
    fixture.detectChanges();
  });

  it('powinien utworzyć komponent', () => {
    expect(component).toBeTruthy();
  });

  it('powinien emitować zdarzenie kontynuacji po kliknięciu przycisku', () => {
    const spy = spyOn(component.continueGame, 'emit');
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');

    button.click();

    expect(spy).toHaveBeenCalledWith(game.gameId);
  });

  it('powinien wyświetlać informacje o poziomie trudności bota', () => {
    const tags = Array.from(
      fixture.nativeElement.querySelectorAll('.p-tag')
    ).map((tag: unknown) => (tag as Element).textContent?.trim());

    expect(tags).toContain('Bot średni');
  });
});

