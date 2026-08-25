# Cómo probar este plugin

Este plugin revisa un archivo de código y avisa cuando el programa
usa un "dato secreto" (como una contraseña o una clave) que nunca fue
declarado en el archivo de configuración correspondiente.

## Qué hacer

1. En el panel de la izquierda, abrí el archivo **`server.js`**
   (dentro de la carpeta `demo`).
2. Mirá las 3 líneas de código, una por una.

## Qué deberías ver

- La primera línea (`DATABASE_URL`) **no debería tener ningún
  aviso** — porque ese dato ya está anotado correctamente en el
  archivo `.env.example` de al lado.
- La segunda línea (`PORT`) puede tener un aviso más suave/tenue —
  porque el código ya tiene un valor de respaldo por si falta (`||
  3000`), así que no es tan urgente.
- La tercera línea (`STRIPE_SECRET_KEY`) **debería tener un aviso
  fuerte** — porque ese dato se usa en el código pero nunca fue
  anotado en ningún lado. Si hacés click sobre el aviso, debería
  ofrecerte un botón para arreglarlo automáticamente (agregarlo al
  archivo de configuración).

## Si algo no se ve así

Sacá la captura igual, y avisame qué línea no coincide con lo de
arriba.
