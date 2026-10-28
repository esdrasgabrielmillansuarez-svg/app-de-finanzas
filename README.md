# Finanzas — MVP Android (offline-first)

App de finanzas personales **bimoneda** para Android, con entrada en menos de 3
segundos y **cero red**: no declara permiso de `INTERNET`. Kotlin + Compose +
Room + Hilt.

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

## Qué funciona hoy

**Hoy**
- Saldo por divisa (local y USD), "te queda este mes", proyección de cierre y
  últimos movimientos.
- Pill de tasa → pantalla de divisas.

**Registrar (FAB)**
- Tipo, divisa, categoría, importe con aritmética inline (`12+450`, `1200/2`).
- **Escanear factura**: foto/galería → OCR en el dispositivo (ML Kit, modelo
  empaquetado) → precarga importe, comercio, categoría y divisa, con aviso de
  confianza.
- Guardado con **Deshacer** en el snackbar.

**Movimientos**
- Búsqueda por comercio, nota o categoría.
- Filtros por tipo (gastos/ingresos) y por divisa.
- Toque en una fila → detalle con **Duplicar** y **Eliminar** (con deshacer).

**Plan**
- **Sobres**: límite mensual editable por categoría, con progreso real y aviso al
  pasarse.
- **Pagos**: alta con selector de fecha y recurrencia, marcar pagado (crea el
  movimiento y permite deshacer) y eliminar.

**Ajustes**
- Perfil y cierre de sesión.
- Divisas base/secundaria y tasa (auto o manual por divisa).
- **Exportar movimientos (CSV)** por el selector del sistema (FileProvider).
- **Borrar todos los movimientos** con confirmación.
- Estado honesto de seguridad: contraseña PBKDF2; biometría pendiente.

**Divisas**
- Catálogo de ~55 monedas con país, bandera y región.
- Equivalencia en **USD** y **EUR** por unidad.
- Elegir base y secundaria; fijar tasa manual o volver a la de referencia.

## Estructura

```
app/src/main/java/com/suarezmillan/finanzas/
├── FinanzasApp.kt              # @HiltAndroidApp
├── MainActivity.kt             # edge-to-edge + RootGate
├── core/
│   ├── auth/                   # PasswordHasher (PBKDF2), AuthValidation
│   ├── money/Money.kt          # unidades menores + formato es-AR
│   ├── currency/               # catálogo de ~55 divisas + tasas (USD pivot)
│   ├── receipt/                # ReceiptParser (heurística) + ReceiptScanner (ML Kit)
│   ├── export/                 # CsvExporter + shareCsv (FileProvider)
│   ├── fx/Fx.kt                # FxRate, FxSource, conversión
│   └── settings/AppSettings.kt # divisas y tasa (en memoria, TODO DataStore)
├── data/
│   ├── local/
│   │   ├── FinanzasDatabase.kt # v2
│   │   ├── Migrations.kt       # 1 → 2 (tabla users)
│   │   ├── Converters.kt
│   │   ├── entity/             # Account, Category, Txn, Envelope, ScheduledPayment, Debt, User
│   │   ├── dao/                # un DAO por agregado
│   │   └── model/EnvelopeProgress.kt
│   └── repo/                   # Txn, Budget, Category, ScheduledPayment, Auth
├── di/                         # Hilt: base de datos, semilla, bindings
└── ui/
    ├── RootGate.kt             # splash / login / app
    ├── auth/                   # AuthViewModel + AuthScreen
    ├── FinanzasRoot.kt         # Scaffold, tabs, FAB, hoja de registro, diálogo de detalle
    ├── MainViewModel.kt        # estado único del MVP
    ├── home/ movs/ plan/ ajustes/ divisas/
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
| `AndroidManifest` **sin** permiso `INTERNET` | Offline-first de verdad, no "offline con letra chica". |
| Sin Material You dinámico | El color codifica divisa; el wallpaper no puede decidirlo. |
| `MoneyText` como único formateador | Importes con cifras tabulares y formato es-AR en un solo lugar. |
| Migraciones Room escritas a mano | Los movimientos del usuario no son recuperables. |
| PBKDF2 con sal y comparación en tiempo constante | Nunca se guarda la contraseña; una filtración de la DB no la revela. |
| OCR local (ML Kit empaquetado) + parser propio | Sin subir facturas a ningún lado y con lógica testeable en JVM. |

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

- `AmountParserTest` — aritmética inline y decimales.
- `CurrencyRatesTest` — paridad vía USD, equivalencias EUR/USD y formato de tasas.
- `ReceiptParserTest` — extracción de importe/comercio/categoría en tickets.
- `PasswordHasherTest` — hash/verificación, sal distinta, no reversible.
- `AuthValidationTest` — nombre, correo normalizado, contraseña mínima.
- `CsvExporterTest` — cabecera, decimales y escapado RFC 4180.

## Qué falta (a propósito)

- Edición de movimientos ya guardados (hoy: duplicar/eliminar).
- CRUD de cuentas; el registro usa `acc-cash` / `acc-usd` fijos.
- Deudas: entidad y DAO listos, **sin UI**.
- SQLCipher (bloque comentado en `DatabaseModule`) + clave en Android Keystore.
- Bloqueo biométrico (`androidx.biometric` ya está en dependencias).
- DataStore para ajustes y sesión; `AppSettings` vive en memoria.
- Widget Glance + deep link `finanzas://registrar` (el manifiesto ya lo declara).
- Backup propio cifrado (Auto Backup está **desactivado** para la base).

## Versiones

Fijadas a un set conservador y mutuamente compatible: Kotlin 2.0.21, AGP 8.7.3,
Gradle 8.9, Compose BOM 2024.10.01, Room 2.6.1, Hilt 2.52, KSP 2.0.21-1.0.28,
ML Kit Text Recognition 16.0.1.

Actualización recomendada cuando se quiera tocar el tema:

- `material3` 1.4.0 (estable) y cambiar `MaterialTheme` por
  `MaterialExpressiveTheme(motionScheme = MotionScheme.expressive())` en
  `ui/theme/Theme.kt`.
- AGP / Kotlin / KSP / Hilt a sus últimas estables (subir en bloque y volver a
  correr `./gradlew :app:assembleDebug`).
