# Certificados locales

Esta carpeta contiene material TLS **generado en cada máquina** y está ignorada por Git. Nunca subas la clave privada ni el certificado a GitHub.

No hace falta crearlos a mano: el arranque del proyecto los valida y resuelve.

```bash
./start.sh          # Git Bash, macOS o Linux
.\start.cmd         # Windows CMD o PowerShell
```

El arranque:

1. Crea `.env` con secretos aleatorios si todavía no existe.
2. Si `prueba.somosmakers.com.pem` y su clave existen, están vigentes, cubren el dominio y coinciden, **los reutiliza**.
3. Si faltan, caducaron o no coinciden, instala `mkcert` si hace falta y genera un certificado nuevo con SAN para `prueba.somosmakers.com`, `localhost` y `127.0.0.1`.
4. Ejecuta `mkcert -install` para que el sistema confíe en la CA local.
5. Añade `127.0.0.1 prueba.somosmakers.com` a `hosts`; si no hay permisos, pide UAC/sudo una vez.

Para forzar una regeneración, borra los dos archivos y vuelve a arrancar:

```bash
rm certs/prueba.somosmakers.com.pem certs/prueba.somosmakers.com-key.pem
./start.sh
```

La preparación HTTPS forma parte del mismo comando de arranque; no hay un paso separado.

Cada persona que clone el repositorio repite esto una vez en su máquina. La confianza del navegador se gestiona con la CA local de `mkcert`; no se puede compartir una clave privada de forma segura.

Si el dominio se controla por DNS público, se puede usar un registro `A 127.0.0.1` para evitar la edición de `hosts`, pero la CA local seguirá siendo necesaria. No uses ese registro para un sitio público real.

Los certificados de `mkcert` caducan; el arranque detecta la caducidad y genera uno nuevo.

En Docker Desktop el bind mount puede mostrar permisos amplios sobre estos archivos. Es aceptable en local; en producción usa Docker secrets o el almacén de secretos del proveedor, nunca un bind mount con la clave privada.
