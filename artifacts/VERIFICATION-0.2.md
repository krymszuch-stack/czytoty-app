# Weryfikacja prototypu — 2026-09-07

## Artefakt

- APK: `CzyToTy-prototype.apk`
- Wersja: `0.2.0-prototype`, versionCode 2.
- Identyfikator: `com.aistudio.ochronapolaczen.ktwzpd.prototype`.
- Rozmiar: 20 669 237 bajtów.
- SHA-256: `4663219046FA0BF1DE7E61959FC80F063739CAF9A6641235FB70A60EEE9350DA`.
- Końcowy APK zainstalowany na podłączonym Samsungu SM-S937B, Android 16, jako osobna aplikacja **CzyToTy? Prototyp**.

## Wyniki

| Kontrola | Wynik | Faktycznie sprawdzony zakres |
|---|---|---|
| Kompilacja debug APK | PASS | Gradle assembleDebug, rzeczywista instalacja przez ADB |
| Testy jednostkowe | 17/17 PASS | Dopasowanie domen i subdomen, IDN, userinfo w URL, brak i starzenie danych, normalizacja numerów, trwałość SQLite, ograniczenie historii, atomowa podmiana listy, zachowanie cache po błędzie HTTP i niepoprawnej odpowiedzi |
| Testy na telefonie | 4/4 PASS | Kontekst aplikacji, zapis i ponowny odczyt notatki, rzeczywiste pobranie listy CERT i wynik analizy w interfejsie, odczyt katalogu źródeł przez Supabase SDK |
| Android Lint | 0 błędów, 42 ostrzeżenia | Pozostały głównie sugestie aktualizacji zależności, niewykorzystane zasoby i uwagi stylistyczne. Nie jest to raport bez ostrzeżeń |
| Supabase REST | PASS | Publiczny katalog źródeł zwrócił HTTP 200; anonimowy odczyt prywatnych notatek odrzucony HTTP 401 |
| Supabase Auth i notatki | PASS | Logowanie na syntetycznym, potwierdzonym koncie testowym; insert, upsert, odczyt i usunięcie notatki przez REST; wylogowanie |
| Izolacja użytkowników | PASS | Własny odczyt/zapis; odmowa przypisania rekordu innemu użytkownikowi; brak odczytu, aktualizacji i usuwania cudzych rekordów |
| Supabase Security Advisors | PASS | Brak zgłoszonych problemów po migracji |
| Kontrola wizualna | PASS w sprawdzonym zakresie | Ekran sprawdzania wiadomości i ekran konta na telefonie; poprawione jasne ikony paska systemowego |
| Przycisk połączenia z bazą | PASS | Na telefonie uzyskano komunikat „Połączono z bazą czytoty-app” |
| Kontrola zmian Git | PASS | git diff --check |

Testy używały danych syntetycznych. Konto testowe zostało wylogowane i usunięte; SQL do sprawdzenia izolacji wykonano w wycofanej transakcji. Nie wysłano SMS-ów ani wiadomości e-mail do użytkownika w ramach tych testów.

## Potwierdzone źródło

W aplikacji pobrano rzeczywisty plik `https://hole.cert.pl/domains/v2/domains.txt`. Pierwsza zaobserwowana kopia zawierała 138 278 domen, końcowy zrzut interfejsu pokazywał 138 267. Liczba zmienia się wraz z listą źródłową; nie jest wpisana na stałe do programu. Sprawdzanie domen nie odwiedza zgłoszonych witryn.

Zrzuty ekranu: `phone-home.png`, `phone-cloud.png`.

## Granice wykonanej weryfikacji

- **Nie wykonano prawdziwego połączenia od operatora** w celu potwierdzenia jego odrzucenia. Wymaga to nadania roli filtrowania na telefonie i połączenia z drugiego urządzenia. Nie zastąpiono tego testu sztucznym zdarzeniem w produkcyjnym rejestrze.
- Test logowania używał potwierdzonego konta technicznego. **Nie sprawdzono dostarczenia maila rejestracyjnego ani przejścia użytkownika przez link aktywacyjny.** Rejestracja zależy od konfiguracji i limitów usługi e-mail Supabase.
- Nie wysyłano prawdziwego SMS-a do CERT. Przycisk ma otwierać systemowy formularz SMS, a użytkownik sam zatwierdza wysłanie.
- Kopia offline i awarie pobierania są objęte testami kodu; nie zmieniano globalnie połączenia Wi-Fi ani transmisji danych na telefonie.
- Testy nie stanowią audytu penetracyjnego, testu obciążenia ani potwierdzenia zgodności ze wszystkimi producentami telefonów.
- Historyczna prezentacja biznesowa nie jest specyfikacją wykonanych funkcji.

## Test ręczny filtrowania

1. W zakładce **Numery** dodaj numer drugiego telefonu do własnej listy blokad i zapisz.
2. W **Ochrona** nadaj aplikacji rolę identyfikowania/filtrowania połączeń i pozostaw włączony przełącznik blokowania.
3. Zadzwoń z drugiego telefonu. Jeśli jest zapisany w systemowych kontaktach, Android może nie przekazać połączenia tej usłudze.
4. Sprawdź faktyczne odrzucenie i wpis w rejestrze. Usuń blokadę testową po zakończeniu.

Pełne instrukcje i ograniczenia: `../README.md`.
