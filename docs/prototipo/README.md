# Prototipo visual (Bootstrap 5)

Maqueta de las pantallas de **Finanzas** para revisar jerarquía, colores, tarjetas
de presupuesto y equivalencias en USD/VES. No es la app Android y no usa datos
reales: todos los importes y tasas están marcados como ejemplo.

## Verlo

Abre `index.html` en el navegador. No necesita build ni servidor: Bootstrap e
iconos se cargan desde CDN.

## Archivos

- `index.html`: pantallas Inicio, Plan, Divisas y Ajustes; modales de registrar,
  nuevo sobre y nuevo pago.
- `assets/app.css`: sistema visual (tarjetas, efectivo/tabular, progreso,
  equivalencias, modo oscuro).
- `assets/app.js`: navegación entre pantallas, tema, toasts y acciones demo.

## Relación con la app Android

La app real está en Kotlin/Compose. Este prototipo replica las decisiones
visuales y de contenido:

- Marca del creador **Esdras Millan** visible.
- Disponible = ingresos del mes menos todos los gastos, convertidos a la divisa
  base. El límite de sobres se muestra aparte como tope por categoría.
- Equivalencias en dólares y bolívares que se recalculan con la tasa actual.
- Si falta una cotización se muestra **“sin cotización”**; no se inventa un
  valor.

Bootstrap Studio es una app de escritorio y no tiene una API para conectarse
desde este entorno. Estos archivos sirven como maqueta y como referencia para
recrear o ajustar el diseño allí si lo deseas.
