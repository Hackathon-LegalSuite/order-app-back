# Restaurant Order App — API Endpoints

Base URL: `https://<tu-app>.onrender.com`

> **Alcance del proyecto (Hackathon)**
> - Flujo completo del cliente/consumidor
> - Vista del cocinero — actualización de estado de platos
> - Pantalla de despacho para meseros
> - Búsqueda inteligente del menú con IA (Groq / Llama)

---

## Autenticación

### Cliente (acceso por QR)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/auth/cliente/{mesaId}` | Login por QR. Valida código de mesa. Retorna JWT de 6h |

**Body:**
```json
{ "nombre": "Juan", "codigoMesa": "MESA-05" }
```

**Response:**
```json
{ "token": "eyJhbGci...", "rol": "CLIENTE", "nombre": "Juan", "mesaId": 5, "expiresIn": "6h" }
```

El JWT del cliente incluye `mesaId` y `clienteId` (UUID único de sesión) como claims.

| Código | Motivo |
|--------|--------|
| `404` | Mesa no encontrada |
| `400` | Código no corresponde a esa mesa |

---

### Staff — Cocinero / Mesero

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/auth/login` | Login con usuario y contraseña. Retorna JWT |

**Body:**
```json
{ "username": "cocinero01", "password": "1234" }
```

**Response:**
```json
{ "token": "eyJhbGci...", "rol": "COCINERO", "nombre": "Carlos" }
```

> Todos los endpoints siguientes requieren `Authorization: Bearer <token>` en el header.

---

## Menú — Platos

| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| `GET` | `/platos` | CLIENTE | Listar todos los platos con sus ingredientes |
| `GET` | `/platos/{id}` | CLIENTE | Detalle de un plato por ID |
| `GET` | `/platos/categoria/{categoria}` | CLIENTE | Filtrar por categoría |

Categorías válidas: `ENTRADA`, `BEBIDA`, `PLATO_FUERTE`, `POSTRE`

**Response `GET /platos`:**
```json
[
  {
    "id": 1,
    "nombre": "Hamburguesa Clásica",
    "descripcion": "Carne de res, lechuga, tomate y papas fritas",
    "precio": 18500,
    "categoria": "PLATO_FUERTE",
    "imagenUrl": "https://...",
    "ingredientes": [
      { "id": 1, "nombre": "Carne de res", "obligatorio": true },
      { "id": 2, "nombre": "Lechuga",      "obligatorio": false }
    ]
  }
]
```

`preparada` solo aparece en bebidas (`true` = elaborada en cocina, `false` = embotellada). `imagenUrl` se omite si no está asignada.

---

## Búsqueda inteligente con IA

| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| `POST` | `/menu/buscar` | CLIENTE | Interpreta lenguaje natural y filtra el menú |

**Body:**
```json
{ "prompt": "Quiero algo con carne que no sea muy caro" }
```

**Response:**
```json
{
  "mensaje": "Encontré estos platos con carne para vos.",
  "ingredientesExcluir": [
    { "id": 4, "nombre": "Cebolla" }
  ],
  "platos": [ ... ]
}
```

El LLM (Groq / Llama 3.1) actúa como **parser de intención**: extrae `busqueda`, `categoria`, `caracteristicas`, `precioMaximo` e `ingredientesExcluir` del texto libre. El filtrado real lo hace la aplicación sobre la BD. `ingredientesExcluir` devuelve los IDs listos para usar en `POST /pedido`. Cada llamada es independiente — sin historial de sesión.

| Código | Motivo |
|--------|--------|
| `400` | `prompt` vacío |
| `503` | Groq no respondió o devolvió formato inesperado |

---

## Pedidos

| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| `POST` | `/pedido` | CLIENTE | Crear nuevo pedido |
| `GET`  | `/pedido` | TODOS  | Ver ítems (respuesta varía según rol) |
| `PATCH` | `/pedido/item/{itemId}/estado` | TODOS | Avanzar estado del ítem |
| `DELETE` | `/pedido/{pedidoId}/item/{itemId}` | CLIENTE | Eliminar ítem del pedido |

### POST /pedido

Mesa y nombre del cliente se extraen del JWT — el body solo lleva los ítems.

**Body:**
```json
{
  "items": [
    { "platoId": 1, "ingredientesExcluidos": [2, 3] },
    { "platoId": 4, "ingredientesExcluidos": [] }
  ]
}
```

> `ingredientesExcluidos` solo acepta IDs con `obligatorio: false` → si no, `400`.

### GET /pedido — respuesta según rol

| Rol | Ítems que ve | Filtro de estado | Campos `mesa` y `mesero` |
|-----|-------------|-----------------|--------------------------|
| CLIENTE | Solo los de su sesión (`clienteSessionId`) | Todos | No aparecen |
| COCINERO | Todos, todas las mesas | `EN_ESPERA` y `EN_PROGRESO` | Sí |
| MESERO | Solo sus mesas asignadas | `LISTO` | Sí |

**Response:**
```json
[
  {
    "pedidoId": 1,
    "itemId": 3,
    "platoId": 1,
    "platoNombre": "Hamburguesa Clásica",
    "precio": 18500,
    "imagenUrl": "https://...",
    "ingredientes": [
      { "id": 1, "nombre": "Carne de res", "obligatorio": true },
      { "id": 2, "nombre": "Lechuga",      "obligatorio": false }
    ],
    "ingredientesExcluidos": ["Lechuga"],
    "estado": "EN_ESPERA",
    "mesa": 5,
    "mesero": "Álvaro"
  }
]
```

`mesa` y `mesero` usan `@JsonInclude(NON_NULL)` — no aparecen para CLIENTE.

### PATCH /pedido/item/{itemId}/estado

Sin body. Avanza automáticamente al siguiente estado:

```
EN_ESPERA → EN_PROGRESO → LISTO → ENTREGADO
```

No se puede retroceder. Si ya está en `ENTREGADO` → `400`.

### DELETE /pedido/{pedidoId}/item/{itemId}

Solo el cliente que creó el pedido puede eliminarlo (`clienteSessionId` del JWT). Solo ítems en `EN_ESPERA` → si ya empezó a cocinarse, `400`. Si era el último ítem del pedido, el pedido se elimina también.

---

## Ciclo de vida de un ítem

```
[CLIENTE crea pedido]
        ↓
    EN_ESPERA          → visible para COCINERO
        ↓  (COCINERO avanza)
   EN_PROGRESO         → visible para COCINERO
        ↓  (COCINERO avanza)
      LISTO            → visible para MESERO | sigue en vista del CLIENTE
        ↓  (MESERO avanza)
    ENTREGADO          → solo visible para CLIENTE
