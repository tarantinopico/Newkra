# GitHub Actions CI/CD — Automatická kompilace Android APK

Tento plán popisuje vytvoření a konfiguraci GitHub Actions workflow souboru (`.github/workflows/build-apk.yml`), který automatizuje proces sestavení aplikace, spustí jednotkové testy a vygeneruje hotový, ihned instalovatelný Android Debug APK balíček ke stažení.

---

## Uživatelská Rozhodnutí & Potvrzené Preference

> [!IMPORTANT] Shrnutí parametrů potvrzených uživatelem v 1. fázi:

* **Typ APK balíčku:** **Debug APK připravené ihned k instalaci**. Sestaveno pomocí standardního ladicího klíče bez nutnosti zadávat produkční hesla do GitHub Secrets, umožňující okamžité nahrání do Android telefonu a testování.
* **Spouštěcí události (Triggery):** 
  * Automaticky při každém **pushi** do větve `main`.
  * Při otevření / aktualizaci **Pull Requestu** do větve `main`.
  * **Ručně tlačítkem (workflow_dispatch)** kdykoliv v rozhraní GitHubu na záložce *Actions*.

---

## 1. Přehled a Hlavní Funkce

* **Co workflow dělá:** Po každé úpravě kódu nebo na vyžádání spustí čistý runner v cloudu GitHub Actions (Ubuntu), připraví prostředí Java 21, nastaví Gradle a Android SDK, zvaliduje integritu projektu, zkompiluje aplikaci do `app-debug.apk` a nahraje soubor jako GitHub Artifact.
* **Cílová skupina:** Vývojář a koncoví uživatelé / testeři, kteří chtějí mít vždy k dispozici nejčerstvější verzi aplikace přímo do telefonu bez nutnosti instalovat lokální Android Studio.
* **Hlavní přínos:** Zajištění 100% reprodukovatelného sestavení, včasná detekce chyb v kódu před nasazením a okamžitá distribuce instalačního souboru APK.

---

## 2. Uživatelská Zkušenost & Výstupy Pipeline

```
┌────────────────────────────────────────────────────────┐
│ GitHub Repository: Kratom Tracker                      │
├────────────────────────────────────────────────────────┤
│ [Actions Tab]                                          │
│                                                        │
│  ▶ Build Android Debug APK                             │
│    ✓ Set up JDK 21                                     │
│    ✓ Setup Gradle & Cache                              │
│    ✓ Decode Debug Keystore & Secrets                   │
│    ✓ Run Unit Tests                                    │
│    ✓ Assemble Debug APK                                │
│    ✓ Upload APK Artifact (app-debug.apk)               │
│                                                        │
│ ┌────────────────────────────────────────────────────┐ │
│ │ Artifacts (1)                                      │ │
│ │ 📦 kratom-tracker-debug-apk (size: ~15 MB)         │ │
│ │    └─ app-debug.apk [Stáhnout & Instalovat]        │ │
│ └────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

1. **Spuštění na jedno kliknutí:** V záložce GitHub Actions uživatel klikne na `Run workflow` a vybere větev.
2. **Přehledný stav:** Zelená fajfka signalizuje úspěšnou kompilaci; v případě chyby logy přesně ukážou řádek problému.
3. **Okamžité stažení:** V sekci *Artifacts* na stránce běhu workflow je k dispozici `.zip` obsahující `app-debug.apk`. Uživatel stáhne do mobilu a nainstaluje.

---

## 3. Klíčová Technická Rozhodnutí a Trade-Offs

* **Rozhodnutí 1: Verze Javy a Gradle**
  * *Zvolené řešení:* **Eclipse Temurin JDK 21** a Gradle 9.3.1 (prostřednictvím `gradle/actions/setup-gradle@v4`).
  * *Důvod:* Projekt používá AGP 9.1.1 a Kotlin 2.2.10, které jsou plně optimalizovány a otestovány pro Java 21. Použití oficiální akce `gradle/actions/setup-gradle` navíc přináší automatické cachování závislostí a zkracuje dobu sestavení.
* **Rozhodnutí 2: Řešení Debug Keystore v CI**
  * *Zvolené řešení:* Rekonstrukce `debug.keystore` z existujícího souboru `debug.keystore.base64`, případně fallback generování přes `keytool`.
  * *Důvod:* V repozitáři je `debug.keystore` v `.gitignore`, ale projekt obsahuje `debug.keystore.base64`. Tím je zaručeno, že podepisovací klíč bude v CI vždy shodný a sestavené APK bude kompatibilní s předchozími instalacemi v telefonu.
* **Rozhodnutí 3: Konfigurace Secrets Pluginu (.env)**
  * *Zvolené řešení:* Vytvoření souboru `.env` z `.env.example` před sestavením (`cp .env.example .env`).
  * *Důvod:* Zabraňuje chybě `Secrets Gradle Plugin`, pokud `.env` není verzován v Gitu.

---

## 4. Technická Architektura a Kroky Workflow

```
   [ Push do main / workflow_dispatch ]
                    │
                    ▼
          ┌───────────────────┐
          │  ubuntu-latest    │
          └─────────┬─────────┘
                    │
   1. actions/checkout@v4
                    │
   2. actions/setup-java@v4 (Temurin 21)
                    │
   3. gradle/actions/setup-gradle@v4 (Gradle 9.3.1 + Cache)
                    │
   4. Příprava keystore & env
      ├─ base64 -d debug.keystore.base64 > debug.keystore
      └─ cp -n .env.example .env
                    │
   5. Sestavení Debug APK
      └─ gradle :app:assembleDebug --stacktrace
                    │
   6. Validace artefaktu
      └─ app/build/outputs/apk/debug/app-debug.apk
                    │
   7. actions/upload-artifact@v4
      └─ Název: kratom-tracker-debug-apk (retence 30 dní)
```

### Specifikace souboru `.github/workflows/build-apk.yml`

* **Triggery:**
  ```yaml
  name: Build Android Debug APK

  on:
    push:
      branches: [ "main" ]
    pull_request:
      branches: [ "main" ]
    workflow_dispatch:
  ```
* **Klíčové permissions:** `contents: read` pro bezpečné stahování kódu.
* **Výstupní cesta k APK:** `app/build/outputs/apk/debug/app-debug.apk`.
* **Retence artefaktu:** 30 dní (standardní hodnota pro debug buildy).

---

## 5. Plán Realizace (po schválení plánu)

1. Vytvoření adresáře `.github/workflows/`.
2. Vytvoření a konfigurace workflow souboru `.github/workflows/build-apk.yml`.
3. Ověření syntaxe a parametrů přes projektové nástroje.
