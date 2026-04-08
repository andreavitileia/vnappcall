# VNAppCall v2.0

App Android per il tracciamento e la gestione delle chiamate telefoniche aziendali di VN sas.

## Funzionalità

- **Registro Chiamate** — Annotare chiamate con note, flag "da fatturare"/"risolto", e numeri seriali macchine
- **Contatti** — Ricerca nella rubrica Android e nel registro chiamate di sistema
- **Report** — Export CSV, invio email SMTP, sincronizzazione con portale web
- **Report Automatico** — Invio giornaliero programmato via AlarmManager
- **Sincronizzazione Portale** — Upload JSON verso il portale web (`/api/reports`)
- **Bulk Sync** — Sincronizzazione massiva di dati storici

## Stack Tecnologico

| Componente | Tecnologia |
|---|---|
| Linguaggio | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Database | Room (SQLite) |
| ViewModel | AndroidX ViewModel + StateFlow |
| Background | AlarmManager |
| Email | JavaMail (SMTP) |
| Target SDK | 34 (Android 14) |
| Min SDK | 26 (Android 8.0) |

## Struttura Progetto

```
app/src/main/java/com/vnsas/vnappcall/
├── MainActivity.kt          # Entry point
├── MainViewModel.kt         # ViewModel centrale
├── VNApp.kt                 # Application class
├── data/                    # Modelli + Room database
├── ui/                      # Composable UI (4 tab)
├── util/                    # CallLogReader, MailSender, PortalSync, ReportExporter
└── worker/                  # BootReceiver, DailyReportReceiver
```

## Build

Apri il progetto in Android Studio e compila con Gradle.

```bash
./gradlew assembleDebug
```

## Permessi Richiesti

- `READ_CALL_LOG` — Lettura registro chiamate
- `READ_CONTACTS` — Accesso rubrica
- `POST_NOTIFICATIONS` — Notifiche
- `INTERNET` — Upload report + email
- `RECEIVE_BOOT_COMPLETED` — Ri-schedulare alarm al boot
