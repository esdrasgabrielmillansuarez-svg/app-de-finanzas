# Finanzas — Android (offline-first)

> **Creado por Esdras Millan.**

App de finanzas personales **bimoneda** para Android, con entrada en menos de 3
segundos y **offline-first**: registrar, consultar y presupuestar funciona sin
red. `INTERNET` se usa solo para traer referencias públicas de cambio (BCV,
paralelo, USDT, EUR, ARS y una tabla global). Kotlin + Compose + Room + Hilt.

- Dirección de diseño: [`docs/diseno/direccion-de-diseno.md`](docs/diseno/direccion-de-diseno.md)
- Prototipo navegable (HTML): [`prototipo/prototipo.html`](prototipo/prototipo.html)

---

## Arrancar

Requisitos: **Android Studio** (Ladybug o superior, trae JDK 17) y Android SDK 35.

1. Abre la carpeta del proyecto en Android Studio.
2. Sync → Run en un dispositivo/emulador **API 26+**.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest    # importes, divisas, parser de facturas, auth, CSV
```

> El repo incluye wrapper de Gradle 8.9. En esta máquina el build se hizo con un
> toolchain portable (JDK 17 + SDK 35 + Gradle 8.9) fuera de Android Studio.

## Entrada (perfil local)

- **Crear cuenta / Entrar** con nombre, correo y contraseña.
- La contraseña se guarda como hash **PBKDF2-HMAC-SHA256** (120.000 iteraciones,
  sal aleatoria de 16 bytes por usuario) y se compara en tiempo constante.
- La sesión se recuerda entre arranques; **Cerrar sesión** está en Ajustes.
- Todo es local: no hay servidor, recuperación por correo ni sincronización.
  Si se olvida la contraseña se pierde el acceso al perfil (documentado en la UI).
- **Sin datos de ejemplo**: la app no precarga cuentas, movimientos ni
  categorías. La primera cuenta de una divisa se crea cuando guardas un
  movimiento en esa divisa; las categorías las creas tú desde Registrar o Plan.

## Qué funciona hoy

**Hoy**
- Saldo por divisa (local y USD), "te queda este mes", proyección de cierre y
  últimos movimientos.
- **Disponible real**: "te queda este mes" es **ingresos del mes menos todos los
  gastos**, incluso los registrados en otra divisa. El límite de sobres se
  muestra aparte como tope por categoría. Debajo se muestran las equivalencias
  en **USD y VES**, recalculadas cuando cambia la tasa.
- **Sticker contextual**: emoji + frase que cambia según vencimientos, sobres al
  límite, presupuesto y hábito. Botón "Otro" para rotar variantes.
- Pill de tasa → pantalla de divisas.

**Registrar (FAB)**
- Tipo, divisa, categoría, importe con aritmética inline (`12+450`, `1200/2`).
- **Escanear factura**: foto/galería → OCR en el dispositivo (ML Kit, modelo
  empaquetado) → precarga importe, comercio, divisa y, si ya creaste una
  categoría compatible, la categoría; con aviso de confianza.
- Guardado con **Deshacer** en el snackbar.

**Movimientos**
- Búsqueda por comercio, nota o categoría.
- Filtros por tipo (gastos/ingresos) y por divisa.
- Toque en una fila → detalle con **Duplicar** y **Eliminar** (con deshacer).

**Plan**
- **Sobres**: límite mensual editable por categoría, con progreso real y aviso al
  pasarse. Cada gasto se descuenta de su sobre aunque esté en otra divisa: se usa
  la tasa guardada al registrarlo y, si no existe, la cotización actual. Cada
  sobre muestra cuánto queda en USD y VES y se recalcula al actualizar la tasa.
- **Pagos**: alta con selector de fecha y recurrencia, marcar pagado (crea el
  movimiento y permite deshacer) y eliminar.

**Ajustes**
- Perfil y cierre de sesión.
- **Apariencia**: modo Sistema/Claro/Oscuro y seis temas (Grafito, Océano,
  Bosque, Atardecer, Lavanda, Monocromo). Se aplican al instante y se recuerdan
  entre arranques.
- **Recordatorios**: aviso diario de vencimientos (WorkManager a las 9:00), con
  interruptor para apagarlo y permiso de notificaciones pedido solo cuando hay
  algo que recordar.
- Divisas base/secundaria y tasa (auto o manual por divisa).
- **Tasas de cambio** (Ajustes): elegir la referencia del bolívar (BCV, paralelo
  o USDT) y fijar a mano la equivalencia de cada referencia, del euro y del peso
  argentino; la manual siempre gana y se puede quitar.
- **Exportar movimientos (CSV)** por el selector del sistema (FileProvider).
- **Borrar todos los movimientos** con confirmación.
- **Acerca de**: creador y versión visibles.
- Seguridad local: contraseña PBKDF2; sin bloqueo biométrico.

**Voz de la app**
- Mensajes contextuales y deterministas (sin nube): prioridad vencido → vence
  hoy → próximo → sobre excedido → sobre al límite → primer movimiento →
  presupuesto → celebración → aliento.
- Hitos al guardar (1, 5, 10, 25, 50 y 100 movimientos) con emoji, nunca culpa
  ni alarmismo.
- El tono se inspira en las notificaciones de Duolingo: una idea por mensaje,
  concreto, con acción; sin "¡estás fallando!".

**Divisas**
- Catálogo de ~56 divisas con país, bandera y región (incluye USDT para
  Venezuela).
- Equivalencia en **USD** y **EUR** por unidad.
- **Referencias en vivo** del bolívar (VES): BCV oficial, dólar paralelo y USDT
  (estimado con el paralelo). Se actualizan al abrir la app y con el botón
  *Actualizar*, se guardan en caché 6 h y muestran cuándo se obtuvieron.
- **Euro** (tipo del BCE vía Frankfurter), **peso argentino oficial** (dolarapi)
  y **tabla global** (ExchangeRate-API) para el resto del catálogo.
- Elegir base y secundaria; fijar tasa manual por divisa o referencia (siempre
  gana sobre la de internet) o volver a la de referencia.
- Si falla la red, se conserva la última cotización. Si una divisa nunca tuvo
  dato, se muestra **sin cotización** y se puede fijar a mano: no hay una tabla
  de valores aproximados ni información inventada.

## Estructura

```
app/src/main/java/com/suarezmillan/finanzas/
├── FinanzasApp.kt              # @HiltAndroidApp
├── MainActivity.kt             # edge-to-edge + RootGate
├── core/
│   ├── auth/                   # PasswordHasher (PBKDF2), AuthValidation
│   ├── coach/CoachEngine.kt    # mensajes/stickers contextuales (JVM puro)
│   ├── money/Money.kt          # unidades menores + formato es-AR
│   ├── currency/               # catálogo (~56) + tasas (USD pivot) + referencias remotas
│   ├── receipt/                # ReceiptParser (heurística) + ReceiptScanner (ML Kit)
│   ├── export/                 # CsvExporter + shareCsv (FileProvider)
│   ├── reminder/               # WorkManager diario + notificación de vencimientos
│   ├── fx/Fx.kt                # FxRate, FxSource, conversión
│   └── settings/               # AppSettings + PreferenceStore (JVM testeable)
├── data/
│   ├── local/
│   │   ├── FinanzasDatabase.kt # v3
│   │   ├── Migrations.kt       # 1 → 2 (users), 2 → 3 (limpia datos de ejemplo)
│   │   ├── Converters.kt
│   │   ├── entity/             # Account, Category, Txn, Envelope, ScheduledPayment, Debt, User
│   │   ├── dao/                # un DAO por agregado
│   │   └── model/EnvelopeProgress.kt
│   └── repo/                   # Txn, Account, Budget, Category, ScheduledPayment, Auth
├── di/                         # Hilt: base de datos y bindings
└── ui/
    ├── RootGate.kt             # splash / login / app
    ├── auth/                   # AuthViewModel + AuthScreen
    ├── FinanzasRoot.kt         # Scaffold, tabs, FAB, hoja de registro, diálogo de detalle
    ├── MainViewModel.kt        # estado único del MVP
    ├── home/                   # Hoy + StickerCard
    ├── movs/ plan/ ajustes/ divisas/
    ├── quick/                  # hoja de registro + AmountParser
    ├── components/             # MoneyText, ConfirmDialog
    └── theme/                  # Color, Type, Shape, Motion, Theme
