# Logo i ikony CzyToTy?

Wektorowe odtworzenie kierunku z dostarczonej planszy: dymek rozmowy, słuchawka i znak zapytania. Nie jest to wycięty fragment ani dokładna kopia rastra.

- logo-dark / logo-light: poziome logo SVG oraz PNG 720 i 1440 px.
- logo-mark: sam znak SVG oraz PNG o szerokości 64, 256 i 1024 px.
- icon-dark / light / mono / bw: kwadratowe ikony SVG oraz PNG 64, 256 i 1024 px.
- phone_lookup, messages_sms, protection_shield, search, account: ikony interfejsu SVG oraz PNG 24, 48 i 96 px.
- Android: aktywna ikona adaptacyjna, wariant monochromatyczny i grafiki wektorowe znajdują się w app/src/main/res.

Kolory: tło #0A0A12, fiolet #A875FF, jasny #FAF4FF, obrys #C49BFF. Napis korzysta z Segoe UI, z zapasową rodziną sans-serif. PNG zawiera wyrenderowany napis; wygląd tekstu w SVG zależy od dostępnych fontów.

Ponowne wygenerowanie zasobów w katalogu projektu:

    npm install --prefix build/brand-tools --no-save --package-lock=false @resvg/resvg-js@2.6.2
    node scripts/brand-assets.cjs

Resvg służy tylko do eksportu PNG. Aplikacja nie potrzebuje Node ani tej biblioteki.
