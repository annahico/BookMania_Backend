# BookMania — Backend

A full stack Library Management System built with **Spring Boot** and **React**.

---

## Description

BookMania is a RESTful API for managing a digital library. It handles books, users, loans, fines, and reservations through clear business rules and a secure JWT-based authentication system.

---

## Tech Stack

**Backend**
- Java 21
- Spring Boot 3.2
- Spring Security + JWT (JJWT 0.12.3)
- Spring Data JPA (Hibernate)
- PostgreSQL
- Maven

**Frontend** *(in progress)*
- React
- REST API integration

---

## Features

### Core Modules

- **Authentication** — Register, login and JWT with `ADMIN` and `USER` roles
- **Catalogue** — Full CRUD for books and categories including cover images
- **Loans** — Issue, extend and return books with strict business rules
- **Fines** — Automatic time-based penalty for overdue returns
- **Reservations** — Waiting queue with a maximum of 3 people per book

### Business Rules

- Loan duration: **21 days**
- Maximum **3 extensions** of 10 days each, calculated from the current due date
- Overdue penalty: **7 base days + 2 days per each day overdue**
- Penalties accumulate if multiple fines are active
- Reservation queue: maximum **3 people** per book
- Users with an active penalty cannot make reservations or borrow books
- When a book is returned, the first person in the queue is automatically notified

---

## API Endpoints

### Authentication
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/auth/register` | Public | Register a new user |
| POST | `/api/auth/login` | Public | Login — returns JWT token |

### Books
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/books` | Public | List books with filters |
| GET | `/api/books/{id}` | Public | Get book details |
| POST | `/api/books` | ADMIN | Create a book |
| PUT | `/api/books/{id}` | ADMIN | Update a book |
| DELETE | `/api/books/{id}` | ADMIN | Delete a book |

### Categories
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/categories` | Public | List categories |
| POST | `/api/categories` | ADMIN | Create a category |

### Loans
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/loans` | USER | Request a loan |
| GET | `/api/loans/my` | USER | My loan history |
| PUT | `/api/loans/{id}/extend` | USER | Extend a loan |
| PUT | `/api/loans/{id}/return` | USER | Return a book |

### Fines
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/fines/my` | USER | My penalties |

### Reservations
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/reservations` | USER | Create a reservation |
| DELETE | `/api/reservations/{id}` | USER | Cancel a reservation |
| GET | `/api/reservations/my` | USER | My reservation history |

---

## Project Structure

```
src/main/java/com/bookmania/bookmania/
├── Configuration/       # SecurityConfig, JwtProperties
├── Controller/          # REST controllers
├── Dtos/                # Request and Response DTOs
├── Entity/              # JPA entities
├── Enums/               # LoanStatus, ReservationStatus, Role
├── Exception/           # GlobalExceptionHandler + custom exceptions
├── Repository/          # Spring Data JPA repositories
├── Scheduler/           # LoanScheduler — marks loans as OVERDUE daily
├── Security/            # JwtUtil, JwtAuthFilter, UserDetailsServiceImpl
└── Services/            # Business logic
```

---

## Local Setup

### Prerequisites

- Java 21+
- PostgreSQL
- Maven
- Node.js and npm (for the frontend)

### Steps

1. Clone the repository

```bash
git clone https://github.com/annahico/BookMania_Backend.git
cd BookMania_Backend
```

2. Create the PostgreSQL database

```sql
CREATE DATABASE bookmania_db;
```

3. Configure your environment variables or edit `application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bookmania_db
spring.datasource.username=postgres
spring.datasource.password=your_password
jwt.secretKey=your_secret_key
```

4. Run the application

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

---

## Frontend Integration

> The React frontend is currently under development and will be integrated soon.

To connect the frontend to this backend, set the base API URL and include the JWT token in every authenticated request:

```javascript
const API_BASE_URL = "http://localhost:8080";

headers: {
  "Authorization": `Bearer ${token}`,
  "Content-Type": "application/json"
}
```

---

## Author

