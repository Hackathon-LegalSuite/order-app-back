# Order App Back

Backend para una plataforma de gestión de pedidos en restaurantes, desarrollado con Spring Boot.

---

## Estado actual

Este proyecto es un esqueleto inicial. Lo que existe hoy:

- Endpoint `/health` funcional
- Estructura de paquetes lista para crecer
- Base de datos H2 en memoria para desarrollo

---

## Stack

| Tecnología      | Versión |
|-----------------|---------|
| Java            | 21      |
| Spring Boot     | 4.0.6   |
| Spring Web MVC  | —       |
| Spring Data JPA | —       |
| Maven           | —       |

**Base de datos por entorno:**

| Entorno    | Base de Datos              |
|------------|----------------------------|
| Desarrollo | H2 (en memoria)            |
| Producción | PostgreSQL *(pendiente)*   |

---

## Estructura del proyecto

```
src/main/java/com/restaurant/order_app/

└── health/
    ├── HealthController.java
    ├── HealthService.java
    └── HealthResponse.java
```

La estructura está preparada para crecer con módulos como `order/`, `menu/`, `user/`, `config/`, `shared/` y `exception/`. Cada módulo manejará sus propias capas: `controller`, `service`, `repository`, `dto` y `entity`.

---

## Cómo correr el proyecto

### Modo desarrollo (H2 en memoria)

No requiere ninguna configuración. Ideal para desarrollar sin depender de una BD externa.

```bash
git clone <repository-url>
./mvnw spring-boot:run
```

La aplicación inicia en `http://localhost:8080`.  
La consola H2 queda disponible en `http://localhost:8080/h2-console`.

---

### Modo producción (PostgreSQL)

Para conectarse a una BD PostgreSQL real (por ejemplo la de Render):

**1. Crear el archivo `.env`** en la raíz del proyecto (no se sube al repo):

```
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://<host>/<dbname>
DB_USER=<usuario>
DB_PASSWORD=<contraseña>
JWT_SECRET=<clave-secreta-minimo-32-caracteres>
```

**2. Crear el archivo `run-local.ps1`** en la raíz del proyecto (no se sube al repo):

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^#][^=]+)=(.+)$') {
        [System.Environment]::SetEnvironmentVariable($Matches[1].Trim(), $Matches[2].Trim(), 'Process')
    }
}

./mvnw spring-boot:run
```

**3. Ejecutarlo:**

```powershell
.\run-local.ps1
```

El script carga las variables del `.env` solo para ese proceso — no modifica el sistema ni otros proyectos.

---

### Perfiles disponibles

| Perfil | Archivo de config | Base de datos | Cómo activar |
|--------|-------------------|---------------|--------------|
| `default` (dev) | `application.yml` | H2 en memoria | `./mvnw spring-boot:run` |
| `prod` | `application-prod.yml` | PostgreSQL | `SPRING_PROFILES_ACTIVE=prod` |

---

## Endpoints disponibles

### `GET /health`

```json
{
  "status": "UP",
  "message": "Backend Running",
  "timestamp": "2026-05-20T15:30:00"
}
```

---

## Roadmap

- [ ] Módulo de menú (CRUD de productos)
- [ ] Módulo de pedidos
- [ ] Módulo de usuarios y roles
- [ ] Autenticación con Spring Security + JWT
- [ ] Migración a PostgreSQL con perfiles de entorno
- [ ] Dockerización
- [ ] Documentación de API con SpringDoc / OpenAPI
- [ ] Despliegue en Render

---

## Autor

Álvaro Clemente — [alvaro.clemente@legalsuitelatam.com](mailto:alvaro.clemente@legalsuitelatam.com)
