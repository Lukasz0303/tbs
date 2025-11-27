# Dokumentacja testów - World at War: Turn-Based Strategy

## Przegląd

Projekt wykorzystuje kompleksowe podejście do testowania, obejmujące testy jednostkowe, integracyjne oraz end-to-end (E2E) dla zarówno frontendu (Angular 17), jak i backendu (Spring Boot 3.x).

## Narzędzia testowe

### Frontend
- **Jest** - framework testowy dla testów jednostkowych
- **Angular Testing Library** - biblioteka do testowania komponentów Angular
- **Cypress** - framework do testów E2E

### Backend
- **JUnit 5** - framework testowy
- **Mockito** - biblioteka do mockowania zależności
- **Testcontainers** - narzędzie do testów integracyjnych z kontenerami Docker
- **RestAssured** - biblioteka do testowania REST API
- **Spring Boot Test** - narzędzia testowe Spring Boot

## Struktura katalogów

### Frontend
```
frontend/
├── src/
│   ├── app/
│   │   ├── **/*.spec.ts          # Testy jednostkowe komponentów i serwisów
│   └── test-setup.ts             # Konfiguracja środowiska testowego
├── cypress/
│   ├── e2e/                      # Testy E2E
│   │   ├── auth.cy.ts
│   │   └── game-flow.cy.ts
│   ├── support/
│   │   ├── commands.ts           # Własne komendy Cypress
│   │   └── e2e.ts                # Konfiguracja wsparcia
│   └── fixtures/                 # Dane testowe (opcjonalnie)
├── jest.config.js                # Konfiguracja Jest
└── cypress.config.ts             # Konfiguracja Cypress
```

### Backend
```
backend/
├── src/
│   └── test/
│       ├── java/
│       │   └── com/tbs/
│       │       ├── controller/           # Testy jednostkowe kontrolerów
│       │       ├── service/              # Testy jednostkowe serwisów
│       │       ├── security/             # Testy bezpieczeństwa
│       │       └── integration/          # Testy integracyjne
│       │           ├── BaseIntegrationTest.java
│       │           ├── AuthIntegrationTest.java
│       │           └── RankingIntegrationTest.java
│       └── resources/
│           └── application-test.properties  # Konfiguracja testowa
└── build.gradle                   # Konfiguracja zależności testowych
```

## Konfiguracja środowiska testowego

### Frontend

#### Wymagania
- Node.js 18+ 
- npm lub yarn

#### Instalacja zależności
```bash
cd frontend
npm install
```

#### Konfiguracja Jest
Plik `jest.config.js` zawiera konfigurację:
- Preset dla Angular (`jest-preset-angular`)
- Środowisko testowe: `jsdom`
- Mapowanie modułów
- Konfiguracja coverage

#### Konfiguracja Cypress
Plik `cypress.config.ts` zawiera:
- Base URL: `http://localhost:4200`
- Konfigurację viewport
- Timeouty dla komend
- Wsparcie dla testów komponentowych

### Backend

#### Wymagania
- Java 21
- Docker Desktop (dla Testcontainers)
- Gradle 8+

#### Konfiguracja Testcontainers
Testcontainers automatycznie uruchamia kontenery Docker dla:
- PostgreSQL 15 (baza danych)
- Redis 7 (cache)

**Uwaga:** Upewnij się, że Docker Desktop jest uruchomiony przed uruchomieniem testów integracyjnych.

#### Konfiguracja profilu testowego
Plik `application-test.properties` zawiera ustawienia:
- Wyłączenie automatycznego tworzenia schematu Hibernate
- Włączenie Flyway dla migracji
- Poziomy logowania

## Uruchamianie testów

### Frontend - Testy jednostkowe

#### Uruchomienie wszystkich testów
```bash
cd frontend
npm test
```

#### Tryb watch (automatyczne uruchamianie przy zmianach)
```bash
npm run test:watch
```

#### Testy z raportem pokrycia
```bash
npm run test:coverage
```

Raport HTML będzie dostępny w `frontend/coverage/index.html`

#### Testy w trybie CI
```bash
npm run test:ci
```

### Frontend - Testy E2E (Cypress)

#### Uruchomienie testów E2E (headless)
```bash
cd frontend
npm run e2e
```

#### Interaktywny tryb Cypress
```bash
npm run e2e:open
```

**Wymagania:**
- Aplikacja frontendowa musi być uruchomiona na `http://localhost:4200`
- Backend musi być dostępny i skonfigurowany

#### Uruchomienie w trybie headless
```bash
npm run e2e:headless
```

### Backend - Testy jednostkowe

#### Uruchomienie wszystkich testów
```bash
cd backend
./gradlew test
```

Na Windows:
```bash
gradlew.bat test
```

#### Tylko testy jednostkowe (oznaczone tagiem `@Tag("unit")`)
```bash
./gradlew testUnit
```

#### Tylko testy integracyjne (oznaczone tagiem `@Tag("integration")`)
```bash
./gradlew testIntegration
```

**Uwaga:** Testy integracyjne wymagają uruchomionego Docker Desktop.

