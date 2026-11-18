# PrivateTube — Android MVP

Prywatna, lokalna biblioteka wideo dla Androida.

## Funkcje wersji 1.0
- Odczyt filmów z pamięci urządzenia przez MediaStore.
- Kafelkowy widok miniatur w dwóch kolumnach.
- Filtrowanie według folderu.
- Kliknięcie kafelka otwiera podgląd do 5 sekund.
- Przycisk „Odtwórz cały film” uruchamia pełne odtwarzanie.
- Działa lokalnie; aplikacja nie wysyła plików do sieci.

## Budowanie APK
1. Otwórz folder projektu w Android Studio (Hedgehog lub nowszym).
2. Poczekaj na synchronizację Gradle.
3. Wybierz Build > Build Bundle(s) / APK(s) > Build APK(s).
4. APK znajdziesz w `app/build/outputs/apk/debug/app-debug.apk`.

Wymagania: Android Studio, JDK 17, Android SDK Platform 35.

## Ważne ograniczenia MVP
- Uprawnienie do odczytu filmów jest wymagane.
- Wybór folderu odbywa się przez listę folderów wykrytych w bibliotece Androida.
- Podgląd ma limit 5 sekund i można go zamknąć; pełne odtwarzanie jest osobnym widokiem.
- To projekt startowy, niecertyfikowany i nieprzetestowany na wszystkich modelach urządzeń.
