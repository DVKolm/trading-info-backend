# Trading Info Backend - Deployment Guide

## Server Configuration

### Production Server Details
- **Host**: `5.129.241.61`
- **SSH Access**: `root@5.129.241.61`
- **Application Port**: `8080`
- **Domain**: `heart-trader.duckdns.org`
- **API Base URL**: `https://heart-trader.duckdns.org/api`
- **Frontend URL**: `https://heart-trader.duckdns.org`
- **Direct API Access**: `http://5.129.241.61:8080/api`

## Prerequisites

### PostgreSQL Database
- **Version**: PostgreSQL 16
- **Port**: 5432 (localhost only)
- **Database**: `trading_info`
- **Username**: `trading_user`
- **Password**: `Ke5zrdsf`
- **Status Check**: `pg_lsclusters`
- **Start Command**: `systemctl start postgresql@16-main`

### Docker
- Docker must be installed and running on the server
- Docker Hub repository: `dekr0x/trading-info-backend:latest`

## Deployment Steps

### 1. Build and Push Application

```bash
# From local development machine
cd /path/to/trading-info-backend

# Build JAR
./gradlew bootJar

# Build Docker image
docker build -t dekr0x/trading-info-backend:latest .

# Push to Docker Hub
docker push dekr0x/trading-info-backend:latest
```

### 2. Deploy on Server

```bash
# SSH to server
ssh root@5.129.241.61

# Pull latest image
docker pull dekr0x/trading-info-backend:latest

# Stop existing container (if running)
docker stop trading-info-backend
docker rm trading-info-backend

# Start new container
docker run -d \
  --name trading-info-backend \
  --network host \
  --restart unless-stopped \
  -e SPRING_PROFILES_ACTIVE=prod \
  dekr0x/trading-info-backend:latest
```

### 3. Verify Deployment

```bash
# Check container status
docker ps | grep trading-info

# Check application logs
docker logs trading-info-backend --tail 20

# Test API endpoint via domain
curl -s 'https://heart-trader.duckdns.org/api/lessons/structure'

# Test API endpoint directly
curl -s 'http://5.129.241.61:8080/api/lessons/structure'
```

## Configuration Files

### Application Properties (Production)
File: `src/main/resources/application-prod.properties`

```properties
# Server Configuration
server.port=8080
server.address=5.129.241.61

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/trading_info
spring.datasource.username=trading_user
spring.datasource.password=Ke5zrdsf

# CORS Configuration
cors.allowed-origins=https://heart-trader.duckdns.org,http://5.129.241.61:8080
```

## Nginx Configuration

The nginx reverse proxy configuration is located at `/etc/nginx/sites-available/trading-app`.

Key points:
- Proxies `/api/` requests to `http://5.129.241.61:8080/api/`
- Serves React frontend from `/var/www/trading-app`
- SSL enabled with Let's Encrypt certificates
- Domain: `heart-trader.duckdns.org`

### Update Nginx Configuration
```bash
# Edit config
nano /etc/nginx/sites-available/trading-app

# Test configuration
nginx -t

# Reload nginx
systemctl reload nginx
```

## Troubleshooting

### Common Issues

1. **502 Bad Gateway**
   - Check if container is running: `docker ps`
   - Check application logs: `docker logs trading-info-backend`
   - Verify PostgreSQL is running: `pg_lsclusters`

2. **Database Connection Failed**
   - Ensure PostgreSQL is running: `pg_lsclusters` (should show "online")
   - Start PostgreSQL if needed: `pg_ctlcluster 16 main start`
   - Use `--network host` for container to access localhost PostgreSQL
   - Check database credentials in application-prod.properties
   - PostgreSQL listens on localhost:5432 only

3. **Port Conflicts**
   - Application runs on port 8080
   - PostgreSQL runs on port 5432
   - Use `ss -tlnp | grep 8080` to check port usage

4. **Nginx 502 Errors**
   - Check nginx error logs: `tail -f /var/log/nginx/trading-app-error.log`
   - Verify nginx proxy target matches application binding address
   - Ensure application listens on correct IP (5.129.241.61:8080)

### Network Configuration
- Container uses `--network host` to access localhost PostgreSQL
- Application binds to `5.129.241.61:8080`
- Nginx proxies `/api/` requests from `https://heart-trader.duckdns.org` to `http://5.129.241.61:8080/api/`
- Frontend connects via domain: `https://heart-trader.duckdns.org/api`

## API Endpoints

### Main Endpoints
- `GET /api/lessons/structure` - Get lesson structure
- `POST /api/upload/lessons` - Upload lessons (ZIP files)
- `POST /api/telegram/check-subscription/{userId}` - Check Telegram subscription

### Upload Endpoints
- `POST /api/upload/lesson` - Upload single lesson (.md or .zip)
  - Body: `FormData` with `lesson` file and `targetFolder` parameter
  - Headers: `initData` (Telegram WebApp data) for authentication
  - Response: `{"success": boolean, "message": string, "fileName": string}`

