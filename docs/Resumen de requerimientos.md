# 🍽️ Restaurant Order App — API Endpoints

Base URL: `https://<tu-app>.onrender.com/api`

> **Alcance del proyecto (Hackathon)**
> - ✅ Flujo completo del cliente/consumidor (prioridad)
> - ✅ Flujo del cocinero — actualización de estado de platos por pedido
> - ✅ Pantalla de despacho para meseros — platos listos para entregar
> - ✅ Confirmación de entrega por parte del mesero

---

## 🔐 Autenticación

### Cliente (acceso por QR)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/auth/cliente` | Inicio de sesión con nombre y código de mesa. Retorna JWT de 6h |

**Body:**
```json
{
  "nombre": "Juan",
  "codigoMesa": "MESA-05"
}
```

**Response:**
```json
{
  "token": "eyJhbGci...",
  "mesaId": 5,
  "clienteNombre": "Juan",
  "expiresIn": "6h"
}
```

---

### Staff — Cocinero / Mesero

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/auth/login` | Login con usuario y contraseña. Retorna JWT |

**Body:**
```json
{
  "username": "cocinero01",
  "password": "1234"
}
```

**Response:**
```json
{
  "token": "eyJhbGci...",
  "rol": "COCINERO",
  "nombre": "Carlos"
}
```

> Roles disponibles en el sistema: `COCINERO`, `MESERO`
> 🔑 Todos los endpoints siguientes requieren `Authorization: Bearer <token>` en el header.

---

## 🍔 Menú — Platos (lectura pública para el cliente)

| Método | Endpoint | Descripción | Rol |
|--------|----------|-------------|-----|
| `GET` | `/platos` | Listar todos los platos del menú | CLIENTE |
| `GET` | `/platos/{id}` | Obtener detalle de un plato con sus ingredientes | CLIENTE |
| `GET` | `/platos/categoria/{categoria}` | Filtrar platos por categoría | CLIENTE |

**Response `GET /platos`:**
```json
[
  {
    "id": 1,
    "nombre": "Hamburguesa Clásica",
    "descripcion": "Carne de res, lechuga, tomate y papas fritas",
    "precio": 18500,
    "categoria": "PLATO_FUERTE",
    "ingredientes": [
      { "id": 1, "nombre": "Carne de res", "obligatorio": true },
      { "id": 2, "nombre": "Lechuga",      "obligatorio": false },
      { "id": 3, "nombre": "Tomate",       "obligatorio": false },
      { "id": 4, "nombre": "Cebolla",      "obligatorio": false }
    ]
  },
  {
    "id": 7,
    "nombre": "Limonada de Coco",
    "descripcion": "Limonada natural con coco",
    "precio": 8000,
    "categoria": "BEBIDA",
    "preparada": true,
    "ingredientes": [
      { "id": 9, "nombre": "Limón",  "obligatorio": true },
      { "id": 10, "nombre": "Coco", "obligatorio": true }
    ]
  }
]
```

> Categorías: `ENTRADA`, `BEBIDA`, `PLATO_FUERTE`, `POSTRE`
> `preparada: true` = elaborada en cocina | `preparada: false` = embotellada

---

## 📋 Pedidos — Flujo del Cliente

| Método | Endpoint | Descripción | Rol |
|--------|----------|-------------|-----|
| `POST` | `/pedidos` | Crear un nuevo pedido desde la mesa | CLIENTE |
| `GET`  | `/pedidos/mesa/{mesaId}` | Ver los pedidos activos de la mesa del cliente | CLIENTE |

**Body `POST /pedidos`:**
```json
{
  "mesaId": 5,
  "clienteNombre": "Juan",
  "items": [
    {
      "platoId": 1,
      "ingredientesExcluidos": [2, 3]
    },
    {
      "platoId": 7,
      "ingredientesExcluidos": []
    }
  ]
}
```

> ⚠️ `ingredientesExcluidos` solo acepta IDs de ingredientes con `obligatorio: false`.
> El backend debe validar esto y retornar `400 Bad Request` si se intenta excluir un ingrediente obligatorio.

**Response `POST /pedidos`:**
```json
{
  "pedidoId": 42,
  "mesaId": 5,
  "estado": "EN_ESPERA",
  "items": [
    {
      "platoId": 1,
      "nombre": "Hamburguesa Clásica",
      "ingredientesExcluidos": ["Lechuga", "Tomate"],
      "estado": "EN_ESPERA"
    },
    {
      "platoId": 7,
      "nombre": "Limonada de Coco",
      "ingredientesExcluidos": [],
      "estado": "EN_ESPERA"
    }
  ],
  "creadoEn": "2025-05-21T14:30:00Z"
}
```

**Response `GET /pedidos/mesa/{mesaId}`:**
```json
[
  {
    "pedidoId": 42,
    "items": [
      {
        "nombre": "Hamburguesa Clásica",
        "ingredientesExcluidos": ["Lechuga", "Tomate"],
        "estado": "EN_PROGRESO"
      },
      {
        "nombre": "Limonada de Coco",
        "ingredientesExcluidos": [],
        "estado": "LISTO"
      }
    ]
  }
]
```

---

## 👨‍🍳 Cocina — Flujo del Cocinero

> El cocinero ve todos los platos **pendientes de preparación** (EN_ESPERA o EN_PROGRESO) en una sola pantalla. Desde ahí puede actualizar el estado de cada plato individualmente hasta marcarlo como `LISTO`.
> Una vez marcado `LISTO`, el plato desaparece de la vista de cocina y pasa a la pantalla de despacho del mesero.

| Método | Endpoint | Descripción | Rol |
|--------|----------|-------------|-----|
| `GET`  | `/cocina/platos` | Listar todos los platos en preparación (EN_ESPERA, EN_PROGRESO) | COCINERO |
| `PATCH`| `/cocina/items/{itemId}/estado` | Actualizar el estado de un plato (hasta LISTO) | COCINERO |

**Response `GET /cocina/platos`:**
```json
[
  {
    "itemId": 101,
    "pedidoId": 42,
    "mesa": 5,
    "clienteNombre": "Juan",
    "plato": "Hamburguesa Clásica",
    "ingredientesExcluidos": ["Lechuga", "Tomate"],
    "estado": "EN_ESPERA"
  },
  {
    "itemId": 102,
    "pedidoId": 42,
    "mesa": 5,
    "clienteNombre": "Juan",
    "plato": "Limonada de Coco",
    "ingredientesExcluidos": [],
    "estado": "EN_PROGRESO"
  },
  {
    "itemId": 110,
    "pedidoId": 45,
    "mesa": 1,
    "clienteNombre": "Álvaro",
    "plato": "Bandeja Paisa",
    "ingredientesExcluidos": [],
    "estado": "EN_ESPERA"
  }
]
```

**Body `PATCH /cocina/items/{itemId}/estado`:**
```json
{
  "estado": "EN_PROGRESO"
}
```

> Estados válidos para el cocinero: `EN_ESPERA → EN_PROGRESO → LISTO`
> El backend valida que no se pueda retroceder un estado.
> Una vez en `LISTO`, el ítem desaparece de `/cocina/platos` y aparece en `/despacho/platos`.

---

## 🖥️ Pantalla de Despacho — Flujo del Mesero

> Vista de platos **listos para entregar**. Muestra únicamente los ítems con estado `LISTO`. Cuando el mesero entrega el plato, lo marca como `ENTREGADO` y desaparece de la pantalla.

| Método | Endpoint | Descripción | Rol |
|--------|----------|-------------|-----|
| `GET`  | `/despacho/platos` | Listar todos los platos con estado LISTO (pendientes de entrega) | MESERO, COCINERO |
| `PATCH`| `/despacho/items/{itemId}/estado` | Marcar un plato como ENTREGADO | MESERO |

**Response `GET /despacho/platos`:**
```json
[
  {
    "itemId": 101,
    "pedidoId": 42,
    "mesa": 5,
    "clienteNombre": "Juan",
    "meseroEncargado": "Álvaro",
    "plato": "Hamburguesa Clásica",
    "ingredientesExcluidos": ["Lechuga", "Tomate"],
    "estado": "LISTO"
  },
  {
    "itemId": 102,
    "pedidoId": 42,
    "mesa": 5,
    "clienteNombre": "Juan",
    "meseroEncargado": "Álvaro",
    "plato": "Limonada de Coco",
    "ingredientesExcluidos": [],
    "estado": "LISTO"
  }
]
```

> Solo aparecen ítems con `estado: "LISTO"`. Los ítems `ENTREGADO` ya no se muestran.

**Body `PATCH /despacho/items/{itemId}/estado`:**
```json
{
  "estado": "ENTREGADO"
}
```

> Esta transición solo la puede realizar un usuario con rol `MESERO`.
> Solo es válida desde `LISTO → ENTREGADO`. Cualquier otra transición retorna `400 Bad Request`.

---

## ⚠️ Validaciones clave del backend

| Regla | Detalle |
|-------|---------|
| Ingredientes obligatorios | Si `ingredientesExcluidos` contiene un `ingredienteId` con `obligatorio: true` → `400 Bad Request` |
| Transición de estados | Solo se permite avanzar: `EN_ESPERA → EN_PROGRESO → LISTO → ENTREGADO`. Retroceder → `400 Bad Request` |
| Transición por rol | `EN_ESPERA → LISTO`: solo `COCINERO`. `LISTO → ENTREGADO`: solo `MESERO` |
| JWT Cliente | Expira en 6 horas. Asociado a la mesa, no a un usuario registrado |
| JWT Staff | Acceso por rol: el `COCINERO` no puede operar en despacho; el `MESERO` no puede operar en cocina |
| Pedido por mesa | Una mesa puede tener múltiples pedidos activos (el cliente puede pedir en rondas) |

---

## 📁 Estructura de paquetes (Spring Boot)

### `auth/`
> Maneja autenticación y generación de tokens JWT para clientes y staff.

```
auth/
├── AuthController.java       → /auth/cliente, /auth/login
├── AuthService.java
└── JwtUtil.java
```

**Entidades:** ninguna propia (usa `Usuario` y `Mesa`)
**Tablas BD:** ninguna propia

---

### `usuario/`
> Representa a los usuarios del sistema con rol (cocinero o mesero). Los datos se cargan directamente en BD, no hay registro por pantalla.

```
usuario/
├── Usuario.java              → @Entity
├── UsuarioRepository.java
└── Rol.java                  → @Enum: COCINERO, MESERO
```

**Entidad `Usuario`:**
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | `Long` PK | Identificador |
| `username` | `String` | Nombre de usuario para login |
| `password` | `String` | Contraseña encriptada (BCrypt) |
| `nombre` | `String` | Nombre visible |
| `rol` | `Enum` | `COCINERO` \| `MESERO` |

**Tabla BD:** `usuarios`

---

### `mesa/`
> Representa las mesas del restaurante. Cada mesa tiene un código único que va en el QR.

```
mesa/
├── Mesa.java                 → @Entity
└── MesaRepository.java
```

**Entidad `Mesa`:**
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | `Long` PK | Identificador |
| `numero` | `Integer` | Número visible de la mesa |
| `codigoQr` | `String` | Código único del QR (ej: `MESA-05`) |
| `mesero` | `Usuario` FK | Mesero asignado a esa mesa (rol `MESERO`) |

**Tabla BD:** `mesas`

---

### `ingrediente/`
> Ingredientes que pueden pertenecer a uno o varios platos. Cada uno tiene características y se marca si es obligatorio a nivel de plato.

```
ingrediente/
├── Ingrediente.java          → @Entity
├── IngredienteRepository.java
└── Caracteristica.java       → @Enum
```

**Entidad `Ingrediente`:**
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | `Long` PK | Identificador |
| `nombre` | `String` | Nombre del ingrediente |
| `caracteristicas` | `List<Enum>` | picante, salado, dulce, acido, amargo, alcohol, caliente, frio, vegetariano |

**Tabla BD:** `ingredientes`, `ingrediente_caracteristicas`

---

### `plato/`
> Platos del menú. Cada plato tiene una categoría y una lista de ingredientes con su carácter obligatorio u opcional.

```
plato/
├── Plato.java                → @Entity
├── PlatoIngrediente.java     → @Entity (tabla intermedia con obligatorio)
├── PlatoController.java      → /platos, /platos/{id}, /platos/categoria/{cat}
├── PlatoService.java
├── PlatoRepository.java
└── Categoria.java            → @Enum: ENTRADA, BEBIDA, PLATO_FUERTE, POSTRE
```

**Entidad `Plato`:**
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | `Long` PK | Identificador |
| `nombre` | `String` | Nombre del plato |
| `descripcion` | `String` | Descripción breve |
| `precio` | `BigDecimal` | Precio |
| `categoria` | `Enum` | `ENTRADA` \| `BEBIDA` \| `PLATO_FUERTE` \| `POSTRE` |
| `preparada` | `Boolean` | Solo para `BEBIDA`: true = preparada, false = embotellada |
| `ingredientes` | `List<PlatoIngrediente>` | Relación con ingredientes |

**Entidad `PlatoIngrediente`** *(tabla intermedia)*:
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | `Long` PK | Identificador |
| `plato` | `Plato` FK | Plato al que pertenece |
| `ingrediente` | `Ingrediente` FK | Ingrediente |
| `obligatorio` | `Boolean` | Si el ingrediente puede excluirse o no |

**Tablas BD:** `platos`, `plato_ingredientes`

---

### `pedido/`
> Pedido realizado por un cliente desde su mesa. Contiene los ítems (platos) solicitados.

```
pedido/
├── Pedido.java               → @Entity
├── PedidoController.java     → /pedidos, /pedidos/mesa/{mesaId}
├── PedidoService.java
└── PedidoRepository.java
```

**Entidad `Pedido`:**
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | `Long` PK | Identificador |
| `mesa` | `Mesa` FK | Mesa desde donde se realizó |
| `clienteNombre` | `String` | Nombre del cliente |
| `creadoEn` | `LocalDateTime` | Timestamp de creación |
| `items` | `List<ItemPedido>` | Platos del pedido |

**Tabla BD:** `pedidos`

---

### `item/`
> Ítem individual dentro de un pedido: un plato específico con sus ingredientes excluidos y su estado de preparación.

```
item/
├── ItemPedido.java           → @Entity
├── ItemService.java
├── ItemRepository.java
└── EstadoItem.java           → @Enum: EN_ESPERA, EN_PROGRESO, LISTO, ENTREGADO
```

> No tiene controller propio. Las actualizaciones de estado se exponen desde `cocina/` (COCINERO) y `despacho/` (MESERO).

**Entidad `ItemPedido`:**
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | `Long` PK | Identificador |
| `pedido` | `Pedido` FK | Pedido al que pertenece |
| `plato` | `Plato` FK | Plato solicitado |
| `estado` | `Enum` | `EN_ESPERA` \| `EN_PROGRESO` \| `LISTO` \| `ENTREGADO` |
| `ingredientesExcluidos` | `List<Ingrediente>` | Ingredientes opcionales eliminados por el cliente |

**Tablas BD:** `items_pedido`, `item_ingredientes_excluidos`

---

### `cocina/`
> Vista y control del cocinero. Muestra platos pendientes de preparación y permite avanzar su estado hasta LISTO.

```
cocina/
├── CocinaController.java     → /cocina/platos, /cocina/items/{itemId}/estado
└── CocinaService.java
```

**Entidades usadas:** `ItemPedido`, `Pedido`, `Mesa`, `Plato`
**Filtro activo:** solo ítems con estado `EN_ESPERA` o `EN_PROGRESO`
**Tablas BD:** ninguna propia

---

### `despacho/`
> Vista y control del mesero. Muestra solo los platos listos para entregar y permite confirmar la entrega.

```
despacho/
├── DespachoController.java   → /despacho/platos, /despacho/items/{itemId}/estado
└── DespachoService.java
```

**Entidades usadas:** `ItemPedido`, `Pedido`, `Mesa`, `Plato`
**Filtro activo:** solo ítems con estado `LISTO`
**Tablas BD:** ninguna propia

---

## 🔄 Ciclo de vida de un ítem

```
[CLIENTE crea pedido]
        ↓
    EN_ESPERA          → visible en /cocina/platos
        ↓  (COCINERO)
   EN_PROGRESO         → visible en /cocina/platos
        ↓  (COCINERO)
      LISTO            → visible en /despacho/platos
        ↓  (MESERO)
    ENTREGADO          → no aparece en ninguna pantalla
