# Post na LinkedIn — propozycja do publikacji

„Dzień dobry, dzwonię z banku”. Ale czy to naprawdę Ty?

Wokół tego pytania rozwijam CzyToTy? — prototyp na Androida, w którym telefon i SMS-y łączą się z informacją o tym, co wiemy o numerze i dlaczego wiadomość powinna wzbudzić ostrożność.

W demo pokazuję mechanizm na konkretnych sytuacjach:

• SMS podszywający się pod bank: aplikacja wyodrębnia domenę i sprawdza ją względem zapisanej listy. Osobno wskazuje wzmiankę o haśle i presję czasu.
• Nieznany numer: dostajemy informację o braku danych, zamiast automatycznej etykiety „bezpieczny”.
• Numer opisany jako bank: widać, że to własne oznaczenie użytkownika, a tożsamość rozmówcy pozostaje niepotwierdzona.
• Własna blokada: lokalna usługa sprawdza zapis w SQLite i może zlecić Androidowi odrzucenie połączenia po włączeniu odpowiedniej roli.

W aplikacji źródłem domen jest rzeczywista Lista Ostrzeżeń CERT Polska. Wiadomości są analizowane lokalnie, bez wysyłania treści do AI. W publicznym demo używam fikcyjnego SMS-a i testowej listy domen, żeby pokazać ten sam kod bez prywatnych danych i aktywnych podejrzanych linków.

To nadal prototyp. Nie ma jeszcze integracji potwierdzającej, że po drugiej stronie rzeczywiście jest bank, ani podłączonej bazy reputacji numerów. Ten etap wymaga wiarygodnego źródła danych i osobnej walidacji.

A codzienna obsługa? Jasny i ciemny motyw, własne kolory, przypinanie rozmów, wyszukiwanie i lokalne szkice. Wygoda ma pomagać korzystać z ochrony.

Jakiej informacji najbardziej brakuje Wam przy podejrzanym połączeniu: kto dzwoni, skąd pochodzi ocena ryzyka czy co zrobić dalej?

#Cyberbezpieczeństwo #FraudPrevention #Android #UX #BudowanieProjektów

---

Status: przygotowany szkic, nie opublikowano. Tekst opiera się na kodzie tej iteracji; nie deklaruje pełnej zgodności funkcjonalnej z Google Messages lub Apple Messages ani zakończonych testów operatora.
