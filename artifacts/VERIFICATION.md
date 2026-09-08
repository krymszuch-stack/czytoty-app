# CzyToTy? 3w1 — weryfikacja 2026-09-08

## Końcowy artefakt

- APK: `CzyToTy-prototype.apk`, wersja `0.5.0-prototype`, versionCode 5.
- Pakiet: `com.aistudio.ochronapolaczen.ktwzpd.prototype`.
- SHA-256: `769EEF5A9F8EFC7DF241D29612B47C3281B8AD3B14A6AF1080AF2DC475B0E7DD`.
- Końcowy APK zainstalowany i uruchomiony na Samsungu SM-S937B, Android 16. Po testach usunięto wyłącznie pakiet testowy.
- Ekran pozostaje aktywny podczas USB na prośbę użytkownika (stay_on_while_plugged_in=2, mStayOn=true).

## Wyniki

| Kontrola | Wynik |
|---|---|
| assembleDebug | PASS |
| testDebugUnitTest | 23 testy, 0 błędów i porażek |
| Instrumentacja końcowego APK | OK (9 tests), 10,004 s |
| lintDebug | 0 błędów, 39 ostrzeżeń |
| Kontrola wizualna | Klawiatura, syntetyczne dymki i połączenie przychodzące sprawdzone po poprawce kontrastu |
| git diff --check | PASS; komunikaty LF/CRLF dotyczą konwersji na Windows |

Testy obejmują CERT, numery, SQLite, potwierdzenia SMS, normalizację dla Telecom, rzeczywisty katalog Supabase przez SDK, lokalną notatkę, szkic SMS, zachowanie stanu zakładek, klawiaturę oraz syntetyczne stany rozmowy i dymków. Testy nie wykonują transmisji przez operatora. Pomocnicze ostrzeżenie infrastruktury o braku androidx.test.services nie spowodowało porażki testów.

## Wdrożone

- Telefon: Ostatnie / Kontakty / Klawiatura, duże okrągłe klawisze, litery, haptyka, animacja naciśnięcia, przytrzymanie 0 dla + i przejście do SMS.
- SMS: wyszukiwanie, filtr nieprzeczytanych przychodzących, nazwy kontaktów po zgodzie, awatary, dymki, grupowanie dat, pole pisania na dole, szkice osobno dla wątków. Statusy nie udają doręczenia.
- Połączenia: natywny InCallService, konfiguracja roli Telefon, ekran przychodzący i aktywny, powrót do rozmowy, DTMF oraz akcje dźwięku/SIM/zawieszania/łączenia udostępniane przez Telecom. Kod prototypowy, bez testu operatora.
- Ochrona: rzeczywista lista domen CERT, lokalne reguły, notatki i blokady, bez fikcyjnej reputacji numerów.
- Supabase: osobny projekt czytoty-app (ouszurtddchzztdcyrdx). Bieżący test SDK potwierdził katalog źródeł. Schemat pozostał bez zmian.

## Zrzuty

- `phone-final.png`: klawiatura końcowego APK bez prywatnej historii i kontaktów.
- `sms-final.png`: Wiadomości końcowego APK.
- `visual-qa-final/synthetic-incoming.png`: syntetyczne połączenie z zarezerwowanym numerem testowym, nie połączenie operatora.
- `visual-qa-final/synthetic-message-bubbles.png`: jawnie oznaczone syntetyczne wiadomości tylko w teście UI. Niczego nie wysłano ani nie zapisano do systemowej skrzynki.

Starsze zrzuty bez przyrostka final i katalog visual-qa dokumentują wcześniejsze iteracje, nie końcowy wygląd 0.5. Branding: CzyToTy-logo-ikony.zip oraz brand/.

## Granice prototypu

- Nie wykonano rzeczywistego połączenia ani wysyłki/odbioru SMS z drugim telefonem. Testy nie potwierdzają modemu, mikrofonu, Bluetooth, dual-SIM ani działania przy zablokowanym ekranie.
- Nie zmieniano automatycznie domyślnego Telefonu. Ostatni odczyt wskazywał com.samsung.android.dialer. Użytkownik może wybrać CzyToTy przez Telefon → ⓘ → Ustaw jako domyślny Telefon; przed codziennym użyciem potrzebne są rzeczywiste próby.
- Nie nadawano automatycznie dostępu do prywatnych SMS, kontaktów i historii.
- Wiadomości nie przejmują jeszcze roli domyślnej aplikacji SMS. Brak MMS/RCS, usuwania i oznaczania jako przeczytane; wiadomości przychodzące zapisuje obecna domyślna aplikacja SMS. Nie jest to jeszcze pełny zamiennik systemowych Wiadomości.
- Brak kompletnej obsługi wideo/RTT; odbieranie w prototypie jest audio.
- CERT opisuje domeny, nie tożsamość i reputację wszystkich numerów. Reguły tekstowe wskazują powody ostrożności, nie rozstrzygają o oszustwie.
- Auth/CRUD/RLS Supabase sprawdzono wcześniej (VERIFICATION-0.2.md). Nie ponawiano teraz prób maili rejestracyjnych ani izolacji kont.

Cel i pozostałe wymagania: ../PRODUCT.md. Instrukcje: ../README.md. Kierunek wizualny: ../DESIGN.md.