**Anna Costa**
[LinkedIn](https://www.linkedin.com/in/annahico/)
[GitHub](https://github.com/annahico)

---
---

# BookMania — Backend

Sistema de Gestión de Bibliotecas full stack desarrollado con **Spring Boot** y **React**.

---

## Descripción

BookMania es una API RESTful para la gestión de una biblioteca digital. Permite gestionar libros, usuarios, préstamos, multas y reservas mediante reglas de negocio claras y un sistema de autenticación seguro basado en JWT.

---

## Stack tecnológico

**Backend**
- Java 21
- Spring Boot 3.2
- Spring Security + JWT (JJWT 0.12.3)
- Spring Data JPA (Hibernate)
- PostgreSQL
- Maven

**Frontend** *(en desarrollo)*
- React
- Integración con API REST

---

## Características

### Módulos principales

- **Autenticación** — Registro, login y JWT con roles `ADMIN` y `USER`
- **Catálogo** — CRUD completo de libros y categorías con imágenes de portada
- **Préstamos** — Emisión, prórroga y devolución con reglas de negocio estrictas
- **Multas** — Penalización temporal automática por retrasos
- **Reservas** — Cola de espera con máximo 3 personas por libro

### Reglas de negocio

- Duración del préstamo: **21 días**
- Máximo **3 prórrogas** de 10 días cada una calculadas desde la fecha de vencimiento
- Penalización por retraso: **7 días base + 2 días por cada día de retraso**
- Las penalizaciones se acumulan si hay varias multas activas
- Cola de reservas con máximo **3 personas** por libro
- Los usuarios con penalización activa no pueden hacer reservas ni préstamos
- Al devolver un libro, se notifica automáticamente al primero de la cola

---

## Endpoints de la API

### Autenticación
| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| POST | `/api/auth/register` | Público | Registro de usuario |
| POST | `/api/auth/login` | Público | Login — devuelve JWT |

### Libros
| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/api/books` | Público | Listar libros con filtros |
| GET | `/api/books/{id}` | Público | Detalle de un libro |
| POST | `/api/books` | ADMIN | Crear libro |
| PUT | `/api/books/{id}` | ADMIN | Actualizar libro |
| DELETE | `/api/books/{id}` | ADMIN | Eliminar libro |

### Categorías
| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/api/categories` | Público | Listar categorías |
| POST | `/api/categories` | ADMIN | Crear categoría |

### Préstamos
| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| POST | `/api/loans` | USER | Solicitar préstamo |
| GET | `/api/loans/my` | USER | Mis préstamos |
| PUT | `/api/loans/{id}/extend` | USER | Prorrogar préstamo |
| PUT | `/api/loans/{id}/return` | USER | Devolver libro |

### Multas
| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/api/fines/my` | USER | Mis penalizaciones |

### Reservas
| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| POST | `/api/reservations` | USER | Crear reserva |
| DELETE | `/api/reservations/{id}` | USER | Cancelar reserva |
| GET | `/api/reservations/my` | USER | Mis reservas |

---

## Arquitectura

```
src/main/java/com/bookmania/bookmania/
├── Configuration/       # SecurityConfig, JwtProperties
├── Controller/          # Controladores REST
├── Dtos/                # Request y Response DTOs
├── Entity/              # Entidades JPA
├── Enums/               # LoanStatus, ReservationStatus, Role
├── Exception/           # GlobalExceptionHandler + excepciones custom
├── Repository/          # Repositorios Spring Data JPA
├── Scheduler/           # LoanScheduler — marca préstamos OVERDUE diariamente
├── Security/            # JwtUtil, JwtAuthFilter, UserDetailsServiceImpl
└── Services/            # Lógica de negocio
```

---

## Instalación local

### Prerrequisitos

- Java 21+
- PostgreSQL
- Maven
- Node.js y npm (para el frontend)

### Pasos

1. Clona el repositorio

```bash
git clone https://github.com/annahico/BookMania_Backend.git
cd BookMania_Backend
```

2. Crea la base de datos en PostgreSQL

```sql
CREATE DATABASE bookmania_db;
```

3. Configura las variables de entorno o edita `application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bookmania_db
spring.datasource.username=postgres
spring.datasource.password=tu_password
jwt.secretKey=tu_secret_key
```

4. Arranca la aplicación

```bash
mvn spring-boot:run
```

La API estará disponible en `http://localhost:8080`

---

## Integración con el Frontend

> El frontend en React está actualmente en desarrollo y se integrará próximamente.

Para conectar el frontend a este backend, configura la URL base de la API e incluye el token JWT en cada petición autenticada:

```javascript
const API_BASE_URL = "http://localhost:8080";

headers: {
  "Authorization": `Bearer ${token}`,
  "Content-Type": "application/json"
}
```

---

## Autor

**Anna Costa**
[LinkedIn](https://www.linkedin.com/in/annahico/)
[GitHub](https://github.com/annahico)