#### Testy z raportem
```bash
./gradlew test --info
```

Raporty znajdują się w `backend/build/reports/tests/test/index.html`

### Backend - Testy integracyjne z Testcontainers

Testy integracyjne automatycznie:
1. Uruchamiają kontenery Docker (PostgreSQL, Redis)
2. Konfigurują połączenia do kontenerów
3. Uruchamiają migracje Flyway
4. Wykonują testy
5. Czyszczą kontenery po zakończeniu

**Przykład uruchomienia:**
```bash
cd backend
./gradlew testIntegration
```

## Przykładowe testy

### Frontend - Test jednostkowy komponentu

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { ButtonSoundService } from './services/button-sound.service';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let buttonSoundService: jest.Mocked<ButtonSoundService>;

  beforeEach(async () => {
    buttonSoundService = {
      register: jest.fn(),
      unregister: jest.fn(),
    } as unknown as jest.Mocked<ButtonSoundService>;

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        { provide: ButtonSoundService, useValue: buttonSoundService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });
});
```

### Frontend - Test serwisu

```typescript
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should load current user successfully', (done) => {
    const mockResponse = { userId: 1, username: 'testuser', ... };

    service.loadCurrentUser().subscribe((user) => {
      expect(user).toBeTruthy();
      done();
    });

    const req = httpMock.expectOne(`${apiUrl}/v1/auth/me`);
    req.flush(mockResponse);
  });
});
```

### Frontend - Test E2E (Cypress)

```typescript
describe('Authentication Flow', () => {
  beforeEach(() => {
    cy.visit('/');
  });

  it('should display login form', () => {
    cy.visit('/auth/login');
    cy.get('[data-cy="username-input"]').should('be.visible');
    cy.get('[data-cy="password-input"]').should('be.visible');
  });
});
```

### Backend - Test jednostkowy serwisu

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserProfile_shouldReturnProfileForRegisteredUser() {
        Long userId = 1L;
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(registeredUser));

        UserProfileResponse response = userService.getUserProfile(userId, currentUserId);

        assertThat(response).isNotNull();
        verify(userRepository, times(1)).findById(userId);
    }
}
```

### Backend - Test integracyjny z Testcontainers

```java
class AuthIntegrationTest extends BaseIntegrationTest {
    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    void shouldRegisterNewUser() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"username\": \"testuser\", ...}")
            .when()
            .post("/v1/auth/register")
            .then()
            .statusCode(201)
            .body("userId", notNullValue());
    }
}
```

## Best practices

### Frontend
1. **Używaj Angular Testing Library** zamiast bezpośredniego dostępu do DOM
2. **Mockuj zależności** zewnętrzne (HTTP, serwisy)
3. **Testuj zachowanie**, nie implementację
4. **Używaj data-cy** atrybutów dla selektorów w testach E2E
5. **Izoluj testy** - każdy test powinien być niezależny

### Backend
1. **Oznaczaj testy tagami** (`@Tag("unit")`, `@Tag("integration")`)
2. **Używaj Testcontainers** dla testów integracyjnych z bazą danych
3. **Mockuj zależności zewnętrzne** w testach jednostkowych
4. **Testuj warstwy osobno** - kontrolery, serwisy, repozytoria
5. **Używaj `@Transactional`** w testach integracyjnych dla izolacji

## Troubleshooting

### Frontend

**Problem:** Testy nie znajdują modułów Angular
**Rozwiązanie:** Sprawdź konfigurację `jest.config.js` i `tsconfig.spec.json`

**Problem:** Cypress nie może połączyć się z aplikacją
**Rozwiązanie:** Upewnij się, że aplikacja działa na `http://localhost:4200`

### Backend

**Problem:** Testcontainers nie może uruchomić kontenerów
**Rozwiązanie:** 
- Sprawdź, czy Docker Desktop jest uruchomiony
- Sprawdź, czy masz wystarczająco miejsca na dysku
- Sprawdź logi Docker

**Problem:** Testy integracyjne są wolne
**Rozwiązanie:** 
- Użyj `@Container(shared = true)` dla współdzielonych kontenerów
- Rozważ użycie `@Testcontainers` z `@Container(shared = true)`

## CI/CD

### GitHub Actions

Przykładowa konfiguracja dla CI/CD:

```yaml
- name: Run Frontend Tests
  run: |
    cd frontend
    npm ci
    npm run test:ci

- name: Run Backend Tests
  run: |
    cd backend
    ./gradlew test
```

## Pokrycie kodem

### Frontend
```bash
cd frontend
npm run test:coverage
```

Raport: `frontend/coverage/index.html`

### Backend
```bash
cd backend
./gradlew test jacocoTestReport
```

Raport: `backend/build/reports/jacoco/test/html/index.html`

## Dodatkowe zasoby

- [Jest Documentation](https://jestjs.io/docs/getting-started)
- [Angular Testing Library](https://testing-library.com/angular)
- [Cypress Documentation](https://docs.cypress.io/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [RestAssured Documentation](https://rest-assured.io/)

