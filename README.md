# Sistema de préstamos - base técnica

Base de la prueba técnica: Spring Boot + Angular + PostgreSQL, empaquetada para arrancar los tres servicios con Docker Compose. Incluye autenticación con JWT, gestión de usuarios, dashboard por rol y solicitudes de préstamo.

## Estructura

```text
backend/    Spring Boot 4.1.2, Java 25, Maven, JWT, JPA, Flyway
frontend/   Angular 22.2, Vite 8 (interno), Tailwind CSS 4.3, ESLint + Stylelint
database/   PostgreSQL 18.6, rol de aplicación y migraciones Flyway
certs/      Certificados TLS locales generados por máquina (ignorados por Git)
scripts/    Utilidades de entorno local
start.sh    Arranque local de un comando (Git Bash/macOS/Linux)
start.cmd   Arranque local de un comando (Windows)
compose.yaml
.env        Secretos locales generados (ignorado por Git)
```

## Stack y versiones

| Pieza | Versión | Nota |
| --- | --- | --- |
| Spring Boot | 4.1.2 | Última estable en Maven Central |
| Java | 25 LTS | Imagen Eclipse Temurin 25 |
| Maven | 3.10.0 | Build multi-stage con caché |
| Angular | 22.2.3 (CLI 22.2.5) | Standalone, routing, Vitest |
| Vite | 8.3.5 | Lo usa `@angular/build` internamente |
| Tailwind CSS | 4.3.3 | Plugin oficial de PostCSS |
| Node | 24.22.1 LTS | Build del frontend |
| PostgreSQL | 18.6 | 19 todavía está en RC; no se usa en producción |
| Nginx | 1.30.6 | Estable, TLS local y proxy `/api` |
| ESLint | 10.13.0 | Angular ESLint 22.6, flat config |
| Stylelint | 17.16.0 | CSS, con reglas de Tailwind |

## Arranque con un solo comando

```bash
./start.sh          # Git Bash, macOS o Linux
.\start.cmd         # Windows CMD o PowerShell, desde la raíz
```

El comando prepara el entorno y levanta los tres servicios. No hace falta ejecutar `mkcert` ni editar `hosts` a mano:

1. Crea `.env` con secretos aleatorios si no existe.
2. Comprueba el certificado de `certs/`:
   - Si existe, es válido, cubre el dominio y coincide con su clave, **lo reutiliza**.
   - Si falta, caducó o no coincide, instala `mkcert` si hace falta y genera uno nuevo.
3. Ejecuta `mkcert -install` para confiar en la CA local.
4. Añade `127.0.0.1 prueba.somosmakers.com` a `hosts`. Si no hay permisos, abre UAC (Windows) o pide `sudo` (macOS/Linux) una sola vez.
5. Ejecuta `docker compose up --build` en primer plano.

La primera ejecución en una máquina puede mostrar avisos de administrador (UAC o `sudo`) para confiar en la CA y editar `hosts`; si aparecen, ocurren una sola vez. Después, el mismo comando solo valida y arranca. Para levantar en segundo plano: `./start.sh -d`.

Si `./start.sh` da `Permission denied` porque el bit de ejecución no se conservó al clonar, usa `bash start.sh`.

Requisito: Node >= 24.15 y Docker. El arranque instala `mkcert` automáticamente con `winget` (Windows) o `brew` (macOS); en Linux muestra el enlace de instalación si no está disponible.

## HTTPS local: https://prueba.somosmakers.com

El frontend se sirve por HTTPS en `prueba.somosmakers.com`. Cada máquina necesita dos cosas locales que **no se pueden compartir de forma segura**: resolver el dominio a `127.0.0.1` y confiar en una CA local. El arranque automatiza ambas; solo la aprobación del sistema operativo queda en manos de la persona que ejecuta el comando.

Si cancelas el aviso de administrador o tu sistema no permite la elevación, añade la línea manualmente y vuelve a ejecutar:

