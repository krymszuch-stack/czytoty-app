# CzyToTy? 3w1 — weryfikacja 2026-09-07

## Końcowy artefakt

- APK: `CzyToTy-prototype.apk`, wersja `0.3.0-prototype`, versionCode 3.
- Pakiet: `com.aistudio.ochronapolaczen.ktwzpd.prototype`.
- SHA-256: `C6AECB2F57D7412A4426E106ACD53854108FF8991459D13B8B164E3E7C79AA56`.
- Zainstalowany przez ADB na Samsungu SM-S937B, Android 16. Wersję potwierdzono przez Package Manager.
- Na prośbę użytkownika ustawiono pozostawianie ekranu aktywnego podczas USB (`stay_on_while_plugged_in=2`). Telefon potwierdził `mStayOn=true`.
- Logo i ikony: `CzyToTy-logo-ikony.zip`, pliki źródłowe i eksporty w `brand/`.

## Wyniki bieżącej kontroli

| Kontrola | Wynik | Zakres |
|---|---|---|
| assembleDebug | PASS | Końcowy APK skompilowany, zainstalowany i uruchomiony |
| testDebugUnitTest | 20/20 PASS | CERT, numery, SQLite i potwierdzenia wysyłki SMS |
| connectedDebugAndroidTest | 5/5 PASS | Rzeczywisty feed CERT i analiza w UI, katalog źródeł Supabase przez SDK, zapis notatki, szkic SMS i zachowanie go po przejściu między zakładkami, kontekst Android |
| lintDebug | 0 błędów, 38 ostrzeżeń | Pozostały sugestie aktualizacji zależności, niewykorzystane zasoby i uwagi stylistyczne |
| Kontrola wizualna | PASS w sprawdzonym zakresie | Ekrany Telefon i SMS z nowym znakiem, ikonami i czytelną nawigacją |
| git diff --check | PASS | Brak błędów białych znaków w śledzonych zmianach |

Nowe testy transmisji sprawdzają, że duplikaty i nieznane potwierdzenia nie zawyżają liczby wysłanych części, błąd części i timeout nie stają się sukcesem, a zapis potwierdzeń przetrwa ponowne otwarcie bazy. Nie wykorzystują modemu ani rzeczywistych odbiorców.

Pięć testów urządzenia uruchomiono przed końcową, jednoliniową poprawką sprawdzania identyfikatora SIM dla Androida 8–9. Po niej ponownie wykonano kompilację, 20 testów lokalnych, lint oraz instalację i kontrolę wizualną końcowego APK. Testy urządzenia nie wykonywały wysyłki SMS. Narzędzie testowe zgłosiło pomocnicze ostrzeżenie o braku pakietu `androidx.test.services`; raport XML potwierdza 5 testów, 0 błędów i 0 porażek.

Zrzuty końcowego APK: `phone-3w1.png`, `phone-3w1-sms.png`. Pokazują widoki bez dostępu do prywatnej skrzynki, kontaktów i historii użytkownika.

## Co zostało podłączone

- Telefon: klawiatura, opcjonalny odczyt systemowej historii i kontaktów, przekazanie numeru do systemowego telefonu, notatki, blokady i przejście do SMS. Szybka blokada zachowuje istniejącą notatkę.
- SMS: odczyt systemowego dostawcy danych w tle, wątki, obserwowanie zmian, szkic, świadome zatwierdzenie wysyłki, domyślna SIM i trwałe potwierdzenia części wiadomości. Przyznanie uprawnienia samo nie wysyła SMS-a.
- Ostrzeżenia: lokalna analiza tekstu i domen, rzeczywista lista CERT, odbiornik przychodzących SMS-ów i powiadomienie o sygnałach ryzyka po uzyskaniu zgód. Powiadomienie nie zawiera treści ani nadawcy SMS-a.
- Supabase: pozostaje osobny projekt `czytoty-app` (`ouszurtddchzztdcyrdx`). Aktualny test SDK potwierdził odczyt katalogu źródeł.
- Branding: wektorowe odtworzenie dostarczonego kierunku, adaptacyjna ikona Androida, wariant monochromatyczny, logo poziome i ikony interfejsu.

## Granice weryfikacji i prototypu

- Nie wysłano prawdziwego SMS-a i nie sprawdzono jego doręczenia przez operatora. Nie wykonano testu przyjścia SMS-a z drugiego telefonu ani powiadomienia o takim SMS-ie. Kod i testy potwierdzeń nie zastępują tych prób.
- Nie nadawano automatycznie dostępu do prywatnych SMS-ów, kontaktów i historii. Odczyt prawdziwej skrzynki użytkownik włącza systemowym oknem zgody.
- Nie wykonano połączenia od operatora w celu sprawdzenia odrzucenia przez CallScreeningService. Wymaga to aktywnej roli filtrowania i drugiego telefonu.
- Aplikacja nie jest pełnym zamiennikiem domyślnego telefonu ani komunikatora SMS/MMS/RCS. Rozmowę prowadzi systemowy telefon; przychodzące wiadomości zapisuje obecna domyślna aplikacja SMS. Brak obsługi MMS/RCS, usuwania i oznaczania SMS jako przeczytane.
- Nie ma podłączonej uniwersalnej bazy reputacji numerów ani potwierdzania tożsamości rozmówcy. Lista CERT dotyczy domen; reguły tekstowe są wskazówkami, nie wyrokiem o fraudzie.
- Supabase Auth/CRUD/RLS i izolację użytkowników sprawdzono w poprzednim etapie. Szczegółowy historyczny raport: `VERIFICATION-0.2.md`. W tym wznowieniu nie tworzono kolejnego projektu ani konta i nie ponawiano testów rejestracyjnego e-maila.

Implementację SIM sprawdzono również z [dokumentacją Android SubscriptionManager](https://developer.android.com/reference/android/telephony/SubscriptionManager). Instrukcje budowania, uprawnienia, limity i źródła: `../README.md`.
