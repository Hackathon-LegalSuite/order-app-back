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

```bash
git clone <repository-url>
./mvnw spring-boot:run
```

La aplicación inicia en `http://localhost:8080`.

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