```powershell
# Windows: PowerShell como administrador
Add-Content -Path "C:\Windows\System32\drivers\etc\hosts" -Value "127.0.0.1 prueba.somosmakers.com"
```

```bash
# macOS / Linux
echo "127.0.0.1 prueba.somosmakers.com" | sudo tee -a /etc/hosts
```

Después, abre **https://prueba.somosmakers.com**. Nginx escucha en 80 y 443; el puerto 80 redirige a HTTPS. Si esos puertos están ocupados, ajusta los mapeos de `frontend` en `compose.yaml`.

Notas:

- El certificado y la clave privada están en `certs/` y **nunca se commitean**. Cada clon genera los suyos.
- Chrome/Edge usan el almacén de Windows; `mkcert -install` ya deja la CA confiada. Firefox puede requerir importar manualmente `rootCA.pem`.
- `curl` en Windows con Schannel puede fallar por revocación (`revocation status is unknown`) aunque el certificado sea válido; usa `--ssl-no-revoke` para pruebas locales. Los navegadores no tienen ese problema.
- Si controlas el DNS público de `somosmakers.com`, puedes crear un registro `A 127.0.0.1` para `prueba` y evitar la edición de `hosts`; la CA local sigue siendo necesaria. No uses ese registro para un sitio público real.

## Servicios

Con el stack arrancado:

- Frontend: https://prueba.somosmakers.com
- Alternativa HTTP: http://localhost:4200 (redirige a HTTPS)
- API directa: http://localhost:8080
- Salud del backend: http://localhost:8080/actuator/health
- Salud del frontend: https://prueba.somosmakers.com/healthz

En este workspace ya existe un `.env` local con secretos aleatorios (ignorado por Git). En un clon nuevo lo crea el arranque; si prefieres hacerlo a mano:

```bash
cp .env.example .env
# Edita .env; JWT_SECRET debe ser Base64 de >= 32 bytes.
```

Comandos útiles:

```bash
docker compose ps
docker compose logs -f backend
docker compose down
docker compose down -v   # destructivo: borra la base local
```

## Usuarios base

Estos usuarios los crea `database/migrations/V4__seed_default_users.sql` en una base nueva. Son credenciales de demostración: cámbialas antes de desplegar.

| Rol | Nombre | Correo | Contraseña |
| --- | --- | --- | --- |
| ADMIN | Andres Valencia | `admin@test.com` | `admin123` |
| USER | Cesar Rios | `usuario@test.com` | `user1234` |

## Respuestas teóricas

Las respuestas están en [frontend/public/teoria.html](frontend/public/teoria.html) y se sirven en https://prueba.somosmakers.com/teoria.html.

## Desarrollo local

Requisitos: Node >= 24.15 (Angular 22 lo exige; hay un `frontend/.nvmrc` con 24.22.1 para `nvm use`), Java 25 y Docker para la base.

```bash
# Base de datos
docker compose up -d db

# Backend
cd backend
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/prestamos
export SPRING_DATASOURCE_USERNAME=prestamos_app
export SPRING_DATASOURCE_PASSWORD='...'   # APP_DB_PASSWORD del .env
export JWT_SECRET='...'                    # JWT_SECRET del .env
mvn spring-boot:run

# Frontend (otra terminal)
cd frontend
npm install
npm start
```

El dev server de Angular queda en http://localhost:4200 y su proxy reenvía `/api` al backend. El dominio HTTPS se sirve desde Nginx en Docker, no desde `ng serve`.

## Tests

```bash
cd backend && mvn test                     # contexto Spring + Testcontainers PostgreSQL 18.6
cd frontend && npm test -- --watch=false    # Vitest, sin modo watch
```

Los tests del backend usan Testcontainers; Docker debe estar corriendo. La imagen de PostgreSQL está fijada a `18.6-alpine`.

## Alcance actual

Incluye autenticación con JWT y refresh token, CRUD de usuarios, login/registro, dashboard por rol, 404 y solicitudes de préstamo con aprobación/rechazo. Queda pendiente rotar las credenciales de demostración antes de un entorno real.
