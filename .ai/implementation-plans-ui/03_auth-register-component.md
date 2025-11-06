# Plan implementacji: AuthRegisterComponent

## 1. Przegląd

**Nazwa komponentu**: `AuthRegisterComponent`  
**Lokalizacja**: `frontend/src/app/features/auth/auth-register.component.ts`  
**Ścieżka routingu**: `/auth/register`  
**Typ**: Standalone component

## 2. Główny cel

Umożliwienie nowym użytkownikom utworzenia konta w systemie w celu śledzenia postępów i stałego dostępu do profilu.

## 3. Funkcjonalności

### 3.1 Formularz rejestracji
- Pole nazwa użytkownika (wymagane, 3-50 znaków, alfanumeryczne + podkreślniki)
- Pole email (wymagane, format email, unikalny)
- Pole hasło (wymagane, min. długość, wymagania bezpieczeństwa)
- Pole potwierdzenie hasła (wymagane, musi być zgodne z hasłem)
- Przycisk submit
- Link do logowania

### 3.2 Walidacja
- Walidacja po stronie klienta (reactive forms)
- Walidacja po stronie serwera (API)
- Wyświetlanie błędów walidacji
- Wskaźnik siły hasła

### 3.3 Obsługa błędów
- 409 Conflict: Nazwa użytkownika lub email już istnieje
- 422 Unprocessable Entity: Błędy walidacji
- Toast notifications dla błędów

### 3.4 Po sukcesie
- Zapisanie tokenu JWT w localStorage
- Aktualizacja stanu użytkownika (AuthService)
- Przekierowanie do HomeComponent
- Toast notification z potwierdzeniem rejestracji

## 4. Struktura komponentu

### 4.1 Template

```html
<div class="auth-container">
  <div class="auth-card">
    <h2>Rejestracja</h2>
    
    <form [formGroup]="registerForm" (ngSubmit)="onSubmit()">
      <div class="form-group">
        <label for="username">Nazwa użytkownika</label>
        <input
          id="username"
          type="text"
          pInputText
          formControlName="username"
          [class.ng-invalid]="isFieldInvalid('username')"
          aria-describedby="username-error"
          aria-required="true">
        <small
          id="username-error"
          class="error-message"
          *ngIf="isFieldInvalid('username')">
          {{ getFieldError('username') }}
        </small>
      </div>

      <div class="form-group">
        <label for="email">Email</label>
        <input
          id="email"
          type="email"
          pInputText
          formControlName="email"
          [class.ng-invalid]="isFieldInvalid('email')"
          aria-describedby="email-error"
          aria-required="true">
        <small
          id="email-error"
          class="error-message"
          *ngIf="isFieldInvalid('email')">
          {{ getFieldError('email') }}
        </small>
      </div>

      <div class="form-group">
        <label for="password">Hasło</label>
        <p-password
          id="password"
          formControlName="password"
          [feedback]="true"
          [toggleMask]="true"
          [class.ng-invalid]="isFieldInvalid('password')"
          aria-describedby="password-error password-strength"
          aria-required="true">
        </p-password>
        <app-password-strength-indicator
          [password]="registerForm.get('password')?.value"
          id="password-strength">
        </app-password-strength-indicator>
        <small
          id="password-error"
          class="error-message"
          *ngIf="isFieldInvalid('password')">
          {{ getFieldError('password') }}
        </small>
      </div>

      <div class="form-group">
        <label for="confirmPassword">Potwierdzenie hasła</label>
        <p-password
          id="confirmPassword"
          formControlName="confirmPassword"
          [feedback]="false"
          [toggleMask]="true"
          [class.ng-invalid]="isFieldInvalid('confirmPassword')"
          aria-describedby="confirmPassword-error"
          aria-required="true">
        </p-password>
        <small
          id="confirmPassword-error"
          class="error-message"
          *ngIf="isFieldInvalid('confirmPassword')">
          {{ getFieldError('confirmPassword') }}
        </small>
      </div>

      <p-button
        type="submit"
        label="Zarejestruj się"
        [disabled]="registerForm.invalid || isLoading"
        [loading]="isLoading">
      </p-button>
    </form>

    <div class="auth-links">
      <p>Masz już konto? <a routerLink="/auth/login">Zaloguj się</a></p>
      <p>Lub <a routerLink="/">graj jako gość</a></p>
    </div>
  </div>
</div>
```

### 4.2 Komponent TypeScript

