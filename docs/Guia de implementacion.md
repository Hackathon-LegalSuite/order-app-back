# Guía de implementación — Order App Back

Cada paso es un bloque entregable. Completá uno antes de pasar al siguiente.

---

## Paso 1 — Creación y conexión a la base de datos

### 1.1 Crear la base de datos en Render

1. Ingresá a [render.com](https://render.com) → **New** → **PostgreSQL**
2. Nombre: `order-app-db` — Free tier
3. Render te entrega:
   - **Internal Database URL** — para el servicio desplegado en Render
   - **External Database URL** — para conectarte desde tu máquina o editor local
4. Convertir la URL al formato JDBC: `postgresql://...` → `jdbc:postgresql://...`

### 1.2 Perfiles de Spring Boot

| Perfil | Archivo | Base de datos | Cómo activar |
|--------|---------|---------------|--------------|
| `default` (dev) | `application.yml` | H2 en memoria | `./mvnw spring-boot:run` |
| `prod` | `application-prod.yml` | PostgreSQL | `SPRING_PROFILES_ACTIVE=prod` |

**`application.yml`** (dev):
```yaml
spring:
  application:
    name: order-app-back
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    open-in-view: false
  h2:
    console:
      enabled: true
  sql:
    init:
      mode: never
server:
  port: ${PORT:8080}
```

**`application-prod.yml`** (producción):
```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    open-in-view: false
    defer-datasource-initialization: true
  sql:
    init:
      mode: never
```

> `sql.init.mode: never` en ambos perfiles — el seed data se carga manualmente una sola vez por paso.

### 1.3 Variables de entorno en Render

| Variable | Valor |
|----------|-------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | `jdbc:postgresql://<internal-host>/<dbname>` |
| `DB_USER` | usuario de Render |
| `DB_PASSWORD` | contraseña de Render |
| `JWT_SECRET` | cadena aleatoria de mínimo 32 caracteres |

### 1.4 Ejecución local contra la BD de Render

Crear `.env` en la raíz (ignorado por git):
```
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://<external-host>/<dbname>
DB_USER=<usuario>
DB_PASSWORD=<contraseña>
JWT_SECRET=<clave-secreta>
```

Crear `run-local.ps1` en la raíz (ignorado por git):
```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^#][^=]+)=(.+)$') {
        [System.Environment]::SetEnvironmentVariable($Matches[1].Trim(), $Matches[2].Trim(), 'Process')
    }
}
./mvnw spring-boot:run
```

Ejecutar con: `.\run-local.ps1`

---

## Paso 2 — Autenticación

### Dependencias a agregar en `pom.xml`

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
├── Usuario.java              → @Entity — implementa UserDetails para Spring Security
├── UsuarioRepository.java    → findByUsername(String username)
└── Rol.java                  → @Enum: COCINERO, MESERO

mesa/
├── Mesa.java                 → @Entity con campos: id, numero, codigoQr, mesero (FK Usuario)
└── MesaRepository.java       → findById(Long id) — heredado de JpaRepository

auth/
├── AuthController.java
├── AuthService.java
├── JwtUtil.java              → genera y valida tokens JWT
├── JwtFilter.java            → OncePerRequestFilter — valida token en cada request
├── AuthEntryPoint.java       → respuesta 401 estructurada cuando el JWT falla o no existe
├── SecurityConfig.java       → rutas públicas, sesión stateless, registro de filtros
└── dto/
    ├── ClienteAuthRequest.java   → nombre, codigoMesa
    ├── StaffAuthRequest.java     → username, password
    └── AuthResponse.java         → token, rol, nombre, mesaId, expiresIn
```

> `@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})` en la clase principal para eliminar el warning del password generado automáticamente.

### Endpoints implementados

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/auth/cliente/{mesaId}` | Valida que el codigoMesa del body corresponda a la mesa del path. Genera JWT de 6h con mesaId en claims |
| `POST` | `/auth/login` | Valida username + password (BCrypt). Genera JWT con rol en claims |

### Notas de implementación

