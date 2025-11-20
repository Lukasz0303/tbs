import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { MessageModule } from 'primeng/message';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../services/auth.service';
import { TranslateService } from '../../services/translate.service';
import { LoggerService } from '../../services/logger.service';
import { getAvatarPath } from '../../utils/avatar.util';

@Component({
  selector: 'app-auth-register',
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
  templateUrl: './auth-register.component.html',
  styleUrls: ['./auth-register.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthRegisterComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);
  readonly translateService = inject(TranslateService);
  private readonly logger = inject(LoggerService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly registerForm: FormGroup;
  readonly isLoading = signal<boolean>(false);
  readonly showPassword = signal<boolean>(false);
  readonly showConfirmPassword = signal<boolean>(false);
  readonly selectedAvatar = signal<number>(1);

  constructor() {
    this.registerForm = this.fb.group(
      {
        username: [
          '',
          [
            Validators.required,
            Validators.minLength(3),
            Validators.maxLength(50),
            Validators.pattern(/^[a-zA-Z0-9_]+$/),
          ],
        ],
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]],
        avatar: [1, [Validators.required, Validators.min(1), Validators.max(6)]],
      },
      {
        validators: this.passwordMatchValidator,
      }
    );
  }

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/']);
    }
  }

  passwordMatchValidator(form: AbstractControl): ValidationErrors | null {
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');

    if (!password || !confirmPassword) {
      return null;
    }

    if (password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }

    if (confirmPassword.hasError('passwordMismatch')) {
      const errors = { ...confirmPassword.errors };
      delete errors['passwordMismatch'];
      confirmPassword.setErrors(Object.keys(errors).length > 0 ? errors : null);
    }

    return null;
  }

  onSubmit(): void {
    if (this.isLoading() || this.registerForm.pending) {
      return;
    }

    if (this.registerForm.invalid) {
      this.markFormGroupTouched(this.registerForm);
      return;
    }

    this.isLoading.set(true);
    const { username, email, password, avatar } = this.registerForm.value;

    this.authService
      .register(username, email, password, avatar || 1)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isLoading.set(false);
          this.messageService.add({
            severity: 'success',
            summary: this.translateService.translate('auth.register.success.title'),
            detail: this.translateService.translate('auth.register.success.detail'),
          });
          this.router.navigate(['/']);
        },
        error: (error: HttpErrorResponse) => {
          this.isLoading.set(false);
          this.handleRegisterError(error);
        },
      });
  }

  private handleRegisterError(error: HttpErrorResponse): void {
    let messageKey = 'auth.register.error.generic';

    if (error.status === 409) {
      const errorMessage = error.error?.message || '';
      if (errorMessage.includes('username')) {
        messageKey = 'auth.register.error.usernameExists';
      } else if (errorMessage.includes('email')) {
        messageKey = 'auth.register.error.emailExists';
      } else {
        messageKey = 'auth.register.error.conflict';
      }
    } else if (error.status === 422) {
      messageKey = 'auth.register.error.validation';
      this.handleValidationErrors(error.error);
    }

    this.messageService.add({
      severity: 'error',
      summary: this.translateService.translate('auth.register.error.title'),
      detail: this.translateService.translate(messageKey),
    });
  }

  private handleValidationErrors(errors: unknown): void {
    if (errors && typeof errors === 'object' && 'errors' in errors) {
      const validationErrors = errors as { errors: Record<string, string> };
      Object.keys(validationErrors.errors).forEach((key) => {
        const control = this.registerForm.get(key);
        if (control) {
          control.setErrors({ serverError: validationErrors.errors[key] });
          control.markAsTouched();
        }
      });
      this.cdr.markForCheck();
    }
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.registerForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  getFieldError(fieldName: string): string {
    const field = this.registerForm.get(fieldName);
    if (field && field.errors) {
      if (field.errors['required']) {
        return this.translateService.translate('auth.register.validation.required');
      }
      if (field.errors['email']) {
        return this.translateService.translate('auth.register.validation.email');
      }
      if (field.errors['minlength']) {
        return this.translateService.translate('auth.register.validation.minLength', {
          min: field.errors['minlength'].requiredLength,
        });
      }
      if (field.errors['maxlength']) {
        return this.translateService.translate('auth.register.validation.maxLength', {
          max: field.errors['maxlength'].requiredLength,
        });
      }
      if (field.errors['pattern']) {
        if (fieldName === 'username') {
          return this.translateService.translate('auth.register.validation.usernamePattern');
        }
      }
      if (field.errors['passwordMismatch']) {
        return this.translateService.translate('auth.register.validation.passwordMismatch');
      }
      if (field.errors['serverError']) {
        return field.errors['serverError'];
      }
    }
    return '';
  }

  togglePasswordVisibility(): void {
    this.showPassword.update((value) => !value);
  }

  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword.update((value) => !value);
  }

  selectAvatar(avatarId: number): void {
    this.selectedAvatar.set(avatarId);
    this.registerForm.patchValue({ avatar: avatarId });
  }

  getAvatarPath(avatarId: number): string {
    return getAvatarPath(avatarId);
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach((key) => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }
}