```typescript
@Component({
  selector: 'app-auth-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    FormsModule,
    PasswordStrengthIndicatorComponent
  ],
  templateUrl: './auth-register.component.html',
  styleUrls: ['./auth-register.component.scss']
})
export class AuthRegisterComponent implements OnInit {
  registerForm: FormGroup;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private messageService: MessageService
  ) {
    this.registerForm = this.fb.group({
      username: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
        Validators.pattern(/^[a-zA-Z0-9_]+$/)
      ]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]]
    }, {
      validators: this.passwordMatchValidator
    });
  }

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/']);
    }
  }

  passwordMatchValidator(form: AbstractControl): ValidationErrors | null {
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');
    
    if (password && confirmPassword && password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    
    return null;
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.markFormGroupTouched(this.registerForm);
      return;
    }

    this.isLoading = true;
    const { username, email, password } = this.registerForm.value;

    this.authService.register(username, email, password).subscribe({
      next: (user) => {
        this.isLoading = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Sukces',
          detail: 'Konto zostało utworzone pomyślnie'
        });
        this.router.navigate(['/']);
      },
      error: (error) => {
        this.isLoading = false;
        this.handleRegisterError(error);
      }
    });
  }

  private handleRegisterError(error: HttpErrorResponse): void {
    let message = 'Wystąpił błąd podczas rejestracji';
    
    if (error.status === 409) {
      const errorMessage = error.error?.message || '';
      if (errorMessage.includes('username')) {
        message = 'Nazwa użytkownika już istnieje';
      } else if (errorMessage.includes('email')) {
        message = 'Email już istnieje';
      } else {
        message = errorMessage;
      }
    } else if (error.status === 422) {
      message = 'Nieprawidłowe dane. Sprawdź formularz.';
      this.handleValidationErrors(error.error);
    }

    this.messageService.add({
      severity: 'error',
      summary: 'Błąd rejestracji',
      detail: message
    });
  }

  private handleValidationErrors(errors: any): void {
    if (errors?.errors) {
      Object.keys(errors.errors).forEach(key => {
        const control = this.registerForm.get(key);
        if (control) {
          control.setErrors({ serverError: errors.errors[key] });
          control.markAsTouched();
        }
      });
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
        return 'To pole jest wymagane';
      }
      if (field.errors['email']) {
        return 'Nieprawidłowy format email';
      }
      if (field.errors['minlength']) {
        return `Minimalna długość: ${field.errors['minlength'].requiredLength} znaków`;
      }
      if (field.errors['maxlength']) {
        return `Maksymalna długość: ${field.errors['maxlength'].requiredLength} znaków`;
      }
      if (field.errors['pattern']) {
        if (fieldName === 'username') {
          return 'Nazwa użytkownika może zawierać tylko litery, cyfry i podkreślniki';
        }
      }
      if (field.errors['passwordMismatch']) {
        return 'Hasła nie są zgodne';
      }
      if (field.errors['serverError']) {
        return field.errors['serverError'];
      }
    }
    return '';
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }
}
```

## 5. Integracja API

### 5.1 Endpointy

- `POST /api/auth/register` - rejestracja nowego użytkownika

**Request body**:
```json
{
  "email": "string",
  "password": "string",
  "username": "string"
}
```

**Response (201 Created)**:
```json
{
  "userId": "string (UUID)",
  "username": "string",
  "email": "string",
  "isGuest": false,
  "totalPoints": 0,
  "gamesPlayed": 0,
  "gamesWon": 0,
  "authToken": "string (JWT)"
}
```

### 5.2 Serwisy

- `AuthService.register(username, email, password)` - rejestracja nowego użytkownika
- `AuthService.isAuthenticated()` - sprawdzenie czy użytkownik jest zalogowany

## 6. Walidacja

### 6.1 Walidacja po stronie klienta

- **Nazwa użytkownika**: 
  - Wymagane (`Validators.required`)
  - 3-50 znaków (`Validators.minLength(3)`, `Validators.maxLength(50)`)
  - Pattern: `/^[a-zA-Z0-9_]+$/` (alfanumeryczne + podkreślniki)
- **Email**: 
  - Wymagane (`Validators.required`)
  - Format email (`Validators.email`)
- **Hasło**: 
  - Wymagane (`Validators.required`)
  - Minimalna długość 8 znaków (`Validators.minLength(8)`)
- **Potwierdzenie hasła**: 
  - Wymagane (`Validators.required`)
  - Custom validator sprawdzający zgodność z hasłem

