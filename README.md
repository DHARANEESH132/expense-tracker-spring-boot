# 💎 Expense Tracker - Full-Stack Personal Finance & Analytics Web Application (Spring Boot 3 + Vanilla JS)

A complete, production-grade, interview-ready full-stack Expense Tracker web application built from scratch with a **Spring Boot 3 (Java 21) REST API** and a modern **Vanilla HTML5/CSS3/JavaScript frontend** with interactive **Chart.js** analytics.

---

## 📑 Table of Contents

1. [Features](#features)
2. [Tech Stack](#tech-stack)
3. [Project Architecture](#project-architecture)
4. [Folder Structure](#folder-structure)
5. [Database Design & Indexing](#database-design--indexing)
6. [Authentication & Authorization Flow](#authentication--authorization-flow)
7. [API Endpoints Catalog](#api-endpoints-catalog)
8. [Installation & Setup](#installation--setup)
9. [Configuration Properties](#configuration-properties)
10. [Running the Application](#running-the-application)
11. [Automated Testing Suite](#automated-testing-suite)
12. [Frontend Architecture & Centralized API Client](#frontend-architecture--centralized-api-client)
13. [Interview Talking Points](#interview-talking-points)

---

## ✨ Features

- **User Authentication**: Secure user registration, password hashing with Spring Security's `BCryptPasswordEncoder`, stateless JWT issuance via JJWT, session persistence, and clean logout.
- **Strict Multi-Tenant Isolation**: Zero cross-user data leakage. Every expense and budget query and mutation is strictly scoped to the authenticated user (`userPrincipal.getId()`). User B cannot view, update, or delete User A's resources even with knowledge of their resource IDs.
- **Comprehensive Expense Management**:
  - Full CRUD operations with detailed categories, amounts, payment methods, and notes.
  - Multi-condition filtering by Category, Payment Method, Date Range, and Min/Max amounts.
  - Case-insensitive search across descriptions and notes.
  - Multi-direction sorting (Newest, Oldest, Highest amount, Lowest amount).
  - Server-side pagination (`page`, `limit`, `totalPages`, `totalItems`).
- **Dynamic Budget System**:
  - Set overall monthly budgets or category-specific spending caps.
  - Real-time spending reconciliation computing `amount`, `spent`, `remaining`, and `percentageUsed`.
  - Color-coded progress indicators and status threshold warnings (`normal` <80%, `warning` 80-99.9%, `exceeded` >=100%).
- **Financial Analytics & Visualizations**:
  - Overall & monthly spending summary metrics (Total, Average, Highest, Lowest).
  - Spending by category breakdown with percentages and interactive Chart.js Doughnut chart.
  - Historical 6-month spending trends visualized via Chart.js Bar chart.
- **CSV Data Export**: RFC 4180-compliant CSV export streaming user expenses directly to the browser for spreadsheet analysis.
- **Account & Security Settings**:
  - View account metadata and registration date.
  - Update full name with instant UI reflection.
  - Secure password update requiring validation of the current password before applying new BCrypt hashes.
- **Interactive OpenAPI Documentation**: Built-in Swagger UI documentation mounted at `/swagger-ui.html` and OpenAPI specification at `/api-docs`.

---

## 🛠️ Tech Stack

### Frontend
- **HTML5**: Semantic markup, accessible forms, and dialog modals across all views.
- **CSS3**: Responsive modern SaaS design system, CSS custom properties, card grids, progress bars, and toast animations.
- **Vanilla JavaScript (ES6+)**: Modular code without heavy frameworks (No React, Angular, or Vue).
- **Fetch API**: Centralized API abstraction module (`api.js`) with JWT interceptors and global 401 redirection.
- **Chart.js**: Lightweight, responsive charting via CDN.

### Backend
- **Framework**: **Spring Boot 3.3.4** on **Java 21**
- **Security**: **Spring Security 6** with custom `JwtAuthenticationFilter` and `BCryptPasswordEncoder`.
- **Database & Persistence**: **Spring Data JPA (Hibernate 6)** with an embedded/file-based **H2 Database** (`jdbc:h2:file:./data/expensedb`) that runs 100% out-of-the-box with zero external database setup required.
- **Authentication**: **JJWT (Java JWT 0.12.6)** with HMAC-SHA256 tokens.
- **Validation**: **Jakarta Bean Validation** (`spring-boot-starter-validation`).
- **Documentation**: **Springdoc OpenAPI / Swagger UI** (`springdoc-openapi-starter-webmvc-ui 2.6.0`).
- **Testing**: **JUnit 5**, **MockMvc**, and **Spring Security Test**.

---

## 🏛️ Project Architecture

The backend follows an enterprise **Layered / Clean Architecture** pattern:

```
[ Browser / Vanilla JS Frontend ]
           │
           ▼ (HTTP/JSON + Authorization: Bearer <token>)
[ Security Filter Chain (CORS, CSRF disabled, Stateless Session) ]
           │
           ▼
[ JwtAuthenticationFilter ] ── (Validates JWT signature & populates SecurityContext)
           │
           ▼
[ RestControllers (Health, Auth, Expense, Budget) ]
           │
           ▼
[ Service Layer (AuthService, ExpenseService, BudgetService) ]
           │
           ▼
[ Spring Data JPA Repositories (UserRepository, ExpenseRepository, BudgetRepository) ]
           │
           ▼
[ H2 Persistent / In-Memory Database ]
```

---

## 📁 Folder Structure

```
expense-tracker/
│
├── backend/                              # Complete Spring Boot 3 Backend
│   ├── pom.xml                           # Maven dependencies & build configuration
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/expensetracker/
│   │   │   │   ├── ExpenseTrackerApplication.java
│   │   │   │   ├── config/
│   │   │   │   │   ├── SecurityConfig.java         # Spring Security 6, JWT filter, CORS
│   │   │   │   │   ├── WebMvcConfig.java           # Static resource mapping for frontend/ directory
│   │   │   │   │   └── OpenApiConfig.java          # Swagger UI / OpenAPI 3.0 configuration
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AuthController.java         # /api/auth endpoints
│   │   │   │   │   ├── ExpenseController.java      # /api/expenses CRUD, filters, analytics, export
│   │   │   │   │   ├── BudgetController.java       # /api/budgets CRUD & dynamic status
│   │   │   │   │   └── HealthController.java       # /api/health endpoint
│   │   │   │   ├── dto/
│   │   │   │   │   ├── ApiResponse.java            # Standard { success, message, data, pagination, errors }
│   │   │   │   │   ├── AuthDto.java                # Register, Login, Profile, Password request/response
│   │   │   │   │   ├── ExpenseDto.java             # Expense request/response with dual id & _id
│   │   │   │   │   ├── AnalyticsDto.java           # Summary, CategorySummary, MonthlySummary
│   │   │   │   │   └── BudgetDto.java              # Budget request/response with dynamic reconciliation
│   │   │   │   ├── entity/
│   │   │   │   │   ├── User.java                   # User entity with BCrypt password
│   │   │   │   │   ├── Expense.java                # Expense entity with composite indexes
│   │   │   │   │   └── Budget.java                 # Budget entity with period & category unique constraints
│   │   │   │   ├── exception/
│   │   │   │   │   ├── AppException.java           # Custom operational exception
│   │   │   │   │   └── GlobalExceptionHandler.java # @RestControllerAdvice for validation, auth, 404, 409
│   │   │   │   ├── repository/
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   ├── ExpenseRepository.java      # JPA Repository + specifications/aggregations
│   │   │   │   │   └── BudgetRepository.java
│   │   │   │   ├── security/
│   │   │   │   │   ├── JwtTokenProvider.java       # Token generator, signer & validator
│   │   │   │   │   ├── JwtAuthenticationFilter.java# Bearer token interceptor
│   │   │   │   │   ├── UserPrincipal.java          # UserDetails implementation
│   │   │   │   │   └── CustomUserDetailsService.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── AuthService.java            # Registration, login, profile, password hashing
│   │   │   │   │   ├── ExpenseService.java         # Filtering, pagination, analytics, CSV export
│   │   │   │   │   └── BudgetService.java          # Spending reconciliation and alert status calculation
│   │   │   │   └── util/
│   │   │   │       └── CsvExporter.java            # RFC 4180 CSV builder
│   │   │   └── resources/
│   │   │       └── application.properties          # Server port 5000, H2 database, JWT secret
│   │   └── test/
│   │       ├── java/com/expensetracker/
│   │       │   ├── AuthControllerTest.java         # Spring Boot MockMvc auth tests
│   │       │   ├── ExpenseControllerTest.java      # Expense CRUD & multi-tenant isolation tests
│   │       │   └── BudgetControllerTest.java       # Budget reconciliation tests
│   │       └── resources/
│   │           └── application.properties          # Test in-memory H2 configuration
│
├── frontend/                             # Vanilla JS Modern Frontend
│   ├── index.html                        # Public landing page with features & dynamic CTA
│   ├── login.html                        # Sign In page with validation
│   ├── register.html                     # Sign Up page with validation
│   ├── dashboard.html                    # Financial dashboard with KPI cards & Chart.js
│   ├── expenses.html                     # Expense management with filters, modals & CSV export
│   ├── budgets.html                      # Budget management with progress bars & status alerts
│   ├── profile.html                      # User profile, name update & password change
│   ├── css/
│   │   ├── style.css                     # Base theme, typography, modals, tables, toasts
│   │   ├── auth.css                      # Auth card styling and layout
│   │   └── dashboard.css                 # Stats cards, charts grid, filter bars, progress bars
│   └── js/
│       ├── api.js                        # Centralized Fetch client with JWT & 401 interceptor
│       ├── auth.js                       # Client auth state, route guards & session helpers
│       ├── dashboard.js                  # Dashboard KPI loader & Chart.js charts
│       ├── expenses.js                   # Expenses table, pagination, modals, CSV export
│       ├── budgets.js                    # Budget cards, progress calculation, modals
│       └── profile.js                    # Profile update & password modification
│
├── .gitignore
└── README.md
```

---

## 🗄️ Database Design & Schema

### 1. User Table (`users`)
- `id`: `BIGINT PRIMARY KEY AUTO_INCREMENT`
- `name`: `VARCHAR(50) NOT NULL`
- `email`: `VARCHAR(100) NOT NULL UNIQUE` (indexed)
- `password`: `VARCHAR(255) NOT NULL` (BCrypt hashed)
- `created_at`: `TIMESTAMP WITH TIME ZONE NOT NULL`
- `updated_at`: `TIMESTAMP WITH TIME ZONE NOT NULL`

### 2. Expense Table (`expenses`)
- `id`: `BIGINT PRIMARY KEY AUTO_INCREMENT`
- `user_id`: `BIGINT NOT NULL FOREIGN KEY -> users(id)`
- `amount`: `DOUBLE PRECISION NOT NULL`
- `category`: `VARCHAR(50) NOT NULL`
- `description`: `VARCHAR(200) NOT NULL`
- `payment_method`: `VARCHAR(50) NOT NULL`
- `date`: `DATE NOT NULL`
- `notes`: `VARCHAR(500)`
- `created_at`: `TIMESTAMP WITH TIME ZONE NOT NULL`
- `updated_at`: `TIMESTAMP WITH TIME ZONE NOT NULL`
- **Composite Indexes**:
  - `idx_expense_user_date` on `(user_id, date)`
  - `idx_expense_user_cat` on `(user_id, category)`

### 3. Budget Table (`budgets`)
- `id`: `BIGINT PRIMARY KEY AUTO_INCREMENT`
- `user_id`: `BIGINT NOT NULL FOREIGN KEY -> users(id)`
- `category`: `VARCHAR(50) NOT NULL` ("All" or specific category)
- `budget_month`: `INTEGER NOT NULL` (1-12)
- `budget_year`: `INTEGER NOT NULL`
- `amount`: `DOUBLE PRECISION NOT NULL`
- `created_at`: `TIMESTAMP WITH TIME ZONE NOT NULL`
- `updated_at`: `TIMESTAMP WITH TIME ZONE NOT NULL`
- **Composite Unique Constraint**:
  - `uk_user_cat_month_year` on `(user_id, category, budget_month, budget_year)`

---

## 📡 API Endpoints Catalog

| Method | Endpoint | Auth Required | Description |
|---|---|:---:|---|
| `GET` | `/api/health` | No | Service health and uptime check |
| `POST` | `/api/auth/register` | No | Create account & generate JWT |
| `POST` | `/api/auth/login` | No | Authenticate & issue JWT |
| `GET` | `/api/auth/me` | Yes | Get authenticated user profile |
| `PUT` | `/api/auth/profile` | Yes | Update user display name |
| `PUT` | `/api/auth/change-password` | Yes | Update password with verification |
| `POST` | `/api/auth/logout` | Yes | Invalidate client session |
| `GET` | `/api/expenses` | Yes | Paginated expense listing (filters, search, sort) |
| `POST` | `/api/expenses` | Yes | Create new expense |
| `GET` | `/api/expenses/{id}` | Yes | Get expense details by ID |
| `PUT` | `/api/expenses/{id}` | Yes | Update existing expense |
| `DELETE` | `/api/expenses/{id}` | Yes | Delete expense |
| `GET` | `/api/expenses/summary` | Yes | Statistics: Total, Count, Avg, Min, Max |
| `GET` | `/api/expenses/category-summary` | Yes | Spending grouped by category |
| `GET` | `/api/expenses/monthly-summary` | Yes | 6-month historical monthly spend for charts |
| `GET` | `/api/expenses/export` | Yes | Export expenses as RFC 4180 CSV file |
| `GET` | `/api/budgets` | Yes | List budgets with real-time spending progress |
| `POST` | `/api/budgets` | Yes | Create monthly or category budget |
| `GET` | `/api/budgets/{id}` | Yes | Get budget details by ID |
| `PUT` | `/api/budgets/{id}` | Yes | Update budget limit or period |
| `DELETE` | `/api/budgets/{id}` | Yes | Delete budget |

---

## 💻 Installation & Setup

### Prerequisites
- **Java**: JDK 21 or higher (JDK 26 tested)
- **Maven**: 3.8+ (Maven 3.9 installed)

### Build and Run
```bash
# Navigate to backend directory
cd backend

# Run automated tests
mvn test

# Start the Spring Boot application
mvn spring-boot:run
```

---

## 🚀 Accessing the Application

When the Spring Boot application starts on port 5000:
1. **Frontend Application**: Open [http://localhost:5000/](http://localhost:5000/) (Spring Boot directly serves the static HTML/CSS/JS frontend).
2. **Interactive Swagger UI**: Open [http://localhost:5000/swagger-ui.html](http://localhost:5000/swagger-ui.html) to test all REST API endpoints interactively.
3. **H2 Database Console**: Open [http://localhost:5000/h2-console](http://localhost:5000/h2-console) (JDBC URL: `jdbc:h2:file:./data/expensedb`, Username: `sa`, Password: *blank*).
4. **Health Check**: Open [http://localhost:5000/api/health](http://localhost:5000/api/health).

---

## 🧪 Automated Testing Suite

Run tests via Maven:
```bash
mvn test --file backend/pom.xml
```

**Results**:
```text
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Tests cover:
- **Authentication**: Registration, duplicate rejection (409), login, invalid password check (401), protected `/api/auth/me`.
- **Expenses**: CRUD, pagination, category filtering, summary calculation, and **strict multi-tenant isolation** (verifying User B cannot view, edit, or delete User A's expense).
- **Budgets**: CRUD, duplicate period rejection (409), dynamic expense reconciliation, threshold status warnings (>=80%), and tenant isolation.

---

## 🎙️ Interview Talking Points

1. **Why Spring Boot for Enterprise Microservices?**
   - Strong typing, compile-time validation, mature dependency injection ecosystem (`@Service`, `@Repository`, `@RestController`), and seamless transaction management (`@Transactional`).
2. **Multi-Tenant Authorization Security**:
   - Preventing IDOR vulnerabilities: Every repository query mandates `WHERE user.id = :userId`. Attempting to access another user's expense or budget returns an immediate 404, preventing both unauthorized access and ID enumeration.
3. **Dynamic Spending Reconciliation vs Denormalized Storage**:
   - Rather than storing "spent so far" in the `Budget` entity (which easily falls out of sync when expenses are created, modified, or deleted), spending is computed on-the-fly via indexed SQL aggregations (`SUM(e.amount)`).
4. **Zero-Dependency Local Persistence**:
   - Using H2 file-mode persistence (`jdbc:h2:file:./data/expensedb`) allows the project to be cloned and run immediately by any reviewer without installing external database software, while supporting seamless migration to PostgreSQL or MySQL simply by updating `application.properties`.
