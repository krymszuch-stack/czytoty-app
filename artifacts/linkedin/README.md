# CzyToTy? — materiały do LinkedIn

Przygotowane 8 września 2026. Niczego nie opublikowano.

- [Post do skopiowania](POST-LINKEDIN.md)
- [Opis projektu i technologii](OPIS-PROJEKTU.md)

## Obrazy demo

Sześć PNG w katalogu `demo/`, w rozdzielczości 822 × 1782. Proponowana kolejność załączników:

| Plik | Co pokazuje | Tekst alternatywny |
|---|---|---|
| `01-wiadomosci-ciemne.png` | Lista w ciemnym motywie | CzyToTy: ciemna lista przykładowych rozmów, oznaczenia szkiców i przypięć |
| `02-wiadomosci-jasne.png` | Zwarta lista, akcent oceaniczny | Jasny motyw CzyToTy z niebieskim akcentem i zwartą listą rozmów |
| `03-rozmowa-mieta.png` | Dymki i akcent miętowy | Przykładowa rozmowa SMS; odebrane i wysłane wiadomości wyrównane do przeciwnych stron |
| `04-telefon.png` | Rzeczywisty ekran panelu Telefon | Panel Telefon w CzyToTy z zakładkami Ostatnie, Kontakty i Klawiatura |
| `05-personalizacja.png` | Panel „Po Twojemu” | Ustawienia motywu, akcentu, tekstu wiadomości, zwartości, dymków i wibracji |
| `06-edytor-sms.png` | Rzeczywisty edytor SMS | Szkic SMS do zarezerwowanego numeru testowego z licznikiem części i przyciskiem wysyłki |

**Pochodzenie:** obrazy wygenerował test `CommunicationDemoTest` z komponentów Compose aplikacji przez natywny Android Canvas w Robolectric. To renderowanie UI na Windows, nie zdjęcia fizycznego telefonu. Ekrany 01–03 mają kompozycję demonstracyjną z prawdziwymi komponentami wiersza/dymków, 04 korzysta z panelu `DialerTab`, 05 z `AppearanceControls`, a 06 z `SmsTabScrollable`. Dodatkowy napis DEMO pochodzi z ramki testowej.

Treści, imiona i numer są syntetyczne; nigdy nie trafiły do systemowej skrzynki, kontaktów ani operatora. Nie użyto grafik generowanych przez AI. Piktogram tarczy w nagłówku demo jest ikoną Material, a nie eksportem firmowego logo. Status „wysłano” w przykładzie przedstawia syntetyczny typ wiadomości, nie wykonaną transmisję.

## Odtworzenie

Z katalogu projektu uruchom `./scripts/build.ps1`. Testy wygenerują obrazy w `artifacts/linkedin/demo`. Brak urządzenia nie blokuje renderowania. Pierwsze rozwiązywanie nowych zależności testowych można wykonać przełącznikiem `-UpdateLocks`; lockfile jest już zapisany w repozytorium.

Przed publikacją wybierz ujęcia i ostateczny ton posta. Testy oraz granice prototypu opisano w dołączonym [raporcie weryfikacji](VERIFICATION-0.6.md), a kontrolę danych w [raporcie baz](DATABASE-AUDIT-0.6.md). Nie przedstawiaj tych materiałów jako dowodu zakończonych prób operatora, MMS/RCS ani pełnej równoważności z systemowymi komunikatorami.