### 6.2 Walidacja po stronie serwera

- Sprawdzenie unikalności nazwy użytkownika
- Sprawdzenie unikalności email
- Sprawdzenie wymagań bezpieczeństwa hasła
- Generowanie tokenu JWT

## 7. Komponenty współdzielone

### 7.1 PasswordStrengthIndicatorComponent

**Lokalizacja**: `components/ui/password-strength-indicator.component.ts`

**Funkcjonalność**:
- Wyświetlanie wskaźnika siły hasła
- Wizualne wskaźniki (słabe, średnie, silne)
- Opcjonalne: lista wymagań hasła

**Inputs**:
- `password: string` - hasło do analizy

## 8. Obsługa błędów

### 8.1 Błędy API

- **409 Conflict**: Nazwa użytkownika lub email już istnieje
  - Toast notification z konkretnym komunikatem
  - Ustawienie błędów walidacji w formularzu
- **422 Unprocessable Entity**: Błędy walidacji
  - Wyświetlanie błędów pod odpowiednimi polami
  - Toast notification z ogólnym komunikatem

### 8.2 Błędy walidacji

- Wyświetlanie błędów pod każdym polem
- Markowanie pól jako touched po submit
- Wyświetlanie błędów tylko dla touched fields

## 9. Stylowanie

### 9.1 SCSS

```scss
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 200px);
  padding: 2rem;

  .auth-card {
    width: 100%;
    max-width: 400px;
    padding: 2rem;
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

    h2 {
      margin-bottom: 1.5rem;
      text-align: center;
    }

    .form-group {
      margin-bottom: 1.5rem;

      label {
        display: block;
        margin-bottom: 0.5rem;
        font-weight: 500;
      }

      .error-message {
        color: #dc3545;
        font-size: 0.875rem;
        margin-top: 0.25rem;
        display: block;
      }
    }

    .auth-links {
      margin-top: 1.5rem;
      text-align: center;

      p {
        margin: 0.5rem 0;
      }

      a {
        color: #007bff;
        text-decoration: none;

        &:hover {
          text-decoration: underline;
        }
      }
    }
  }
}
```

## 10. Animacje

- Fade-in dla formularza (300ms)
- Smooth transitions dla przycisków
- Pulse animation dla błędów walidacji
- Progress animation dla wskaźnika siły hasła

## 11. Testy

### 11.1 Testy jednostkowe

- Sprawdzenie walidacji formularza
- Sprawdzenie walidacji zgodności haseł
- Sprawdzenie rejestracji z poprawnymi danymi
- Sprawdzenie obsługi błędów (409, 422)
- Sprawdzenie przekierowania po sukcesie

### 11.2 Testy E2E (Cypress)

- Scenariusz: Rejestracja z poprawnymi danymi
- Scenariusz: Rejestracja z nieprawidłowymi danymi
- Scenariusz: Rejestracja z duplikatem nazwy użytkownika/email

## 12. Dostępność

- ARIA labels dla wszystkich pól formularza
- ARIA describedby dla błędów walidacji
- Keyboard navigation (Tab, Enter)
- Focus management
- Screen reader support dla komunikatów błędów i wskaźnika siły hasła

## 13. Wsparcie dla wielu języków (i18n)

### 13.1 Implementacja

Komponent wykorzystuje Angular i18n do obsługi wielu języków. Wszystkie teksty w komponencie są tłumaczone:
- Etykiety pól formularza ("Nazwa użytkownika", "Email", "Hasło", "Potwierdzenie hasła")
- Komunikaty błędów walidacji
- Komunikaty błędów API (409, 422)
- Linki ("Masz już konto? Zaloguj się", "Lub graj jako gość")

### 13.2 Języki wspierane

- **Angielski (en)** - język podstawowy, domyślny
- **Polski (pl)** - język dodatkowy

### 13.3 Użycie

Wszystkie teksty w template są opakowane w pipe `i18n` lub używają serwisu `TranslateService`:
```typescript
{{ 'auth.register.title' | translate }}
{{ 'auth.register.username' | translate }}
{{ 'auth.register.email' | translate }}
```

### 13.4 Backend

Backend pozostaje bez zmian - wszystkie odpowiedzi API są w języku angielskim. Tłumaczenie komunikatów błędów z backendu na język użytkownika odbywa się po stronie frontendu.

## 14. Mapowanie historyjek użytkownika

- **US-002**: Rejestracja nowego użytkownika