- El `JwtFilter` distingue token expirado (`ExpiredJwtException`) de token inválido y marca el error como atributo del request
- El `AuthEntryPoint` lee ese atributo y devuelve el mensaje correcto con status 401
- Contraseñas en BD deben estar hasheadas con BCrypt — generarlas en [bcrypt.online](https://bcrypt.online) con cost 10

### Script de seed (ejecutar una vez en el editor)

```sql
INSERT INTO usuarios (id, username, password, nombre, rol) VALUES
  (1, 'cocinero01', '<hash-bcrypt-de-1234>', 'Carlos', 'COCINERO'),
  (2, 'mesero01',   '<hash-bcrypt-de-1234>', 'Álvaro', 'MESERO'),
  (3, 'mesero02',   '<hash-bcrypt-de-1234>', 'Deivy',  'MESERO');

INSERT INTO mesas (id, numero, codigo_qr, mesero_id) VALUES
  (1, 1, 'MESA-01', 2),
  (2, 2, 'MESA-02', 2),
  (3, 3, 'MESA-03', 3),
  (4, 4, 'MESA-04', 3),
  (5, 5, 'MESA-05', 2);

```

---

## Paso 3 — Menú (Platos)

### Archivos a crear

```
ingrediente/
├── Ingrediente.java            → @Entity con relación @ManyToMany a Caracteristica
├── IngredienteRepository.java
└── Caracteristica.java         → @Entity (no enum) — permite agregar características desde BD sin tocar código

plato/
├── Plato.java                  → @Entity — incluye campo imagenUrl
├── PlatoIngrediente.java       → @Entity tabla intermedia (plato, ingrediente, obligatorio)
├── PlatoRepository.java        → findByCategoria(Categoria categoria)
├── PlatoService.java
├── PlatoController.java
├── Categoria.java              → @Enum: ENTRADA, BEBIDA, PLATO_FUERTE, POSTRE
└── dto/
    ├── PlatoResponse.java          → id, nombre, descripcion, precio, categoria, preparada, imagenUrl, ingredientes
    └── IngredienteEnPlatoResponse.java → id, nombre, obligatorio
```

> `Caracteristica` es una entidad con su propia tabla, no un enum. Esto permite agregar nuevas características insertando en BD sin necesidad de deploy.

### Endpoints implementados en este paso

| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| `GET` | `/platos` | CLIENTE | Retorna todos los platos del menú con ingredientes y flag obligatorio |
| `GET` | `/platos/{id}` | CLIENTE | Retorna el detalle de un plato específico por su ID |
| `GET` | `/platos/categoria/{categoria}` | CLIENTE | Filtra platos por categoría: `ENTRADA`, `BEBIDA`, `PLATO_FUERTE`, `POSTRE` |

### Notas de implementación

- `obligatorio` viene de `PlatoIngrediente`, no de `Ingrediente`
- `preparada` y `imagenUrl` usan `@JsonInclude(NON_NULL)` — no aparecen si son null
- La relación `ingrediente_caracteristicas` es una tabla ManyToMany entre `Ingrediente` y `Caracteristica`

### Script de seed (ejecutar una vez en el editor)

```sql

CREATE TABLE ingrediente_caracteristicas (
    ingrediente_id    BIGINT NOT NULL REFERENCES ingredientes(id),
    caracteristica_id BIGINT NOT NULL REFERENCES caracteristicas(id),
    PRIMARY KEY (ingrediente_id, caracteristica_id)
);

INSERT INTO caracteristicas (id, nombre) VALUES
  (1,'Picante'),(2,'Salado'),(3,'Dulce'),
  (4,'Acido'),(5,'Amargo'),(6,'Alcohol'),
  (7,'Caliente'),(8,'Frio'),(9,'Vegetariano');

INSERT INTO ingredientes (id, nombre) VALUES
  (1,'Carne de res'),(2,'Lechuga'),(3,'Tomate'),
  (4,'Cebolla'),(5,'Pan'),(6,'Arroz'),
  (7,'Fríjoles'),(8,'Chicharrón'),(9,'Limón'),(10,'Coco');

INSERT INTO platos (id, nombre, descripcion, precio, categoria, preparada) VALUES
  (1,'Hamburguesa Clásica','Carne de res, lechuga, tomate y papas fritas',18500,'PLATO_FUERTE',null),
  (2,'Bandeja Paisa','Arroz, fríjoles, chicharrón y carne',32000,'PLATO_FUERTE',null),
  (3,'Ensalada César','Lechuga, tomate, crutones y aderezo',9500,'ENTRADA',null),
  (4,'Limonada de Coco','Limonada natural con coco',8000,'BEBIDA',true),
  (5,'Agua Mineral','Agua mineral embotellada',3000,'BEBIDA',false);

INSERT INTO plato_ingredientes (plato_id, ingrediente_id, obligatorio) VALUES
  (1,1,true),(1,2,false),(1,3,false),(1,4,false),(1,5,true),
  (2,1,true),(2,6,true),(2,7,true),(2,8,true),
  (3,2,true),(3,3,false),
  (4,9,true),(4,10,true),
  (5,9,false);

INSERT INTO ingrediente_caracteristicas (ingrediente_id, caracteristica_id) VALUES
  (1,2),(1,7),(2,9),(3,9),(3,4),(4,9),(4,1),
  (5,9),(6,9),(7,9),(8,2),(9,4),(9,3),(10,3),(10,9);

ALTER TABLE caracteristicas ALTER COLUMN id RESTART WITH 100;
ALTER TABLE ingredientes ALTER COLUMN id RESTART WITH 100;
ALTER TABLE platos ALTER COLUMN id RESTART WITH 100;
```

> Para cargar imágenes en los platos: `UPDATE platos SET imagen_url = '<url>' WHERE id = <id>;`

---

## Paso 4 — Pedidos

### Archivos creados

```
item/
├── ItemPedido.java       → @Entity — plato + ingredientes excluidos + estado
├── ItemRepository.java
└── EstadoItem.java       → @Enum: EN_ESPERA, EN_PROGRESO, LISTO, ENTREGADO

pedido/
├── Pedido.java               → @Entity — mesa, clienteNombre, clienteSessionId, items, creadoEn
├── PedidoRepository.java     → findByClienteSessionId(String, Sort), findByMesaIdOrderByCreadoEnDesc(Long)
├── PedidoService.java
├── PedidoController.java
└── dto/
    ├── CrearPedidoRequest.java    → items[] (mesa y nombre se extraen del JWT)
    ├── ItemRequest.java           → platoId, ingredientesExcluidos[]
    ├── ItemResponse.java          → id, platoId, platoNombre, ingredientesExcluidos, estado
    ├── PedidoResponse.java        → id, mesaId, clienteNombre, clienteSessionId, creadoEn, items
    └── ConsultaItemResponse.java  → pedidoId, itemId, platoId, platoNombre, precio, imagenUrl,
                                     ingredientes[], ingredientesExcluidos[], estado, mesa?, mesero?
```

### Endpoints implementados

| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| `POST` | `/pedido` | CLIENTE | Crea un nuevo pedido. Mesa y nombre del cliente se extraen del JWT |
| `GET`  | `/pedido` | TODOS | Retorna ítems filtrados según el rol del token (ver lógica abajo) |
| `PATCH` | `/pedido/item/{itemId}/estado` | TODOS | Avanza el estado del ítem al siguiente en la secuencia |
| `DELETE` | `/pedido/{pedidoId}/item/{itemId}` | CLIENTE | Elimina un ítem del pedido. Si era el último, elimina el pedido |

### Lógica del GET /pedido por rol

| Rol | Ítems que ve | Filtro de estado | Mesa y mesero |
|-----|-------------|-----------------|---------------|
| CLIENTE | Solo los de su sesión (`clienteSessionId`) | Todos | No |
| COCINERO | Todos los pedidos de todas las mesas | `EN_ESPERA` y `EN_PROGRESO` | Sí |
| MESERO | Solo pedidos de sus mesas asignadas | `LISTO` | Sí |

> Los resultados se ordenan por `pedidos.creado_en DESC` en todos los casos.

Cada ítem en la respuesta incluye `ingredientes` (lista completa del plato con `id`, `nombre` y `obligatorio`) e `ingredientesExcluidos` (nombres de los que el cliente pidió quitar). `mesa` y `mesero` solo aparecen para COCINERO y MESERO — `@JsonInclude(NON_NULL)` los omite para CLIENTE.

### Diseño de `clienteSessionId`

Al hacer login con `/auth/cliente/{mesaId}`, el JWT incluye un `clienteId` (UUID generado en ese momento). Este UUID se guarda en cada pedido como `clienteSessionId`, permitiendo identificar de forma única qué cliente creó qué pedido dentro de una misma mesa.

### Validaciones

- `ingredientesExcluidos` solo puede contener IDs con `obligatorio: false` en `PlatoIngrediente` → si no, `400`
- `DELETE`: solo se pueden eliminar ítems en estado `EN_ESPERA` → si está en otro estado, `400`
- `DELETE`: el `clienteSessionId` del JWT debe coincidir con el del pedido → si no, `403`
- `PATCH /estado`: la secuencia es `EN_ESPERA → EN_PROGRESO → LISTO → ENTREGADO`. No se puede retroceder ni saltar. Si ya está en `ENTREGADO` → `400`

---

## Paso 5 — Transición de estados ✅

Implementado dentro de `PedidoService` y `PedidoController`. No requirió archivos adicionales.

### Endpoint implementado

| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| `PATCH` | `/pedido/item/{itemId}/estado` | TODOS | Avanza el estado del ítem al siguiente en la secuencia. Sin body. |

### Lógica de transición

```
EN_ESPERA → EN_PROGRESO → LISTO → ENTREGADO
```

- No recibe estado en el body — siempre avanza al siguiente
- Si el ítem ya está en `ENTREGADO` → `400`
- La secuencia está definida como constante en `PedidoService.SECUENCIA_ESTADOS`

---

## Resumen de orden de implementación

| Paso | Qué se construye | Endpoints habilitados |
|------|------------------|-----------------------|
| 1 | BD en Render + perfiles + variables de entorno | — |
| 2 | Auth + JWT + Security + Usuario + Mesa | `POST /auth/cliente/{mesaId}`, `POST /auth/login` |
| 3 | Ingrediente + Caracteristica + Plato + PlatoIngrediente | `GET /platos`, `GET /platos/{id}`, `GET /platos/categoria/{cat}` |
| 4 | Pedido + ItemPedido + EstadoItem | `POST /pedido`, `GET /pedido`, `PATCH /pedido/item/{itemId}/estado`, `DELETE /pedido/{pedidoId}/item/{itemId}` |
| 5 | Transición de estados | Incluido en el Paso 4 — `PATCH /pedido/item/{itemId}/estado` |
