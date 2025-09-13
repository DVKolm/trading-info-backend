# Trading Info Backend - Spring Boot

Spring Boot backend for the Trading Info application, migrated from Node.js to Java 21.

## Prerequisites

- Java 21
- PostgreSQL 12+
- Gradle 8+

## Configuration

The application uses PostgreSQL as the database. Configuration can be found in:
- `src/main/resources/application.properties` - Main configuration
- `src/main/resources/application-dev.properties` - Development profile
- `src/main/resources/application-prod.properties` - Production profile
- `.env` - Environment variables (for production deployment)

## Database Setup

Ensure PostgreSQL is running and create the database:

```sql
CREATE DATABASE trading_info;
CREATE USER trading_user WITH PASSWORD 'Ke5zrdsf';
GRANT ALL PRIVILEGES ON DATABASE trading_info TO trading_user;
```

## Running the Application

### Development Mode
```bash
./gradlew bootRun
```

### Production Mode
```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### Building JAR
```bash
./gradlew clean build
java -jar build/libs/trading-info-backend-1.0.0.jar --spring.profiles.active=prod
```

## API Endpoints

The application runs on port 3001 with context path `/api`:

- `GET /api/lessons/folders` - Get lesson folders
- `GET /api/lessons/structure` - Get lesson structure
- `GET /api/lessons/content/*` - Get specific lesson content
- `GET /api/lessons/resolve` - Resolve lesson links
- `GET /api/lessons/search` - Search lessons
- `POST /api/lessons/progress` - Update user progress
- `POST /api/lessons/analytics` - Track analytics events
- `POST /api/upload/lessons` - Upload lessons (ZIP file)
- `DELETE /api/upload/lessons/{folder}` - Delete lesson folder

## Technology Stack

- Java 21
- Spring Boot 3.2.2
- Spring Data JPA
- PostgreSQL
- Lombok
- JWT for authentication
- Gradle build system

## Production Deployment

For production deployment on server (5.129.241.61):

1. Copy the JAR file to the server
2. Set environment variables or use the .env file
3. Run with production profile:
```bash
java -jar trading-info-backend-1.0.0.jar --spring.profiles.active=prod
```

Or use systemd service for automatic startup.