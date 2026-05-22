# Guía de implementación — Order App Back

Cada paso es un bloque entregable. Completá uno antes de pasar al siguiente.

---

## Paso 1 — Creación y conexión a la base de datos

### 1.1 Crear la base de datos en Render

1. Ingresá a [render.com](https://render.com) → **New** → **PostgreSQL**
2. Nombre: `order-app-db` — Free tier
3. Render te entrega:
   - **Internal Database URL** (para usar desde el mismo servicio en Render)
   - **External Database URL** (para conectarte desde tu máquina local si querés)
4. Guardá el **Internal Database URL** — lo vas a usar como variable de entorno

### 1.2 Configurar perfiles de Spring Boot

Creá el archivo `src/main/resources/application-prod.yml`:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update
    show-sql: false

  sql:
    init:
      mode: always

  jpa:
    defer-datasource-initialization: true
```

> `defer-datasource-initialization: true` es obligatorio cuando usás JPA + `data.sql`. Sin esto, Spring intenta ejecutar el SQL antes de que Hibernate cree las tablas.

El `application.yml` existente (H2) sigue siendo el perfil de desarrollo. En Render configurás la variable de entorno `SPRING_PROFILES_ACTIVE=prod`.

### 1.3 Variables de entorno en Render

En el servicio de Render → **Environment** → agregar:

| Variable | Valor |
|----------|-------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | `jdbc:postgresql://<host>/<db>` (del Internal URL) |
| `DB_USER` | usuario de Render |
| `DB_PASSWORD` | contraseña de Render |

### 1.4 Seed data

Creá `src/main/resources/data.sql` con los datos iniciales. Este archivo se ejecuta automáticamente en cada inicio cuando `sql.init.mode=always`.

Orden obligatorio por dependencias de FK:

```sql
-- 1. Usuarios (cocineros y meseros)
INSERT INTO usuarios (id, username, password, nombre, rol)
VALUES
  (1, 'cocinero01', '$2a$10$...', 'Carlos',  'COCINERO'),
  (2, 'mesero01',   '$2a$10$...', 'Álvaro',  'MESERO'),
  (3, 'mesero02',   '$2a$10$...', 'Deivy',   'MESERO')
ON CONFLICT (id) DO NOTHING;

-- 2. Mesas (requiere mesero FK)
INSERT INTO mesas (id, numero, codigo_qr, mesero_id)
VALUES
  (1, 1, 'MESA-01', 2),
  (2, 2, 'MESA-02', 2),
  (3, 3, 'MESA-03', 3),
  (4, 4, 'MESA-04', 3),
  (5, 5, 'MESA-05', 2)
ON CONFLICT (id) DO NOTHING;

-- 3. Ingredientes
INSERT INTO ingredientes (id, nombre) VALUES
  (1, 'Carne de res'),
  (2, 'Lechuga'),
  (3, 'Tomate'),
  (4, 'Cebolla'),
  (5, 'Pan'),
  (6, 'Arroz'),
  (7, 'Fríjoles'),
  (8, 'Chicharrón'),
  (9, 'Limón'),
  (10, 'Coco')
ON CONFLICT (id) DO NOTHING;

-- 4. Platos
INSERT INTO platos (id, nombre, descripcion, precio, categoria, preparada) VALUES
  (1, 'Hamburguesa Clásica', 'Carne de res, lechuga, tomate y papas fritas', 18500, 'PLATO_FUERTE', null),
  (2, 'Bandeja Paisa',       'Arroz, fríjoles, chicharrón y carne',          32000, 'PLATO_FUERTE', null),
  (7, 'Limonada de Coco',    'Limonada natural con coco',                     8000, 'BEBIDA',        true)
ON CONFLICT (id) DO NOTHING;

-- 5. Relación plato-ingrediente
INSERT INTO plato_ingredientes (id, plato_id, ingrediente_id, obligatorio) VALUES
  (1, 1, 1, true),
  (2, 1, 2, false),
  (3, 1, 3, false),
  (4, 1, 4, false),
  (5, 2, 6, true),
  (6, 2, 7, true),
  (7, 2, 8, true),
  (8, 7, 9, true),
  (9, 7, 10, true)
ON CONFLICT (id) DO NOTHING;
```

> Las contraseñas en `data.sql` deben estar hasheadas con BCrypt. Podés generarlas con un [BCrypt generator online](https://bcrypt-generator.com/) o con un test de Spring.

---

## Paso 2 — Autenticación

### Dependencias necesarias

Agregar en `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

### Archivos a crear

```
usuario/
├── Usuario.java              → @Entity con campos: id, username, password, nombre, rol
├── UsuarioRepository.java    → findByUsername(String username)
└── Rol.java                  → @Enum: COCINERO, MESERO

mesa/
├── Mesa.java                 → @Entity con campos: id, numero, codigoQr, mesero (FK Usuario)
└── MesaRepository.java       → findByCodigoQr(String codigoQr)

auth/
├── AuthController.java
├── AuthService.java
├── JwtUtil.java
├── JwtFilter.java            → OncePerRequestFilter — valida token en cada request
├── SecurityConfig.java       → configura rutas públicas y protegidas
├── ClienteAuthRequest.java   → DTO: nombre, codigoMesa
├── StaffAuthRequest.java     → DTO: username, password
└── AuthResponse.java         → DTO: token, rol, nombre, mesaId, expiresIn
```

### Endpoints implementados

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/auth/cliente` | Valida nombre + codigoMesa → genera JWT con mesaId en claims |
| `POST` | `/auth/login` | Valida username + password → genera JWT con rol en claims |

### Configuración clave

- `SecurityConfig` debe permitir sin autenticación: `POST /auth/**`
- El resto de rutas requieren JWT válido
- El `JwtFilter` extrae el rol del token y arma el `SecurityContext`
- JWT secreto como variable de entorno: `JWT_SECRET`

### Variable de entorno adicional

| Variable | Valor |
|----------|-------|
| `JWT_SECRET` | cadena aleatoria de 32+ caracteres |

---

## Paso 3 — Menú (Platos)

### Archivos a crear

```
ingrediente/
├── Ingrediente.java          → @Entity con @ElementCollection para caracteristicas
├── IngredienteRepository.java
└── Caracteristica.java       → @Enum: PICANTE, SALADO, DULCE, ACIDO, AMARGO, ALCOHOL, CALIENTE, FRIO, VEGETARIANO

plato/
├── Plato.java                → @Entity
├── PlatoIngrediente.java     → @Entity tabla intermedia (plato, ingrediente, obligatorio)
├── PlatoIngredienteRepository.java
├── PlatoRepository.java      → findByCategoria(Categoria categoria)
├── PlatoService.java
├── PlatoController.java
├── Categoria.java            → @Enum: ENTRADA, BEBIDA, PLATO_FUERTE, POSTRE
└── PlatoResponse.java        → DTO de respuesta con lista de ingredientes
```

### Endpoints implementados

| Método | Endpoint | Acceso |
|--------|----------|--------|
| `GET` | `/platos` | CLIENTE (JWT válido) |
| `GET` | `/platos/{id}` | CLIENTE |
| `GET` | `/platos/categoria/{categoria}` | CLIENTE |

### Notas de implementación

- La respuesta incluye la lista de ingredientes con su flag `obligatorio` — esto viene de `PlatoIngrediente`, no de `Ingrediente` directamente
- `preparada` solo aplica a `BEBIDA`, puede ser `null` en otros casos

---

## Paso 4 — Pedidos

### Archivos a crear

```
item/
├── ItemPedido.java           → @Entity
├── ItemRepository.java
├── ItemService.java          → lógica de validación de transición de estados
└── EstadoItem.java           → @Enum: EN_ESPERA, EN_PROGRESO, LISTO, ENTREGADO

pedido/
├── Pedido.java               → @Entity
├── PedidoRepository.java     → findByMesaId(Long mesaId)
├── PedidoService.java
├── PedidoController.java
├── CrearPedidoRequest.java   → DTO: mesaId, clienteNombre, items[]
├── ItemRequest.java          → DTO: platoId, ingredientesExcluidos[]
└── PedidoResponse.java       → DTO de respuesta
```

### Endpoints implementados

| Método | Endpoint | Acceso |
|--------|----------|--------|
| `POST` | `/pedidos` | CLIENTE |
| `GET`  | `/pedidos/mesa/{mesaId}` | CLIENTE |

### Validaciones obligatorias

- Al crear el pedido, verificar que cada `ingredienteId` en `ingredientesExcluidos` tenga `obligatorio: false` en `PlatoIngrediente`
- Si alguno tiene `obligatorio: true` → `400 Bad Request` con mensaje descriptivo
- El `mesaId` del JWT del cliente debe coincidir con el `mesaId` del body

---

## Paso 5 — Cocina

### Archivos a crear

```
cocina/
├── CocinaController.java     → /cocina/platos, /cocina/items/{itemId}/estado
├── CocinaService.java
└── CocinaItemResponse.java   → DTO: itemId, pedidoId, mesa, clienteNombre, plato, ingredientesExcluidos, estado
```

### Endpoints implementados

| Método | Endpoint | Acceso |
|--------|----------|--------|
| `GET`  | `/cocina/platos` | COCINERO |
| `PATCH`| `/cocina/items/{itemId}/estado` | COCINERO |

### Notas de implementación

- `GET /cocina/platos` filtra `ItemPedido` donde `estado IN (EN_ESPERA, EN_PROGRESO)`
- `PATCH` valida que la transición sea válida (solo avanzar, solo hasta `LISTO`)
- Un `COCINERO` que intente hacer `LISTO → ENTREGADO` → `400 Bad Request`

### Lógica de transición (en `ItemService`)

```
EN_ESPERA   → puede avanzar a: EN_PROGRESO
EN_PROGRESO → puede avanzar a: LISTO
LISTO       → no puede ser modificado por COCINERO
ENTREGADO   → estado final, no modificable
```

---

## Paso 6 — Despacho

### Archivos a crear

```
despacho/
├── DespachoController.java   → /despacho/platos, /despacho/items/{itemId}/estado
├── DespachoService.java
└── DespachoItemResponse.java → DTO: itemId, pedidoId, mesa, clienteNombre, meseroEncargado, plato, ingredientesExcluidos, estado
```

### Endpoints implementados

| Método | Endpoint | Acceso |
|--------|----------|--------|
| `GET`  | `/despacho/platos` | MESERO, COCINERO |
| `PATCH`| `/despacho/items/{itemId}/estado` | MESERO |

### Notas de implementación

- `GET /despacho/platos` filtra `ItemPedido` donde `estado = LISTO`
- El campo `meseroEncargado` se resuelve desde `Mesa → Usuario (MESERO)` — viene del join, no del JWT
- `PATCH /despacho/items/{itemId}/estado` solo acepta `{ "estado": "ENTREGADO" }` y solo desde `LISTO`
- Un `COCINERO` puede ver la pantalla pero no puede ejecutar el PATCH

---

## Resumen de orden de implementación

| Paso | Qué se construye | Endpoints habilitados |
|------|------------------|-----------------------|
| 1 | BD en Render + perfiles + seed data | — |
| 2 | Auth + JWT + Security + Usuario + Mesa | `POST /auth/cliente`, `POST /auth/login` |
| 3 | Ingrediente + Plato + PlatoIngrediente | `GET /platos`, `GET /platos/{id}`, `GET /platos/categoria/{cat}` |
| 4 | Pedido + ItemPedido + EstadoItem | `POST /pedidos`, `GET /pedidos/mesa/{mesaId}` |
| 5 | Cocina | `GET /cocina/platos`, `PATCH /cocina/items/{itemId}/estado` |
| 6 | Despacho | `GET /despacho/platos`, `PATCH /despacho/items/{itemId}/estado` |
