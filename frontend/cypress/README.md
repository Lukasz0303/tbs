# Cypress E2E Tests

## Struktura

- `e2e/` - Testy end-to-end dla głównych user journeys
- `support/` - Wsparcie i własne komendy Cypress
- `fixtures/` - Dane testowe (opcjonalnie)

## Uruchamianie

```bash
# Interaktywny tryb
npm run e2e:open

# Headless mode
npm run e2e

# Tylko headless
npm run e2e:headless
```

## Wymagania

- Aplikacja frontendowa musi być uruchomiona na `http://localhost:4200`
- Backend musi być dostępny i skonfigurowany

## Własne komendy

- `cy.login(username, password)` - Logowanie użytkownika
- `cy.logout()` - Wylogowanie użytkownika

## Best Practices

1. Używaj atrybutów `data-cy` dla selektorów
2. Testuj user journeys, nie implementację
3. Izoluj testy - każdy test powinien być niezależny
4. Używaj fixtures dla danych testowych

