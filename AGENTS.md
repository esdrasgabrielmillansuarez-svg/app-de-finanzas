# app de finanza — guía para agentes

App Android **bimoneda** de finanzas personales, **offline-first**.
Kotlin + Compose + Room + Hilt. `INTERNET` solo para referencias de cambio.

- Rama de trabajo: `master`.
- Secretos: nunca commitear `.env*`, `local.properties`, `*.jks`, `*.keystore`
  ni claves. `local.properties` ya está ignorado.
- Antes de commitear: `git status --short` + revisar el diff staged.

## Comandos

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat testDebugUnitTest --console=plain
```

Nota: PowerShell devuelve exit code 1 por los warnings de Gradle en stderr
aunque el build sea `BUILD SUCCESSFUL`; mirar la línea final, no el exit code.

Para buscar en el código usar `Select-String` (la herramienta `grep` del harness
falla en esta máquina con `tar.exe no se reconoce`).

## Arquitectura (dónde va cada cosa)

| Capa | Ruta |
| --- | --- |
| Dominio (dinero, FX, facturas, reportes, CSV) | `app/src/main/java/com/suarezmillan/finanzas/core/` |
| Persistencia (Room: entidades, DAOs, DB) | `.../data/local/` |
| Repositorios | `.../data/repo/` |
| UI (Compose, pantallas, viewmodels) | `.../ui/` |
| Pruebas unitarias | `app/src/test/java/com/suarezmillan/finanzas/` |

Reglas de dinero (no negociables):

- Los importes son `Long` en **minor units** (centavos); nunca `Double`.
- `TxnEntity.amountMinor` está en su divisa original; `fxRateToBase` guarda la
  tasa usada y `null` si ya está en la divisa base.
- Una transferencia entre cuentas crea **dos** filas unidas por `transferGroupId`.

Al tocar la lectura de facturas (`core/receipt/`):

- `ReceiptParser` extrae `details` (todos los campos clave:valor), `items`
  (concepto + importe), `subtotalMinor`, `taxMinor`, `discountMinor`.
- `detailedParts` solo precarga el desglose si la suma **cuadra con el total**;
  si no cuadra, deja importe único y añade un warning.
- Al guardar, `detailsNote()` escribe todos los campos leídos en `TxnEntity.note`.

## Convenciones

- Español en comentarios, mensajes de UI, warnings y nombres de pruebas
  (`backticks` con frase en español: ``fun `el desglose respeta el tope`()``).
- Identificadores de código en inglés (`amountMinor`, `occurredAt`).
- Comentarios solo cuando explican el *por qué* (una regla de negocio, un bug
  concreto); no narrar lo que ya dice el código.
- Cada cambio de comportamiento lleva su prueba unitaria en `src/test`.
- Commits en el estilo del repo: `finanzas: <resumen en español>`.

## gstack (flujo de trabajo)

gstack está instalado para OpenCode en `C:\Users\Suarezmillan\.config\opencode\`
(skills en `skills/gstack/`, comandos en `commands/gstack-*.md`). El repo fuente
es `C:\Users\Suarezmillan\gstack` y se actualiza con `./setup --host opencode`.

Comandos disponibles como `/gstack-<nombre>`. Flujo recomendado:

1. `/gstack-office-hours` — pensar la idea antes de escribir código.
2. `/gstack-plan-ceo-review` y `/gstack-plan-eng-review` — revisar el plan.
3. Implementar.
4. `/gstack-verify` — correr checks y pruebas hasta verde.
5. `/gstack-review` — revisar el diff antes de mergear.
6. `/gstack-qa` — probar la app y corregir bugs (usa `/gstack-browse` para el navegador).
7. `/gstack-ship` — versionar, CHANGELOG, commit y PR.

Guardarraíles útiles: `/gstack-careful` (avisa antes de comandos destructivos),
`/gstack-freeze` (limita las ediciones a una carpeta), `/gstack-guard` (ambos).

Revisiones externas: `/gstack-codex` usa Codex CLI (`codex-cli 0.167.0`,
requiere `codex login` una vez) y `/gstack-claude-code` usa Claude Code si está
instalado. Sin login, esos comandos no funcionan; el resto sí.

Contexto entre sesiones: `/gstack-context-save` y `/gstack-context-restore`.
