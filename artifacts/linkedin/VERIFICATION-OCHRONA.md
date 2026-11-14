# CzyToTy? 0.6.0 - mechanizm ochrony, 2026-09-08

Końcowy `scripts/build.ps1`: BUILD SUCCESSFUL w 50 s. 35 testów JVM/Robolectric, 0 porażek i błędów. Android Lint: 0 błędów, 40 ostrzeżeń. Wyniki pochodzą z aktywnego katalogu `build/prototype-app`; historyczny `app/build` nie dotyczy tego APK.

SHA-256 APK: `E86C40BA78D2A8DD13BD0DEF3A4D346A0A8F2F3149E62A94FC70B0225A06F36B`.

Dodano kartę źródła informacji o numerze do edytora i rzeczywistego ekranu połączenia. Własna etykieta nie potwierdza tożsamości rozmówcy. Decyzja odrzucenia połączenia korzysta ze wspólnej funkcji testowanej także w demo blokady.

Cztery nowe testy renderują ujęcia 07-10: podejrzany SMS bankowy, brak danych, ekran połączenia i własna blokada. Obrazy obejrzano; ujęcie 08 poprawiono i ponownie wyrenderowano, by zachować pełny nagłówek.

W ujęciach zastosowano syntetyczne numery, wiadomość i domeny `.example`. Lista DEMO służy wyłącznie testowi prawdziwego mechanizmu sprawdzania; nie jest aktualną listą CERT ani zgłoszeniem rzeczywistego banku. Testowe notatki są usuwane po testach. Dane demo nie są dodane do produkcyjnego APK.

Nie wykonano rzeczywistych połączeń, wysyłki SMS ani testu na podłączonym telefonie. Nie dodano dostawcy reputacji numerów ani weryfikacji banku. Historyczny raport VERIFICATION-0.6 opisuje wcześniejszy APK; niniejszy dokument podaje aktualny hash i wyniki.
