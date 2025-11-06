# API Endpoint Implementation Plan: DELETE /api/v1/rankings/cache

> **Status:** ✅ Zaimplementowane

## 1. Przegląd punktu końcowego

DELETE /api/v1/rankings/cache - Czyszczenie wszystkich wpisów cache rankingów z Redis. Endpoint przydatny po zmianach schematu bazy danych, problemach z cache lub przy restartowaniu aplikacji. Automatycznie wywoływany przez skrypt `run-backend.ps1` przy opcjach `start` i `restart`.

## 2. Szczegóły żądania

- **Metoda HTTP:** DELETE
- **URL:** `/api/v1/rankings/cache`
- **Autoryzacja:** Publiczne (bez wymaganej autoryzacji)

### Path Parameters
Brak

### Query Parameters
Brak

### Request Body
Brak

## 3. Wykorzystywane typy

- Response: `Void` (204 No Content lub 200 OK z pustym body)

## 4. Szczegóły odpowiedzi

- **200 OK** - Cache wyczyszczony pomyślnie

**Response Body:**
Brak (pusty body)

## 5. Przepływ danych

1. **Kontroler** przyjmuje żądanie DELETE
2. **Service Layer** (`RankingService`):
   - Wywołuje metodę `clearRankingsCache()` z adnotacją `@CacheEvict`
   - Adnotacja `@CacheEvict(value = {"rankings", "rankingDetail", "rankingsAround"}, allEntries = true)` czyści wszystkie wpisy z cache Redis dla:
     - `rankings` - lista rankingów
     - `rankingDetail` - szczegóły rankingu użytkownika
     - `rankingsAround` - rankingi wokół użytkownika
3. **Odpowiedź** - zwraca `200 OK` z pustym body

## 6. Względy bezpieczeństwa

1. **Autoryzacja**: Endpoint publiczny - czyszczenie cache jest operacją administracyjną
2. **Ochrona przed nadużyciem**: Rozważenie dodania rate limitingu lub autoryzacji dla tego endpointu w produkcji
3. **Idempotentność**: Operacja jest idempotentna - wielokrotne wywołanie nie powoduje błędów

## 7. Obsługa błędów

### 500 Internal Server Error
**Błąd podczas czyszczenia cache:**
```json
{
  "timestamp": "2024-03-15T14:30:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An error occurred while clearing cache",
  "path": "/api/v1/rankings/cache"
}
```

## 8. Rozważania dotyczące wydajności

1. **Cache (Redis)**: Operacja czyszczenia cache jest natychmiastowa i nie wpływa na wydajność zapytań
2. **Automatyczne odświeżanie**: Po wyczyszczeniu cache, nowe zapytania automatycznie odbudują cache z aktualnymi danymi
3. **TTL**: Nowe wpisy cache będą miały standardowy TTL (300 sekund dla rankingów)

## 9. Użycie w skrypcie PowerShell

Endpoint jest automatycznie wywoływany przez skrypt `run-backend.ps1` przy opcjach `start` i `restart`:

```powershell
function Clear-RedisCache {
    $null = Write-ColorOutput -ForegroundColor Cyan "[INFO] Czyszczenie cache Redis..."
    
    $maxRetries = 5
    $retryDelay = 2
    $retries = 0
    $success = $false
    
    while ($retries -lt $maxRetries -and -not $success) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/rankings/cache" -Method Delete -TimeoutSec 5 -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                $null = Write-ColorOutput -ForegroundColor Green "[OK] Cache Redis wyczyszczony pomyślnie"
                $success = $true
                return $true
            }
        } catch {
            $retries++
            if ($retries -lt $maxRetries) {
                $null = Write-ColorOutput -ForegroundColor Yellow "[INFO] Próba $retries/$maxRetries - Aplikacja może nie być jeszcze gotowa, czekam $retryDelay sekund..."
                Start-Sleep -Seconds $retryDelay
            } else {
                $null = Write-ColorOutput -ForegroundColor Yellow "[WARN] Nie udało się wyczyścić cache Redis (aplikacja może nie być jeszcze uruchomiona)"
                $null = Write-ColorOutput -ForegroundColor Yellow "[INFO] Cache zostanie automatycznie odświeżony przy pierwszym zapytaniu"
                return $false
            }
        }
    }
    
    return $false
}
```

## 10. Przykład użycia (curl)

```bash
curl -X DELETE http://localhost:8080/api/v1/rankings/cache
```

## 11. Etapy wdrożenia

1. ✅ Utworzenie metody `clearRankingsCache()` w interfejsie `RankingService`
2. ✅ Implementacja metody w `RankingServiceImpl` z adnotacją `@CacheEvict`
3. ✅ Utworzenie endpointu `DELETE /api/v1/rankings/cache` w `RankingController`
4. ✅ Dodanie adnotacji Swagger dla dokumentacji API
5. ✅ Integracja ze skryptem PowerShell `run-backend.ps1`
6. ✅ Testy jednostkowe dla Service i Controller (JUnit 5, Mockito)
7. ⏳ Testy integracyjne i E2E (Cypress)

