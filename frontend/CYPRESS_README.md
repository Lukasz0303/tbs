# Cypress E2E Tests

## Wymagania

Przed uruchomieniem testów e2e, upewnij się, że:

1. **Aplikacja frontend jest uruchomiona** na `http://localhost:4200`
   ```bash
   npm start
   ```

2. **Backend jest uruchomiony** (jeśli testy wymagają połączenia z API)

## Uruchamianie testów

### W trybie headless (CI/CD)
```bash
npm run e2e
```

### W trybie interaktywnym (development)
```bash
npm run e2e:open
```

## Rozwiązywanie problemów

### Błąd: "Cannot find element"
- Upewnij się, że aplikacja jest uruchomiona na `http://localhost:4200`
- Sprawdź czy komponenty mają atrybuty `data-cy` w szablonach HTML

### Błąd: "Timed out retrying"
- Zwiększ timeout w `cypress.config.ts` lub w konkretnym teście
- Sprawdź czy aplikacja odpowiada poprawnie

### Błąd: "EBUSY: resource busy or locked"
- Zamknij wszystkie procesy Cypress
- Wyczyść cache: `npx cypress cache clear`
- Usuń folder `node_modules/cypress` i zainstaluj ponownie

