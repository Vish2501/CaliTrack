# CaliTrack Backend

Spring Boot 3.2 backend application with Maven.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL database

## Setup

1. Create a PostgreSQL database named `calitrack`
2. Update database credentials in `src/main/resources/application.yml` if needed
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

## Project Structure

```
src/main/java/com/calitrack/
├── controller/     # REST controllers
├── service/        # Business logic layer
├── repository/     # Data access layer
├── entity/         # JPA entities
├── dto/            # Data Transfer Objects
└── config/         # Configuration classes
```

## Dependencies

- Spring Web
- Spring Data JPA
- Spring Security
- Lombok
- PostgreSQL Driver

## Default Configuration

- Server port: 8080
- Database: PostgreSQL (localhost:5432/calitrack)
- Default user: admin/admin (for Spring Security)
