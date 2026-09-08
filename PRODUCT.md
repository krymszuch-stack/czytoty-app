# CzyToTy? — kierunek produktu zaakceptowany przez użytkownika

## Cel

CzyToTy ma zastąpić aplikację Telefon i Wiadomości na Androidzie. Trzy filary: codzienne połączenia, wiadomości oraz ochrona przed sygnałami oszustw. Nowoczesny interfejs ma służyć tym czynnościom; informacje techniczne są dostępne w szczegółach.

## Obecna iteracja 0.5

- Telefon: Ostatnie, Kontakty i Klawiatura. Duże klawisze, litery na klawiaturze, szybkie przejście do SMS i notatki/blokady.
- Wiadomości: lista rozmów z avatarami generowanymi z rzeczywistych nadawców, wyszukiwarka, przycisk nowej rozmowy.
- Rozmowa SMS: dymki przychodzące/wychodzące, najnowsze wiadomości przy polu pisania, możliwość zaznaczania treści, ostrożne opisy statusów i sygnałów CERT.
- Cztery główne pozycje nawigacji. Konto i notatki w górnym pasku.
- Zachowane istniejące źródła i działanie prototypu. Brak fikcyjnej zawartości skrzynki i historii.

## Integracja połączeń w 0.5

Dodano InCallService, konfigurację roli Telefon, ekran połączenia przychodzącego i aktywnego, DTMF, sterowanie dźwiękiem, wybór konta SIM, zawieszanie i łączenie rozmów według możliwości Telecom. Jest to kod prototypowy. Testy UI nie są potwierdzeniem działania operatora; role nie są nadawane automatycznie.

## Warunki pełnego zastąpienia aplikacji systemowych — pozostałe prace

1. Domyślny Telefon: testy rzeczywistych połączeń i reakcji na awarie, odbieranie przy blokadzie ekranu, Bluetooth i wiele kart SIM. Brak kompletnej obsługi wideo/RTT; odbieranie w prototypie jest audio. Ekrany i podstawowe akcje Telecom są już zaimplementowane, ale to nie jest jeszcze potwierdzony zamiennik do codziennego użycia.
2. Domyślne Wiadomości: rola ROLE_SMS, SMS_DELIVER i trwały zapis przychodzących SMS, WAP_PUSH_DELIVER oraz odbiór MMS, zapis i status wysłanych wiadomości, oznaczanie przeczytania, usuwanie z potwierdzeniem, powiadomienia i odpowiedzi. Nie należy przejmować roli, jeśli obecna aplikacja przestałaby dostarczać typ wiadomości, którego CzyToTy jeszcze nie umie odebrać.
3. Testy operatora na rzeczywistych urządzeniach: połączenia przychodzące/wychodzące/nieodebrane, ekran zablokowany, Bluetooth, brak sieci, restart procesu, dual-SIM, krótkie i wieloczęściowe SMS, błędy wysyłki i MMS. Testy wymagające rzeczywistej transmisji mają wcześniej uzgodnionego odbiorcę i treść.
4. Brak obietnicy RCS bez rzeczywiście dostępnej integracji. Obsługa RCS nie wynika z samego uzyskania roli SMS.

## Wyróżnik

Ochrona przy konkretnej czynności: czytaniu SMS, otwieraniu linku, oddzwanianiu. Każde ostrzeżenie ma wyjaśnienie, źródło i informację o aktualności. Numer bez danych nie otrzymuje etykiety bezpieczny. Treści komunikacji pozostają lokalne; chmura przyjmuje wyłącznie świadomie zapisywane notatki.

To dokument celu i braków, nie deklaracja wdrożenia wszystkich powyższych funkcji.

Źródła wymagań systemowych: [Android — domyślna aplikacja Telefon](https://developer.android.com/develop/connectivity/telecom/dialer-app), [Android — Telephony i domyślna aplikacja SMS](https://developer.android.com/reference/android/provider/Telephony).