```

---

## Mapa de endpoints por rol

| Endpoint | CLIENTE | COCINERO | MESERO |
|----------|:-------:|:--------:|:------:|
| `POST /auth/cliente/{mesaId}` | ✅ | — | — |
| `POST /auth/login` | — | ✅ | ✅ |
| `GET /platos` | ✅ | — | — |
| `GET /platos/{id}` | ✅ | — | — |
| `GET /platos/categoria/{cat}` | ✅ | — | — |
| `POST /menu/buscar` | ✅ | — | — |
| `POST /pedido` | ✅ | — | — |
| `GET /pedido` | ✅ | ✅ | ✅ |
| `PATCH /pedido/item/{itemId}/estado` | — | ✅ | ✅ |
| `DELETE /pedido/{pedidoId}/item/{itemId}` | ✅ | — | — |

---

## Estructura de paquetes

```
auth/
├── AuthController.java         → /auth/cliente/{mesaId}, /auth/login
├── AuthService.java
├── JwtUtil.java
├── JwtFilter.java              → valida token en cada request; almacena Claims como auth.details
├── AuthEntryPoint.java         → respuesta 401 estructurada
├── SecurityConfig.java
└── dto/

usuario/
├── Usuario.java                → @Entity — implementa UserDetails
├── UsuarioRepository.java
└── Rol.java                    → @Enum: COCINERO, MESERO

mesa/
├── Mesa.java                   → @Entity — numero, codigoQr, mesero (FK Usuario)
└── MesaRepository.java

