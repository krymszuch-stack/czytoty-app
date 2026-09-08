# Kontrola danych — 8 września 2026

## Supabase — sprawdzone na żywo

Projekt `czytoty-app`, ref `ouszurtddchzztdcyrdx`, Frankfurt, PostgreSQL 17.6.1.166. Integracja zwróciła `ACTIVE_HEALTHY`.

- Obie tabele publiczne, `data_sources` i `private_notes`, mają włączone RLS.
- `data_sources`: anonimowy i zalogowany klient mają wyłącznie SELECT. Publiczna polityka odczytu pasuje do katalogu źródeł.
- `private_notes`: anonimowy klient nie ma uprawnień; zalogowany ma SELECT/INSERT/UPDATE/DELETE z warunkiem właściciela `auth.uid() = user_id`. UPDATE ma zarówno USING, jak i WITH CHECK.
- Klucz `(user_id, phone)`, powiązanie do użytkownika oraz limity długości etykiety i notatki istnieją w bazie. Bieżąca reguła numeru zaakceptowała zarezerwowany numer testowy `+12025550123` w zapytaniu bez zapisu.
- Historia migracji zawiera `20260907123736 / prototype_private_notes`, zgodnie z zakresem repozytorium.
- Doradcy bezpieczeństwa i wydajności zwrócili puste listy zgłoszeń.
- Sprawdzenie rzeczywistym kluczem klienckim z konfiguracji: katalog źródeł HTTP 200 (1 rekord), anonimowy odczyt prywatnych notatek HTTP 401. Sekrety nie zostały zapisane w raporcie.

Nie modyfikowano chmury ani danych użytkowników. Nie ponawiano rejestracji, logowania i pełnego CRUD dwóch kont; kontrola schematu i uprawnień nie jest zastępstwem takich testów.

## CERT — sprawdzone na żywo

Oficjalny feed `https://hole.cert.pl/domains/v2/domains.txt`: HTTP 200, 138 270 niepustych wpisów podczas tej kontroli. Liczba może się zmieniać. Katalog Supabase wskazuje ten sam feed i interwał 300 sekund.

## SQLite — kod i testy lokalne

Istniejące bazy notatek/blokad i potwierdzeń wysyłki pozostają oddzielone od systemowego dostawcy SMS. Dodano `sms-conversations.db` z tabelami `drafts` i `pinned_threads`. Nie zmieniano istniejących baz ani ich wersji. Nowa baza zaczyna się pusta.

Zapisy szkiców korzystają z parametrów/ContentValues i atomowego zastąpienia jednego klucza. Kolejka zapisów działa poza głównym wątkiem, zachowuje kolejność i jest opróżniana po zamknięciu modelu ekranu. Błędy zapisu są pokazywane w UI. Treść jest ograniczona do 2000 znaków. Szkice i przypięcia nie trafiają do chmury ani do skrzynki Androida.

Testy tej iteracji sprawdzają ponowne otwarcie bazy, odseparowanie szkiców, zastępowanie i usuwanie wpisu, przypięcia, polskie znaki, odrzucenie błędnego klucza/zbyt długiego tekstu oraz `PRAGMA integrity_check`. Wyszukiwanie obejmuje starszą treść, zachowując najnowszy podgląd wątku.

Fizyczny telefon nie był podłączony. Nie wykonano `integrity_check` na jego prywatnych bazach. Wyniki uruchomienia testów i kompilacji znajdują się w `VERIFICATION-0.6.md`.

Pozostała obserwacja: istniejący magazyn potwierdzeń SMS pokazuje 10 ostatnich zleceń, lecz nie ma retencji starych rekordów. W tej iteracji nie dodawano automatycznego usuwania historii.
