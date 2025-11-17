import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { take } from 'rxjs';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { MessageModule } from 'primeng/message';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../services/auth.service';
import { TranslateService } from '../../services/translate.service';
import { GameService } from '../../services/game.service';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-auth-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    InputTextModule,
    ButtonModule,
    ToastModule,
    MessageModule,
  ],
  providers: [MessageService],
  templateUrl: './auth-login.component.html',
  styleUrls: ['./auth-login.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthLoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);
  readonly translateService = inject(TranslateService);
  private readonly gameService = inject(GameService);
  private readonly destroyRef = inject(DestroyRef);

  readonly loginForm: FormGroup;
  readonly isLoading = signal<boolean>(false);
  readonly showPassword = signal<boolean>(false);

  constructor() {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
    });
  }

  ngOnInit(): void {
    this.authService
      .getCurrentUser()
      .pipe(take(1), takeUntilDestroyed(this.destroyRef))
      .subscribe((user: User | null) => {
        if (user && !user.isGuest && this.authService.getAuthToken()) {
          this.router.navigate(['/']);
          return;
        }
      });
  }

  onSubmit(): void {
    if (this.isLoading() || this.loginForm.pending) {
      return;
    }

    if (this.loginForm.invalid) {
      this.markFormGroupTouched(this.loginForm);
      return;
    }

    this.isLoading.set(true);
    const { email, password } = this.loginForm.value;

    this.authService
      .login(email, password)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isLoading.set(false);
          this.messageService.add({
            severity: 'success',
            summary: this.translateService.translate('auth.login.success.title'),
            detail: this.translateService.translate('auth.login.success.detail'),
          });
          this.redirectAfterLogin();
        },
        error: (error: HttpErrorResponse) => {
          this.isLoading.set(false);
          this.handleLoginError(error);
        },
      });
  }

  private redirectAfterLogin(): void {
    this.gameService
      .getSavedGame()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (game) => {
          if (game) {
            this.router.navigate(['/game', game.gameId]);
          } else {
            this.router.navigate(['/']);
          }
        },
        error: () => this.router.navigate(['/']),
      });
  }

  private handleLoginError(error: HttpErrorResponse): void {
    let messageKey = 'auth.login.error.generic';

    if (error.status === 401) {
      messageKey = 'auth.login.error.invalidCredentials';
    } else if (error.status === 404) {
      messageKey = 'auth.login.error.userNotFound';
    }

    this.messageService.add({
      severity: 'error',
      summary: this.translateService.translate('auth.login.error.title'),
      detail: this.translateService.translate(messageKey),
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.loginForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  getFieldError(fieldName: string): string {
    const field = this.loginForm.get(fieldName);
    if (field && field.errors) {
      if (field.errors['required']) {
        return this.translateService.translate('auth.login.validation.required');
      }
      if (field.errors['email']) {
        return this.translateService.translate('auth.login.validation.email');
      }
      if (field.errors['minlength']) {
        return this.translateService.translate('auth.login.validation.minLength', {
          min: field.errors['minlength'].requiredLength,
        });
      }
    }
    return '';
  }

  togglePasswordVisibility(): void {
    this.showPassword.update((value) => !value);
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach((key) => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }
}

