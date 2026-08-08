# Online Food Delivery System - Setup Guide

## Prerequisites

1. **Java 17 (LTS)** - Already installed ✅
2. **Maven** - Already installed ✅
3. **MySQL Server** - Needs to be installed and running
4. **Node.js & npm** - For the frontend

---

## Step 1: Install MySQL

If you don't have MySQL installed:

```bash
brew install mysql
brew services start mysql
```

---

## Step 2: Create Database

Run the MySQL command line:

```bash
mysql -u root
```

Then execute:

```sql
CREATE DATABASE food_delivery;
exit;
```

---

## Step 3: Configure Database Password

Edit `backend/src/main/resources/application.properties`:

```properties
spring.datasource.password=YOUR_MYSQL_ROOT_PASSWORD
```

If you don't have a password (default brew installation), leave it empty or set it to an empty string.

---

## Step 4: Run the Backend

From the project root:

```bash
cd backend
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
mvn spring-boot:run
```

**Swagger UI** will be available at: `http://localhost:8080/swagger-ui/index.html`

---

## Step 5: Run the Frontend

In a new terminal:

```bash
cd frontend
npm install
npm start
```

Frontend will be available at: `http://localhost:3000`

---

## Quick Start Script

To make it easier, create a file called `run-backend.sh`:

```bash
#!/bin/bash
cd backend
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
mvn spring-boot:run
```

Make it executable:
```bash
chmod +x run-backend.sh
./run-backend.sh
```

---

## Troubleshooting

### Error: "Cannot create PoolableConnectionFactory"
- MySQL server is not running: `brew services start mysql`
- Database doesn't exist: Create it using the SQL command above
- Wrong password in `application.properties`

### Error: "Lombok getters/setters not found"
- Make sure you're using Java 17, not Java 25
- Run with the export command above
