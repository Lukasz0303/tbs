import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

@Component({
  selector: 'app-game-bot-indicator',
  standalone: true,
  imports: [CommonModule, ProgressSpinnerModule],
  templateUrl: './game-bot-indicator.component.html',
  styleUrls: ['./game-bot-indicator.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameBotIndicatorComponent {}