ingrediente/
├── Ingrediente.java            → @Entity — nombre, caracteristicas (@ManyToMany con Caracteristica)
├── IngredienteRepository.java
├── Caracteristica.java         → @Entity (no enum) — nombre cargado desde BD
└── CaracteristicaRepository.java

plato/
├── Plato.java                  → @Entity — nombre, descripcion, precio, categoria, preparada, imagenUrl
├── PlatoIngrediente.java       → @Entity — tabla intermedia plato↔ingrediente con flag obligatorio
├── PlatoController.java        → /platos, /platos/{id}, /platos/categoria/{cat}
├── PlatoService.java
├── PlatoRepository.java
├── Categoria.java              → @Enum: ENTRADA, BEBIDA, PLATO_FUERTE, POSTRE
└── dto/

item/
├── ItemPedido.java             → @Entity — plato, ingredientesExcluidos (List<Long>), estado
├── ItemRepository.java
└── EstadoItem.java             → @Enum: EN_ESPERA, EN_PROGRESO, LISTO, ENTREGADO

pedido/
├── Pedido.java                 → @Entity — mesa, clienteNombre, clienteSessionId, items (orphanRemoval=true)
├── PedidoController.java       → /pedido (todos los verbos)
├── PedidoService.java
├── PedidoRepository.java
└── dto/

busqueda/
├── GroqClient.java             → HTTP client para Groq (OpenAI-compatible)
├── BusquedaService.java        → construye prompt, llama a Groq, filtra platos en memoria
├── BusquedaController.java     → /menu/buscar
└── dto/
    ├── BusquedaRequest.java
    ├── LlmParseResult.java     → JSON que devuelve el LLM
    ├── BusquedaResponse.java
    └── IngredienteExcluirInfo.java

config/
└── AppConfig.java              → @Bean RestTemplate, @Bean ObjectMapper

exception/
└── MensajeResponse.java        → record { String mensaje } para respuestas de error simples
```

---

## Validaciones clave

| Regla | Detalle |
|-------|---------|
| Ingredientes obligatorios | `ingredientesExcluidos` con `obligatorio: true` → `400` |
| Transición de estados | Solo hacia adelante: `EN_ESPERA → EN_PROGRESO → LISTO → ENTREGADO` → retroceder es `400` |
| Eliminación de ítem | Solo en estado `EN_ESPERA` y por el cliente que lo creó (`clienteSessionId`) |
| `clienteSessionId` | UUID generado al login, guardado en JWT y en cada `Pedido` — identifica la sesión del cliente |
| JWT claims | `JwtFilter` almacena el `Claims` completo en `auth.details` — los servicios extraen `mesaId`, `clienteId`, `rol` sin re-parsear el token |

---

## Nota de escalabilidad

La implementación actual carga entidades completas con `findAll()` y aplica los filtros en memoria con Java Streams. Para el volumen del hackathon es perfectamente válido.

En producción con menús grandes, los filtros deberían moverse a queries en el repositorio (`@Query` con `JOIN` y `WHERE`) para que la base de datos haga el trabajo en lugar de la JVM. La arquitectura en capas actual lo facilita — es una refactorización localizada en el repositorio, no un rediseño.

---

## Criterios Hackathon — Estado de cumplimiento

| Criterio | Estado | Detalle |
|----------|--------|---------|
| Estructura por capas | ✅ | Controller → Service → Repository → Entity en cada módulo |
| Endpoints RESTful | ✅ | Auth, Platos, Pedidos, Búsqueda IA implementados |
| DTOs y validaciones | ✅ | DTOs separados, `@NotBlank`, `@JsonInclude`, validación de ingredientes obligatorios |
| Manejo de excepciones | ✅ | `ResponseStatusException` con mensajes estructurados en todos los casos de error |
| Persistencia con JPA | ✅ | Todas las entidades operativas, relaciones correctas, `orphanRemoval=true` en Pedido |
| Configuración limpia | ✅ | Perfiles dev/prod, variables de entorno, sin credenciales en el repo |
| Normalización BD | ✅ | Relaciones normalizadas, tablas intermedias para ManyToMany |
| Componente con IA | ✅ | `POST /menu/buscar` — Groq / Llama 3.1, búsqueda en lenguaje natural |
