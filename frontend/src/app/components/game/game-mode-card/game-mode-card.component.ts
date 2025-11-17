import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  HostListener,
  Input,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { GameMode } from '../../../models/game.model';
import { TranslatePipe } from '../../../pipes/translate.pipe';

@Component({
  selector: 'app-game-mode-card',
  standalone: true,
  imports: [CommonModule, CardModule, ButtonModule, RippleModule, TranslatePipe],
  templateUrl: './game-mode-card.component.html',
  styleUrls: ['./game-mode-card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameModeCardComponent {
  @Input({ required: true }) mode!: GameMode;
  @Input() isGuest = false;
  @Output() modeSelected = new EventEmitter<GameMode>();

  @HostListener('keydown', ['$event'])
  handleKeydown(event: KeyboardEvent): void {
    if (!this.isAvailable) {
      return;
    }
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.onSelect();
    }
  }

  get isAvailable(): boolean {
    return this.isGuest
      ? this.mode.availableForGuest
      : this.mode.availableForRegistered;
  }

  get iconClass(): string {
    return `pi pi-${this.mode.icon}`;
  }

  onCardClick(): void {
    if (!this.isAvailable) {
      return;
    }
    this.onSelect();
  }

  onButtonClick(event: Event): void {
    event.stopPropagation();
    this.onSelect();
  }

  private onSelect(): void {
    if (!this.isAvailable) {
      return;
    }
    this.modeSelected.emit(this.mode);
  }
}

