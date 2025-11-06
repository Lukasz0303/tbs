# Plan implementacji: AuthLoginComponent

## 1. Przegląd

**Nazwa komponentu**: `AuthLoginComponent`  
**Lokalizacja**: `frontend/src/app/features/auth/auth-login.component.ts`  
**Ścieżka routingu**: `/auth/login`  
**Typ**: Standalone component

## 2. Główny cel

Umożliwienie zarejestrowanym użytkownikom zalogowania się do systemu w celu uzyskania dostępu do pełnego profilu i historii gier.

## 3. Funkcjonalności

### 3.1 Formularz logowania
- Pole email (wymagane, format email)
- Pole hasło (wymagane, min. długość)
- Przycisk submit
- Link do rejestracji

### 3.2 Walidacja
- Walidacja po stronie klienta (reactive forms)
- Walidacja po stronie serwera (API)
- Wyświetlanie błędów walidacji

### 3.3 Obsługa błędów
- 401 Unauthorized: Nieprawidłowe dane uwierzytelniające
- 404 Not Found: Użytkownik nie znaleziony
- Toast notifications dla błędów

### 3.4 Po sukcesie
- Zapisanie tokenu JWT w localStorage
- Aktualizacja stanu użytkownika (AuthService)
- Przekierowanie do HomeComponent lub ostatniej gry

## 4. Struktura komponentu

### 4.1 Template

```html
<div class="auth-container">
  <div class="auth-card">
    <h2>Logowanie</h2>
    
    <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
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
          [feedback]="false"
          [toggleMask]="true"
          [class.ng-invalid]="isFieldInvalid('password')"
          aria-describedby="password-error"
          aria-required="true">
        </p-password>
        <small
          id="password-error"
          class="error-message"
          *ngIf="isFieldInvalid('password')">
          {{ getFieldError('password') }}
        </small>
      </div>

      <p-button
        type="submit"
        label="Zaloguj się"
        [disabled]="loginForm.invalid || isLoading"
        [loading]="isLoading">
      </p-button>
    </form>

    <div class="auth-links">
      <p>Nie masz konta? <a routerLink="/auth/register">Zarejestruj się</a></p>
      <p>Lub <a routerLink="/">graj jako gość</a></p>
    </div>
  </div>
</div>
```

### 4.2 Komponent TypeScript

