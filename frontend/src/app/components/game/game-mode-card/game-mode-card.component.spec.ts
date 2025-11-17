import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GameModeCardComponent } from './game-mode-card.component';
import { TranslateService } from '../../../services/translate.service';
import { GameMode } from '../../../models/game.model';

describe('GameModeCardComponent', () => {
  let fixture: ComponentFixture<GameModeCardComponent>;
  let component: GameModeCardComponent;

  const guestMode: GameMode = {
    id: 'guest',
    labelKey: 'home.gameModes.guest.title',
    descriptionKey: 'home.gameModes.guest.description',
    icon: 'user-plus',
    route: null,
    availableForGuest: true,
    availableForRegistered: false,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GameModeCardComponent],
      providers: [TranslateService],
    }).compileComponents();

    fixture = TestBed.createComponent(GameModeCardComponent);
    component = fixture.componentInstance;
    component.mode = guestMode;
    component.isGuest = true;
    fixture.detectChanges();
  });

  it('powinien utworzyć komponent', () => {
    expect(component).toBeTruthy();
  });

  it('powinien emitować zdarzenie wyboru trybu przy kliknięciu karty', () => {
    const spy = spyOn(component.modeSelected, 'emit');
    const card: HTMLElement = fixture.nativeElement.querySelector('.game-mode-card');

    card.click();

    expect(spy).toHaveBeenCalledWith(guestMode);
  });

  it('nie powinien emitować zdarzenia, jeśli tryb niedostępny', () => {
    component.isGuest = false;
    fixture.detectChanges();
    const spy = spyOn(component.modeSelected, 'emit');
    const card: HTMLElement = fixture.nativeElement.querySelector('.game-mode-card');

    card.click();

    expect(spy).not.toHaveBeenCalled();
  });
});

