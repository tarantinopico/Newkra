# Kratom Tracker v4.1 — Plán Rozšíření Konfigurace, Dashboardu a Grafů

Detailní plán implementace plné konfigurovatelnosti výpočtových konstant v Nastavení, nových delta badge tagů na Dashboardu a zpřesnění analytických vizualizací.

---

## Uživatelská Rozhodnutí & Potvrzené Preference

* **Konfigurovatelnost výpočtů:** Zpřístupnit **všechny farmakokinetické, dehydratační a toleranční konstanty** v novém specializovaném panelu Nastavení s možností rychlého obnovení výchozích vědeckých hodnot.
* **Vzhled Dashboardu:** Zobrazit **barevné zaoblené badge tagy se šipkami nahoru a dolů (↑ / ↓)** porovnávající dnešní hodnoty vůči včerejšku a 7dennímu průměru.

---

## 1. Navrhované Změny v Architektuře a Komponentách

### A. Konfigurace výpočtů v Nastavení (`SettingsScreen.kt`, `AppState.kt`, `Calculations.kt`)
1. **Nová sekce „Pokročilé výpočty a konstanty modelů“:**
   * **Farmakokinetika Mitragyninu:**
     * $t_{\max}$ absorpce (výchozí $1.2\text{ h}$, rozsah $0.5\text{--}4.0\text{ h}$)
     * Poločas distribuce $\alpha$ (výchozí $3.8\text{ h}$, rozsah $1.0\text{--}10.0\text{ h}$)
     * Terminální eliminační poločas $\beta$ (výchozí $24.0\text{ h}$, rozsah $12.0\text{--}48.0\text{ h}$)
     * Koeficient alkaloidů v sušině (výchozí $1.5\%\text{ MIT}$, $0.03\%\text{ 7-OH}$)
     * Bezpečnostní strop plazmy (výchozí $1.5\text{ mg/kg}$)
   * **Hydratační model:**
     * Základní cíl tekutin bez kratomu (výchozí $2500\text{ ml}$)
     * Koeficient kompenzace dehydratace (výchozí $200\text{ ml}$ na $1\text{ g}$ suchého kratomu)
   * **Citlivost receptorů & T-Break:**
     * Penalizace za krátký odstup $<2.5\text{ h}$ (výchozí $15\%$ bodů)
     * Regenerace citlivosti za 24h čisté pauzy (výchozí $+15\%/\text{den}$)
     * Cílový minimální rozestup (výchozí $4.0\text{ h}$)
     * Varovný práh rotace stejné odrůdy v řadě (výchozí $3\text{ dávky}$)
   * **Možnost okamžitého resetu** všech konstant jedním kliknutím na vědecké tovární hodnoty.

### B. Vylepšený Dashboard s Delta Badge Tagy (`DashboardScreen.kt`, `Calculations.kt`)
1. **Výpočet denních srovnání (Today vs. Yesterday & 7D Average):**
   * Pomocná funkce `calcDashboardDeltas(allDoses, settings)`:
     * Dnešní gramáž vs. Včerejšek ($\Delta\text{ g}$, $\Delta\%$) a vs. 7D průměr.
     * Dnešní útrata vs. Včerejšek a vs. 7D průměr.
     * Průměrný denní rozestup vs. 7D průměr.
     * Vypitá voda vs. Včerejšek.
2. **Kompaktní barevné zaoblené Badge Tagy:**
   * Zobrazení přímo pod / vedle hlavních metrik na kartách Bento gridu:
     * Gramáž a Finance: pokles $\downarrow$ je označen **zeleně** (šetření/střídmost), nárůst $\uparrow$ je označen **oranžově/červeně** (zvýšená spotřeba).
     * Rozestupy a Hydratace: nárůst $\uparrow$ je označen **zeleně** (lepší disciplína/více vody), pokles $\downarrow$ **oranžově**.
     * Badge formát: `[↑ +15 % vs včera]` a `[↓ -8 % vs 7D Ø]`.

### C. Zpřesnění detailů a čitelnosti grafů (`GaugesAndCharts.kt`, `AnalyticsScreen.kt`)
1. **Informační štítky a hodnoty u křivek:**
   * Zobrazení min / max / průměrných hodnot přímo v záhlaví a legendách grafů.
   * Přidání vodících horizontálních linek pro lepší odečet hodnot z osy Y.
   * Zobrazení přesných časových popisků a jednotek u farmakokinetických a cirkadiánních grafů.

---

## 2. Plán Fází Realizace

1. **Fáze 1 (Model & Výpočty):**
   * Doplnění nových nastavitelných parametrů a výchozích hodnot do `AppSettings` v `AppState.kt`.
   * Implementace analytické funkce `calcDashboardDeltas(...)` v `Calculations.kt`.
2. **Fáze 2 (Dashboard UI):**
   * Vytvoření reusable composable komponenty `DeltaBadge(deltaPct: Double, inverseColor: Boolean = false, label: String)` v `GaugesAndCharts.kt`.
   * Integrace badge tagů do hero tachometru, metrik HUDu, karty hydratace a rozestupů v `DashboardScreen.kt`.
3. **Fáze 3 (Nastavení UI):**
   * Přidání nové karty a dialogu pro pokročilou konfiguraci konstant v `SettingsScreen.kt` s číselnými voliči a tlačítkem obnovení výchozích hodnot.
4. **Fáze 4 (Detaily grafů):**
   * Doplnění detailních vodících linek, min/max hodnot a popisků v `GaugesAndCharts.kt`.
5. **Fáze 5 (Ověření & Kompilace):**
   * Spuštění `compile_applet` a `gradle :app:testDebugUnitTest`.