```

## Decisiones que no conviene deshacer

| Decisión | Por qué |
|---|---|
| Importes en `Long` (unidades menores) + código ISO | `Double` pierde centavos y mezcla divisas. |
| Tasa de cambio **por transacción** (`fxRateToBase`, `fxSource`, `fxTimestamp`) | Cambiar la tasa de hoy no debe reescribir el pasado. |
| Una cuenta = una divisa | El saldo deja de ser una suma de tasas distintas. |
| Importes firmados (`gasto` negativo, `ingreso` positivo) | Un `SUM` cuadra cualquier cuenta, incluidas transferencias. |
| Transferencias = dos filas unidas por `transferGroupId` | Cada cuenta cuadra por separado; conversión explícita. |
| `INTERNET` solo para referencias públicas de cambio | El resto es offline de verdad: movimientos, facturas y perfil no salen del teléfono. |
| Sin Material You dinámico | El color codifica divisa; el wallpaper no puede decidirlo. |
| `MoneyText` como único formateador | Importes con cifras tabulares y formato es-AR en un solo lugar. |
| Migraciones Room escritas a mano | Los movimientos del usuario no son recuperables. |
| PBKDF2 con sal y comparación en tiempo constante | Nunca se guarda la contraseña; una filtración de la DB no la revela. |
| OCR local (ML Kit empaquetado) + parser propio | Sin subir facturas a ningún lado y con lógica testeable en JVM. |
| El tema cambia **acento y superficies**, nunca el significado | USD sigue ámbar y la moneda local sigue cian: el color no deja de informar. |
| Mensajes del coach deterministas y contextuales | Sin nube, sin IA impredecible y testeables; prioridad explícita por reglas. |
| Recordatorios con WorkManager, no `AlarmManager` | No hace falta precisión al minuto ni permisos de alarmas exactas. |
| Sin cuentas, categorías ni tasas de ejemplo | La app muestra solo datos del usuario o referencias reales; si falta una tasa, dice "sin cotización". |
| Cuenta creada al primer movimiento de cada divisa | Se evita pedir una configuración inicial y nunca se inventa un saldo o una cuenta. |

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

- `AmountParserTest` — aritmética inline y decimales.
- `CurrencyRatesTest` — paridad vía USD, referencias BCV/paralelo/USDT, EUR/ARS
  en vivo, ausencia de tasas inventadas y formato de tasas.
- `ReceiptParserTest` — extracción de importe/comercio/categoría en tickets.
- `PasswordHasherTest` — hash/verificación, sal distinta, no reversible.
- `AuthValidationTest` — nombre, correo normalizado, contraseña mínima.
- `CsvExporterTest` — cabecera, decimales y escapado RFC 4180.
- `CoachEngineTest` — prioridad de vencimientos, sobres, hitos y rotación de variantes.
- `BcvRateParserTest` — parser del BCV/paralelo, rechazo de fuente no esperada y caché de 6 h.
- `MarketRateParsersTest` — euro de Frankfurter, ARS oficial y tabla global.
- `LiveRateSettingsTest` — referencias, elección, base/secundaria y overrides
  sobreviven al reinicio.

**63 tests, 0 fallos** en `:app:testDebugUnitTest`.

## Qué falta (a propósito)

- Otras cotizaciones (MEP/CCL) y actualización en segundo plano de la tasa.
- Edición de movimientos ya guardados (hoy: duplicar/eliminar).
- CRUD de cuentas y renombrar la cuenta automática de cada divisa.
- Deudas: entidad y DAO listos, **sin UI**.
- SQLCipher + clave en Android Keystore (no incluido).
- Bloqueo biométrico (no incluido; `androidx.biometric` está en dependencias).
- DataStore para la sesión (apariencia, recordatorios, divisas y tasas ya
  persisten en SharedPreferences).
- Edición de los textos del coach desde la UI (hoy son constantes testeables).
- Widget Glance + deep link `finanzas://registrar` (el manifiesto ya lo declara).
- Backup propio cifrado (Auto Backup está **desactivado** para la base).

## Versiones

Versión de la app: **0.5.0** (`versionCode 4`).

Fijadas a un set conservador y mutuamente compatible: Kotlin 2.0.21, AGP 8.7.3,
Gradle 8.9, Compose BOM 2024.10.01, Room 2.6.1, Hilt 2.52, KSP 2.0.21-1.0.28,
ML Kit Text Recognition 16.0.1 y WorkManager 2.9.1.

Actualización recomendada cuando se quiera tocar el tema:

- `material3` 1.4.0 (estable) y cambiar `MaterialTheme` por
  `MaterialExpressiveTheme(motionScheme = MotionScheme.expressive())` en
  `ui/theme/Theme.kt`.
- AGP / Kotlin / KSP / Hilt a sus últimas estables (subir en bloque y volver a
  correr `./gradlew :app:assembleDebug`).
