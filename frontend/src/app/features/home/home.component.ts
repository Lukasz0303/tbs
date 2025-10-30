import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CounterFacade } from '../counter/counter.facade';
import { ButtonComponent } from '../../components/ui/button/button.component';
import { ThemeService } from '../../core/theme.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, ButtonComponent],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent {
  readonly counter = inject(CounterFacade);
  readonly theme = inject(ThemeService);
}
