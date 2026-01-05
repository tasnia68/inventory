# Inventory Management System

## Overview
A multi-tenant, industry-agnostic inventory management system using Spring Boot, following enterprise patterns from SAP/Odoo/NetSuite.

## Setup Instructions

### Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL

### Running the Application

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd inventory-system
   ```

2. **Database Setup:**
   Ensure PostgreSQL is running and the following databases exist (or configure `application-dev.yml` to match your setup):
   ```sql
   CREATE DATABASE inventory_dev;
   CREATE DATABASE inventory_staging;
   CREATE DATABASE inventory_prod;
   ```

3. **Build the project:**
   ```bash
   mvn clean install
   ```

4. **Run the application:**
   ```bash
   mvn spring-boot:run -Dspring.profiles.active=dev
   ```

## Documentation
See [ROADMAP.md](ROADMAP.md) for the project implementation roadmap and status.
