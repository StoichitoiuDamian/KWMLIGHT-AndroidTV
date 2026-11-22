# KWMLIGHT Control — Android TV (proiect experimental)

Aplicație Android nativă, Java, pentru controllerul USB ROBOBLOQ / KWMLIGHT **VID 1A86, PID FE07**, interfața HID 0. Nu folosește rețeaua și nu scrie firmware.

## Construire APK

1. Instalează **Android Studio** (JDK 17 + Android SDK 35).
2. Deschide directorul `KWMLIGHT-AndroidTV` cu **Open** și așteaptă sincronizarea Gradle.
3. Din meniu: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. APK-ul rezultat este în `app/build/outputs/apk/debug/app-debug.apk`.
5. Transferă APK-ul pe TCL 50P69K și instalează-l cu un manager de fișiere, după autorizarea instalării din surse externe.

## Testare

1. În DX-Light pe PC, verifică **numărul real de LED-uri** al controllerului; nu ghici. Nu folosi o valoare mai mare decât cea reală.
2. Scoate USB-ul din PC și conectează-l la TV.
3. Pornește KWMLIGHT Control, selectează **Detectează și conectează USB**, acceptă permisiunea Android.
4. Introdu numărul total de LED-uri; selectează o culoare. Aplicația trimite comenzi pentru culoare statică și luminozitate.
5. Dacă nu se schimbă culoarea, notează textul de stare și testează alt port USB. Nu modifica firmware-ul.

## Limite

- **Netestat pe hardware TCL 50P69K**; compatibilitatea USB Host și raportarea HID diferă între televizoare.
- Nu realizează Ambilight/sincronizare cu imaginea Netflix/YouTube.
- Preferințele rămân în aplicație, dar în această versiune trebuie să o deschizi și să apeși din nou culoarea după o repornire. Nu există serviciu de autostart.
- Aplicația nu citește încă automat numărul de LED-uri, tocmai pentru a evita un număr presupus.
- Nu există încă efecte animate.
- Transmiterea se face prin interrupt OUT dacă este disponibil, altfel prin HID SET_REPORT. Pe unele dispozitive Android ar putea fi nevoie de adaptarea transportului.
- La primul build Android Studio poate descărca pluginul Android Gradle și componente SDK.

## Protocol

Cadre `RB`: 52 42 LEN SEQ CMD PAYLOAD CHECKSUM, suma tuturor octeților anteriori modulo 256. `0x87` luminozitate; `0x86` culoare pe interval; `0x97` stinge. Interfața HID 0; raport de 64 octeți, report ID 0. La culori, RGB este limitat astfel încât R+G+B <= 255 pentru alimentarea USB.

Referințe independente pentru formatul protocolului:
- https://github.com/zain-uh/DX-Static
- https://github.com/Tonic-Jin/SyncRGB

Acesta este un prototip pentru teste, nu un produs oficial KWMLIGHT / DX-Light.

## Gradle

Arhiva nu include `gradle-wrapper.jar` (binarul wrapper). Dacă Android Studio cere wrapper, folosește o instalare locală de **Gradle 8.7** și rulează `gradle wrapper --gradle-version 8.7` din directorul proiectului; apoi sincronizează din nou proiectul. Necesită acces la Google Maven/Maven Central pentru pluginul Android.