- `POST /api/upload/lessons` - Upload lessons (ZIP files with .md files inside)
  - Body: `FormData` with `file` and `targetFolder` parameter
  - Headers: `X-Telegram-User-Id` (admin telegram ID: 781182099)
  - Response: `{"success": boolean, "filesUploaded": number, "files": string[], "errors": string[]}`

### Admin Folder Management Endpoints
- `POST /api/upload/folder` - Create folder (legacy with initData)
  - Body: `{"initData": string, "folderName": string, "subscriptionRequired": boolean}`
  - Response: `{"message": string}` or `{"error": string}`

- `POST /api/upload/folder/admin` - Create folder (admin with header)
  - Headers: `X-Telegram-User-Id: 781182099`
  - Body: `{"folderName": string, "subscriptionRequired": boolean}`
  - Response: `{"message": string}` or `{"error": string}`

- `POST /api/upload/folder/subscription` - Update folder subscription requirement
  - Body: `{"initData": string, "folderPath": string, "subscriptionRequired": boolean}`
  - Response: `{"message": string}` or `{"error": string}`

### Admin Management Endpoints
- `GET /api/upload/admin/check` - Check admin access
  - Headers: `X-Telegram-User-Id: 781182099`
  - Response: `{"isAdmin": boolean, "userId": number}`

- `GET /api/upload/file-tree` - Get file tree for admin panel
  - Headers: `X-Telegram-User-Id: 781182099`
  - Response: `{"structure": [{"id": string, "name": string, "type": "folder|file", "path": string, "children": []}]}`

- `POST /api/upload/delete-lesson` - Delete single lesson
  - Body: `{"initData": string, "lessonPath": string}`
  - Response: `{"message": string}` or `{"error": string}`

- `DELETE /api/upload/lessons/{folder}` - Delete entire folder
  - Headers: `X-Telegram-User-Id: 781182099`
  - Response: `{"message": string}`

### Cache Management Endpoints
- `POST /api/upload/clear-cache` - Clear Redis cache
  - Body: `{"initData": string}`
  - Response: `{"message": string}`

- `POST /api/upload/legacy-clear-cache` - Clear cache (legacy endpoint)
  - Body: `FormData` with `initData` parameter
  - Response: `{"message": string}`

### Health Check
- Application startup can be verified by checking logs for "Started TradingInfoBackendApplication"
- API availability via domain: `curl https://heart-trader.duckdns.org/api/lessons/structure`
- API availability directly: `curl http://5.129.241.61:8080/api/lessons/structure`

## Frontend Deployment

### Frontend Configuration
Ensure frontend connects to:
- **Primary API URL**: `https://heart-trader.duckdns.org/api` (recommended)
- **Fallback API URL**: `http://5.129.241.61:8080/api`
- **Domain**: `heart-trader.duckdns.org`
- **Server IP**: `5.129.241.61`
- **Port**: `8080`

### Frontend Deployment Process

**IMPORTANT**: Always follow this exact process - first push to git, then pull and build on server. Do NOT build locally and copy files.

1. **From Development Machine**:
   ```bash
   # Navigate to frontend project directory
   cd /path/to/frontend-project

   # Commit and push changes to repository
   git add -A
   git commit -m "Your changes description"
   git push origin master
   ```

2. **On Server**:
   ```bash
   # SSH to server
   ssh root@5.129.241.61

   # Navigate to frontend directory
   cd /var/www/trading-app

   # Pull latest changes
   git pull origin main

   # Install/update dependencies
   npm install

   # Build for production
   npm run build

   # Verify nginx serves the updated files
   systemctl reload nginx
   ```

3. **Verify Frontend Deployment**:
   ```bash
   # Check if build was successful
   ls -la /var/www/trading-app/build/

   # Test frontend access
   curl -I https://heart-trader.duckdns.org/

   # Check nginx status
   systemctl status nginx
   ```

### Frontend Directory Structure
- **Frontend Location**: `/var/www/trading-app`
- **Build Directory**: `/var/www/trading-app/build`
- **Nginx Serves**: Static files from build directory
- **Git Repository**: Should be cloned directly on server

## First Time Setup

If this is the first deployment, you may need to:

1. **Create PostgreSQL Database**:
   ```bash
   sudo -u postgres psql
   CREATE DATABASE trading_info;
   CREATE USER trading_user WITH PASSWORD 'Ke5zrdsf';
   GRANT ALL PRIVILEGES ON DATABASE trading_info TO trading_user;
   \q
   ```

2. **Install Dependencies**:
   - Docker: `curl -fsSL https://get.docker.com -o get-docker.sh && sh get-docker.sh`
   - PostgreSQL 16: `apt update && apt install postgresql-16`

## Notes

- Telegram notifications to channel are **disabled**
- Application uses production profile (`SPRING_PROFILES_ACTIVE=prod`)
- Container restarts automatically unless stopped (`--restart unless-stopped`)
- Database runs locally on server, not in container
- Application uses JPA auto-update, so database schema is created/updated automatically