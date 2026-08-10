# EKOTE

EKOTE is a web-based inventory, issue/return, maintenance, and record management system built with Spring Boot. The application provides a centralized interface for managing inventory records, tracking individual items, recording issue transactions, handling maintenance information, and generating QR codes.

## Core Features

### Inventory Management
- Manage inventory and stock information
- Store individual item information using unique identifiers
- Track available quantities
- Prevent items from being issued when inventory is unavailable

### Issue and Return Management
- Search and verify users before issuing an item
- Identify individual items using unique identifiers
- Prevent the same item from being issued multiple times simultaneously
- Automatically update inventory quantity after issue
- Store issue date and current issue status
- Support issue/return record management

### QR Code Support
- Generate QR codes for individual records
- QR functionality powered by ZXing
- Provide QR-based access to stored information

### User Management
- User registration
- Login functionality
- Spring Security integration
- User lookup during issue operations

### Maintenance
- Maintenance management interface
- Store and access maintenance-related information for managed inventory

### Record Management
- Record book functionality
- Central dashboard for accessing major application modules
- Individual inventory information management

## Technology Stack

### Backend
- Java 21
- Spring Boot 3.5.5
- Spring MVC
- Spring Data JPA
- Spring Security
- Maven

### Frontend
- Thymeleaf
- HTML
- CSS
- JavaScript

### Database
- MySQL
- MySQL Connector/J
- Spring Data JPA

### Additional Libraries
- ZXing Core
- ZXing JavaSE
- Firebase Admin SDK
- Lombok

### Deployment
- Docker

## Application Modules

The application contains controllers and modules for:

- Home and dashboard
- Registration and authentication
- Inventory management
- Item information management
- Issue and return operations
- Maintenance
- QR code generation
- Record book management

## Project Structure

```text
EKOTE-Spring/
└── demo/
    ├── src/
    │   ├── main/
    │   │   ├── java/com/ekote/
    │   │   │   ├── config/
    │   │   │   ├── controller/
    │   │   │   ├── entities/
    │   │   │   └── repositories/
    │   │   └── resources/
    │   │       ├── static/
    │   │       └── templates/
    │   └── test/
    ├── pom.xml
    ├── Dockerfile
    ├── mvnw
    └── mvnw.cmd
```

## Requirements

To build or run the application locally:

- Java 21
- Maven or the included Maven Wrapper
- MySQL

## Build

Move into the application directory:

```bash
cd demo
```

Using Maven Wrapper:

```bash
./mvnw clean package
```

or with Maven installed:

```bash
mvn clean package
```

## Run

```bash
cd demo
./mvnw spring-boot:run
```

## Docker

A Dockerfile is included under the `demo` directory for containerized deployment.

## How the Issue Workflow Works

When an item is issued, EKOTE:

1. Verifies that the requested user exists.
2. Looks up the individual item using its unique identifier.
3. Confirms that the associated inventory record exists.
4. Checks that the item has not already been issued.
5. Verifies that inventory quantity is available.
6. Decreases the available inventory quantity.
7. Creates an issue record containing the user, item identifier, issue date, and issue status.

This provides traceability between inventory quantities and individual issued items.

## Purpose

EKOTE was developed as a centralized management application for maintaining inventory records and operational workflows such as registration, item tracking, issue/return processing, maintenance, and QR-based identification.

## Repository Status

The project contains the application source code, Maven configuration, frontend templates and resources, database integration, security configuration, and Docker support.