```

---

## 🗺️ Resumen de endpoints por rol

| Endpoint | CLIENTE | COCINERO | MESERO |
|----------|:-------:|:--------:|:------:|
| `POST /auth/cliente` | ✅ | — | — |
| `POST /auth/login` | — | ✅ | ✅ |
| `GET /platos` | ✅ | — | — |
| `GET /platos/{id}` | ✅ | — | — |
| `GET /platos/categoria/{cat}` | ✅ | — | — |
| `POST /pedidos` | ✅ | — | — |
| `GET /pedidos/mesa/{mesaId}` | ✅ | — | — |
| `GET /cocina/platos` | — | ✅ | — |
| `PATCH /cocina/items/{itemId}/estado` | — | ✅ | — |
| `GET /despacho/platos` | — | ✅ | ✅ |
| `PATCH /despacho/items/{itemId}/estado` | — | — | ✅ |

---

## 📋 Criterios mínimos Hackathon — Estado de cumplimiento

| Criterio | Estado | Detalle |
|----------|--------|---------|
| **B1 Estructura por capas** | ✅ Cumplido | Controller → Service → Repository → Entity en cada módulo |
| **B2 Endpoints RESTful** | ⚠️ En progreso | Auth implementado. Faltan platos, pedidos, cocina, despacho |
| **B3 DTOs y validaciones** | ✅ Cumplido | DTOs separados de entidades, `@NotBlank` en requests, `@JsonInclude` en responses |
| **B4 Manejo de excepciones** | ✅ Cumplido | `GlobalExceptionHandler` cubre 401, 404 y 500 con estructura uniforme |
| **B5 Persistencia con JPA** | ⚠️ En progreso | `Usuario` y `Mesa` operativos. Faltan entidades de pasos 3 y 4 |
| **B6 Configuración limpia** | ✅ Cumplido | Perfiles dev/prod separados, env vars, sin credenciales en el repo |
| **B7 Normalización BD + modelo ER** | ⚠️ En progreso | Relación `Mesa → Usuario` normalizada. Modelo completo al finalizar todos los pasos |
