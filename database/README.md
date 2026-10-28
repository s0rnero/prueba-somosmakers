# Base de datos

PostgreSQL 18.6 sobre Alpine. Esta carpeta contiene la imagen de la base, el script de arranque que crea el rol de aplicación y las migraciones versionadas con Flyway.

## Contenido

| Ruta | Uso |
| --- | --- |
| `Dockerfile` | Imagen `postgres:18.6-alpine` con los scripts de inicialización. |
| `init/01-create-app-role.sh` | Crea el rol de aplicación sin privilegios de superusuario. |
| `migrations/` | Migraciones Flyway; el POM del backend las copia al classpath en `db/migration`. |

## Variables de entorno

| Variable | Uso |
| --- | --- |
| `POSTGRES_DB` | Nombre de la base. |
| `POSTGRES_USER` | Superusuario de bootstrap de la imagen. |
| `POSTGRES_PASSWORD` | Contraseña del superusuario de bootstrap. |
| `APP_DB_USER` | Rol con el que conecta Spring Boot. |
| `APP_DB_PASSWORD` | Contraseña de ese rol. |

El rol de aplicación solo puede conectarse y crear objetos en el esquema `public`; no es superusuario ni puede crear bases o roles.

## Inicialización

Los scripts de `init/` se ejecutan **una sola vez**, cuando el volumen de datos está vacío. Si se cambia `APP_DB_PASSWORD` o se añade otro script de arranque, hay que recrear el volumen para que se vuelva a ejecutar (en desarrollo: `docker compose down -v`; es destructivo).

El esquema no se crea aquí: Flyway es el dueño de la evolución del esquema y aplica `migrations/V1__create_users.sql` al arrancar el backend.

## Conexión

```bash
docker compose exec db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Para entrar con el rol de aplicación:

```bash
docker compose exec db sh -c 'psql -U "$APP_DB_USER" -d "$POSTGRES_DB"'
```

No reutilices las contraseñas de desarrollo en producción. Usa secretos gestionados por el entorno y rota las credenciales fuera de la base (nunca en Git).
