# FindIt | Campus & Public Space Lost and Found Platform

[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net/)
[![Spring Boot 3.3.4](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-red.svg)](https://flywaydb.org/)
[![MySQL 8](https://img.shields.io/badge/MySQL-8.0-orange.svg)](https://www.mysql.com/)

FindIt is a production-oriented, full-stack web application designed for college campuses and municipal public spaces. It allows students, faculty, and campus staff to report lost and found belongings, run deterministic multi-attribute matching, privately verify ownership, and track property returns through an auditable state machine.

---

## Architecture & Project Structure

The project strictly separates the presentation assets and templates from backend application logic:

```
FindIt/
├── backend/                  # Java 21 & Spring Boot 3.x Backend Service
│   ├── pom.xml               # Maven configuration & packaging
│   ├── mvnw.cmd / mvnw       # Maven wrappers
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/findit/
│   │   │   │   ├── config/          # Security, Web MVC, Seed Data Initializer
│   │   │   │   ├── controller/      # Spring MVC & REST Controllers
│   │   │   │   ├── dto/             # Validation Transfer Objects
│   │   │   │   ├── exception/       # Global ControllerAdvice & Custom Exceptions
│   │   │   │   ├── model/           # JPA Entities (User, Item, Claim, Notification)
│   │   │   │   │   └── enums/       # Statuses, Categories, Roles, Types
│   │   │   │   ├── repository/      # Spring Data JPA Repositories
│   │   │   │   ├── security/        # UserDetailsService, Security Utilities
│   │   │   │   └── service/         # Business Services & Deterministic Matcher
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── db/migration/    # Flyway Versioned Migrations (V1, etc.)
│   │   └── test/                    # JUnit 5 & Mockito Test Suites
│
├── frontend/                 # Separated Frontend UI
│   ├── static/               # Assets served to clients
│   │   ├── css/
│   │   │   ├── main.css      # Navy/blue/white visual system & variables
│   │   │   ├── components.css# Item cards, badges, match meters, tables
│   │   │   └── responsive.css# Mobile off-canvas drawer, media queries
│   │   ├── js/
│   │   │   ├── app.js        # Mobile navigation, accessible keyboard traps
│   │   │   ├── items.js      # Real-time duplicate report warnings & image preview
│   │   │   └── matches.js    # Animated score meters & match comparison
│   │   └── img/
│   │       ├── logo.svg
│   │       └── default-item.svg
│   └── templates/            # Thymeleaf 3 HTML Templates
│       ├── layout/           # Base template, header, footer, alerts
│       ├── pages/            # Landing, Browse, Details, Reports, Dashboard, Matches
│       └── auth/             # Login & Registration
│
├── .env.example              # Environment variables template
├── Dockerfile                # Multi-stage production container build
├── docker-compose.yml        # Orchestration for Spring Boot app + MySQL 8
└── README.md
```

---

## Key Features

1. **Deterministic Weighted Matching Engine (100% total)**:
   - **Category (30%)**: Exact match awards 30 points; related category families award 15 points; mismatch awards 0 points.
   - **Color (20%)**: Normalized token comparison. If either report omits a color, 0 points are awarded without falsely inflating the score.
   - **Location (20%)**: Campus landmark, building, and room token overlap.
   - **Date Proximity (15%)**: Same calendar day = 15 points; 1–3 days = 12 points; 4–7 days = 8 points; 8–14 days = 4 points; 15–30 days = 2 points; > 30 days = 0 points.
   - **Description & Title Similarity (15%)**: Stopword-filtered Jaccard token overlap.
   - **Exclusion Filters**: Automatically ignores incompatible reports (e.g., comparing LOST with LOST), same item IDs, and already RESOLVED or WITHDRAWN reports.
   - Labeled clearly as **Automated Suggestions** with complete breakdown points.

2. **Duplicate Report Warnings**:
   - Live debounced AJAX check (`/api/items/check-duplicate`) queries existing active reports of the same type. If similarity exceeds 65%, a warning banner directs the user to the existing report before duplicate submission.

3. **Secure Verification & Auditable Return Workflow**:
   - **State Machine**:
     - Item: `OPEN` &rarr; `CLAIM_PENDING` &rarr; `RESOLVED` (or `WITHDRAWN`).
     - Claim: `PENDING` &rarr; `APPROVED` or `REJECTED`.
   - **Server-Side Authorization**:
     - Users can manage only their own reports.
     - Users **cannot** submit claims on their own reports.
     - Claimants **cannot** approve their own claims.
     - Only the report owner or an administrator can approve or reject claims.
     - Approving a claim automatically marks the item `RESOLVED`, marks the claim `APPROVED`, rejects other pending claims with audit notices, and notifies all involved parties.

4. **In-App Notification Center**:
   - Instant notifications for high-confidence match discoveries (>= 60%), new ownership claims, claim approvals, and claim rejections.
   - Unread badge counter in header with "Mark all as read" capability.

5. **Moderator & Admin Operations Center**:
   - Real-time aggregate statistics calculated from database entities (total items, open lost vs found, claims resolved, return rate).
   - Item moderation directory (view, edit, delete).
   - Pending claims queue across all campus items.
   - User account management (enable/disable users, promote/demote administrators with guardrails against self-demotion or removing the last admin).

---

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `SERVER_PORT` | HTTP Port for Spring Boot | `8080` |
| `DB_URL` | JDBC Connection URL | `jdbc:mysql://localhost:3306/findit_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `DB_USERNAME` | MySQL Username | `root` |
| `DB_PASSWORD` | MySQL Password | `root` |
| `APP_UPLOAD_DIR` | Directory for uploaded item images | `uploads` |
| `APP_SEED_DATA` | Automatically seed demo campus data on first run | `true` |

---

## Pre-seeded Demo Accounts

When `APP_SEED_DATA=true` (the default for local runs), the application seeds the following test accounts:

| Role | Email | Password | Full Name |
|---|---|---|---|
| **Administrator** | `admin@findit.campus.edu` | `AdminPass123!` | Campus Safety Admin |
| **Student / User** | `alex@campus.edu` | `UserPass123!` | Alex Rivera |
| **Student / User** | `sarah@campus.edu` | `UserPass123!` | Sarah Chen |
| **Student / User** | `mike@campus.edu` | `UserPass123!` | Michael Patel |

---

## How to Run

### Method 1: Local Maven (Recommended)

1. Ensure MySQL is running on `localhost:3306` with credentials matching your `.env` or `application.properties`.
2. Navigate to the `backend/` directory:
   ```bash
   cd backend
   ```
3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
   *(On Windows PowerShell: `.\mvnw.cmd spring-boot:run`)*
4. Open your browser and navigate to:
   ```
   http://localhost:8080
   ```

### Method 2: Docker Compose

To launch the full production stack (Spring Boot application + MySQL 8 container with persistent storage):
```bash
docker-compose up --build
```
Access the application at `http://localhost:8080`.

---

## Running Automated Tests

FindIt includes comprehensive JUnit 5 and Mockito test suites:
```bash
cd backend
./mvnw test
```
The test suite executes against an in-memory MySQL-mode H2 database with Flyway schema migration validation, ensuring 100% isolation from production databases.
