# Order App Back

Backend para una plataforma de gestión de pedidos en restaurantes, desarrollado con Spring Boot.

---

## Stack

| Tecnología      | Versión |
|-----------------|---------|
| Java            | 21      |
| Spring Boot     | 4.0.6   |
| Spring Web MVC  | —       |
| Spring Data JPA | —       |
| Spring Security | —       |
| jjwt            | 0.12.6  |
| Lombok          | —       |
| Maven           | —       |

**Base de datos por entorno:**

| Entorno    | Base de Datos   |
|------------|-----------------|
| Desarrollo | H2 (en memoria) |
| Producción | PostgreSQL      |

---

## Módulos implementados

| Módulo | Endpoints | Descripción |
|--------|-----------|-------------|
| Auth | `POST /auth/cliente/{mesaId}`, `POST /auth/login` | JWT para clientes (por QR) y staff (usuario/contraseña) |
| Platos | `GET /platos`, `GET /platos/{id}`, `GET /platos/categoria/{cat}` | Menú con ingredientes y flag obligatorio |
| Pedidos | `POST /pedido`, `GET /pedido`, `PATCH /pedido/item/{id}/estado`, `DELETE /pedido/{id}/item/{id}` | Gestión completa de pedidos por rol |
| Búsqueda IA | `POST /menu/buscar` | Búsqueda en lenguaje natural usando Groq / Llama 3.1 |

---

## Cómo correr el proyecto

### Modo desarrollo (H2 en memoria)

No requiere configuración. La BD se crea en memoria al iniciar.

```bash
git clone <repository-url>
./mvnw spring-boot:run
```

La aplicación inicia en `http://localhost:8080`.
La consola H2 queda disponible en `http://localhost:8080/h2-console`.

---

### Modo producción (PostgreSQL)

**1. Crear `.env`** en la raíz (no se sube al repo):

```
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://<host>/<dbname>
DB_USER=<usuario>
DB_PASSWORD=<contraseña>
JWT_SECRET=<clave-secreta-minimo-32-caracteres>
GROQ_API_KEY=gsk_...
```

**2. Crear `run-local.ps1`** en la raíz (no se sube al repo):

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^#][^=]+)=(.+)$') {
        [System.Environment]::SetEnvironmentVariable($Matches[1].Trim(), $Matches[2].Trim(), 'Process')
    }
}
./mvnw spring-boot:run
```

**3. Ejecutar:**

```powershell
.\run-local.ps1
```

---

### Perfiles disponibles

| Perfil | Archivo | Base de datos | Cómo activar |
|--------|---------|---------------|--------------|
| `default` (dev) | `application.yml` | H2 en memoria | `./mvnw spring-boot:run` |
| `prod` | `application-prod.yml` | PostgreSQL | `SPRING_PROFILES_ACTIVE=prod` |

---

## Variables de entorno

| Variable | Descripción | Requerida en |
|----------|-------------|--------------|
| `DB_URL` | JDBC URL de PostgreSQL | prod |
| `DB_USER` | Usuario de la BD | prod |
| `DB_PASSWORD` | Contraseña de la BD | prod |
| `JWT_SECRET` | Clave para firmar tokens JWT (mín. 32 chars) | todos |
| `GROQ_API_KEY` | API key de Groq — obtener en [console.groq.com/keys](https://console.groq.com/keys) | todos |

---

## Estructura de paquetes

```
src/main/java/com/restaurant/order_app/
├── auth/           → JWT, filtros, Security config
├── usuario/        → @Entity Usuario, Rol (COCINERO, MESERO)
├── mesa/           → @Entity Mesa
├── ingrediente/    → @Entity Ingrediente, Caracteristica
├── plato/          → @Entity Plato, PlatoIngrediente, Categoria
├── item/           → @Entity ItemPedido, EstadoItem
├── pedido/         → @Entity Pedido, lógica de negocio por rol
├── busqueda/       → GroqClient, BusquedaService, DTOs de IA
├── config/         → RestTemplate, ObjectMapper beans
└── exception/      → MensajeResponse record
```

---

## Documentación adicional

| Recurso | Contenido |
|---------|-----------|
| `docs/Resumen de requerimientos.md` | Endpoints completos, respuestas, validaciones y arquitectura |
| `docs/Guia de implementacion.md` | Paso a paso de implementación con seed data |
| [API Dog](https://f6ixzhukyd.apidog.io/) | Documentación interactiva de todos los endpoints |

---

## Autor

Álvaro Clemente — [alvaro.clemente@legalsuitelatam.com](mailto:alvaro.clemente@legalsuitelatam.com)
