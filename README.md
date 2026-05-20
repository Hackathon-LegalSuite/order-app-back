# Order App Back

Backend para una plataforma de gestión de pedidos en restaurantes desarrollado con Spring Boot.

---

## Descripción

**Order App Back** es una aplicación backend diseñada para administrar el flujo de pedidos de un restaurante mediante APIs REST.

El proyecto está construido siguiendo una arquitectura modular y escalable, enfocada en mantener una separación clara de responsabilidades y facilitar futuras integraciones con nuevos servicios y funcionalidades.

Actualmente, la aplicación se enfoca en exponer APIs para la gestión del negocio principal del sistema de pedidos.

---

## Objetivos del Proyecto

- Gestionar pedidos de restaurante
- Exponer APIs REST organizadas y escalables
- Mantener una arquitectura limpia y modular
- Facilitar futuras integraciones y crecimiento del sistema
- Preparar la base para futuras funcionalidades avanzadas

---

## Características Principales

- Arquitectura REST
- Estructura modular por funcionalidades
- Endpoint de health check
- Arquitectura por capas
- Integración con JPA
- Configuración por ambientes
- Preparado para Docker y despliegue en la nube

---

## Stack Tecnológico

### Backend

| Tecnología       | Versión |
|------------------|---------|
| Java             | 21      |
| Spring Boot      | 3       |
| Spring Web       | —       |
| Spring Data JPA  | —       |
| Maven            | —       |

### Base de Datos

| Entorno      | Base de Datos               |
|--------------|-----------------------------|
| Desarrollo   | H2 Database                 |
| Producción   | PostgreSQL *(planeado)*     |

### Infraestructura

- Docker
- GitHub
- Render *(despliegue futuro)*

---

## Estructura del Proyecto

```
src/main/java/com/orderapp

├── health/
├── menu/
├── order/
├── user/
├── config/
├── shared/
└── exception/
```

Cada módulo administra sus propias capas internas:

- `controller`
- `service`
- `repository`
- `dto`
- `entity`

Esta estructura permite mejorar la mantenibilidad, escalabilidad y separación de responsabilidades.

---

## Ejecución del Proyecto

### Clonar repositorio

```bash
git clone <repository-url>
```

### Ejecutar aplicación

```bash
./mvnw spring-boot:run
```

La aplicación iniciará en:

```
http://localhost:8080
```

---

## Endpoint de Health

```http
GET /health
```

Ejemplo de respuesta:

```json
{
  "status": "UP",
  "message": "Order App Backend Running",
  "timestamp": "2026-05-20T15:30:00"
}
```

---

## Mejoras Futuras

- [ ] Integración con PostgreSQL
- [ ] Autenticación y autorización
- [ ] Dockerización completa
- [ ] Gestión de sesiones y caché con Redis
- [ ] Documentación de APIs con Swagger/OpenAPI
- [ ] Integración de motores de recomendación
- [ ] Flujo conversacional para pedidos

---

## Autor

Proyecto backend desarrollado con Spring Boot aplicando principios de arquitectura limpia y modular.