```typescript
@Component({
  selector: 'app-auth-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    FormsModule
  ],
  templateUrl: './auth-login.component.html',
  styleUrls: ['./auth-login.component.scss']
})
export class AuthLoginComponent implements OnInit {
  loginForm: FormGroup;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private messageService: MessageService
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/']);
    }
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.markFormGroupTouched(this.loginForm);
      return;
    }

    this.isLoading = true;
    const { email, password } = this.loginForm.value;

    this.authService.login(email, password).subscribe({
      next: (user) => {
        this.isLoading = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Sukces',
          detail: 'Zalogowano pomyślnie'
        });
        this.redirectAfterLogin();
      },
      error: (error) => {
        this.isLoading = false;
        this.handleLoginError(error);
      }
    });
  }

  private redirectAfterLogin(): void {
    this.gameService.getSavedGame().subscribe({
      next: (game) => {
        if (game) {
          this.router.navigate(['/game', game.gameId]);
        } else {
          this.router.navigate(['/']);
        }
      },
      error: () => this.router.navigate(['/'])
    });
  }

  private handleLoginError(error: HttpErrorResponse): void {
    let message = 'Wystąpił błąd podczas logowania';
    
    if (error.status === 401) {
      message = 'Nieprawidłowy email lub hasło';
    } else if (error.status === 404) {
      message = 'Użytkownik nie został znaleziony';
    }

    this.messageService.add({
      severity: 'error',
      summary: 'Błąd logowania',
      detail: message
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
        return 'To pole jest wymagane';
      }
      if (field.errors['email']) {
        return 'Nieprawidłowy format email';
      }
      if (field.errors['minlength']) {
        return `Minimalna długość: ${field.errors['minlength'].requiredLength} znaków`;
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

- `POST /api/auth/login` - logowanie użytkownika

**Request body**:
```json
{
  "email": "string",
  "password": "string"
}
```

**Response (200 OK)**:
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

- `AuthService.login(email, password)` - logowanie użytkownika
- `AuthService.isAuthenticated()` - sprawdzenie czy użytkownik jest zalogowany
- `GameService.getSavedGame()` - pobranie zapisanej gry (dla przekierowania)

## 6. Walidacja

### 6.1 Walidacja po stronie klienta

- **Email**: 
  - Wymagane (`Validators.required`)
  - Format email (`Validators.email`)
- **Hasło**: 
  - Wymagane (`Validators.required`)
  - Minimalna długość 8 znaków (`Validators.minLength(8)`)

### 6.2 Walidacja po stronie serwera

- Sprawdzenie poprawności danych uwierzytelniających
- Sprawdzenie istnienia użytkownika
- Generowanie tokenu JWT

## 7. Obsługa błędów

### 7.1 Błędy API

- **401 Unauthorized**: Nieprawidłowe dane uwierzytelniające
  - Toast notification: "Nieprawidłowy email lub hasło"
- **404 Not Found**: Użytkownik nie znaleziony
  - Toast notification: "Użytkownik nie został znaleziony"
- **500 Internal Server Error**: Błąd serwera
  - Toast notification: "Wystąpił błąd serwera. Spróbuj ponownie później."

### 7.2 Błędy walidacji

- Wyświetlanie błędów pod każdym polem
- Markowanie pól jako touched po submit
- Wyświetlanie błędów tylko dla touched fields

## 8. Stylowanie

### 8.1 SCSS

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

## 9. Animacje

- Fade-in dla formularza (300ms)
- Smooth transitions dla przycisków
- Pulse animation dla błędów walidacji

## 10. Testy

### 10.1 Testy jednostkowe

- Sprawdzenie walidacji formularza
- Sprawdzenie logowania z poprawnymi danymi
- Sprawdzenie obsługi błędów (401, 404)
- Sprawdzenie przekierowania po sukcesie

### 10.2 Testy E2E (Cypress)

- Scenariusz: Logowanie z poprawnymi danymi
- Scenariusz: Logowanie z nieprawidłowymi danymi
- Scenariusz: Przekierowanie do zapisanej gry po logowaniu

## 11. Dostępność

- ARIA labels dla wszystkich pól formularza
- ARIA describedby dla błędów walidacji
- Keyboard navigation (Tab, Enter)
- Focus management
- Screen reader support dla komunikatów błędów

## 12. Wsparcie dla wielu języków (i18n)

### 12.1 Implementacja

Komponent wykorzystuje Angular i18n do obsługi wielu języków. Wszystkie teksty w komponencie są tłumaczone:
- Etykiety pól formularza ("Email", "Hasło")
- Komunikaty błędów walidacji
- Komunikaty błędów API
- Linki ("Nie masz konta? Zarejestruj się", "Lub graj jako gość")

### 12.2 Języki wspierane

- **Angielski (en)** - język podstawowy, domyślny
- **Polski (pl)** - język dodatkowy

### 12.3 Użycie

Wszystkie teksty w template są opakowane w pipe `i18n` lub używają serwisu `TranslateService`:
```typescript
{{ 'auth.login.title' | translate }}
{{ 'auth.login.email' | translate }}
{{ 'auth.login.password' | translate }}
```

### 12.4 Backend

Backend pozostaje bez zmian - wszystkie odpowiedzi API są w języku angielskim. Tłumaczenie komunikatów błędów z backendu na język użytkownika odbywa się po stronie frontendu.

## 13. Mapowanie historyjek użytkownika

- **US-003**: Logowanie zarejestrowanego użytkownika

