# IMS Backend - Complete Setup Guide

This guide will help you copy and paste all the code files to create your complete Spring Boot backend.

## Step 1: Create Project Root Directory

Create a new directory for your project:
```bash
mkdir ims-backend
cd ims-backend
```

## Step 2: Copy Files in This Order

### 2.1 Root Files

Create these files in the root directory (`ims-backend/`):

1. **pom.xml** - Maven configuration file
2. **README.md** - Project documentation

### 2.2 Application Configuration

Create directory: `src/main/resources/`

Files to create:
1. **application.yml** - Application configuration

### 2.3 Main Application Class

Create directory: `src/main/java/com/ims/`

File to create:
1. **InventoryManagementSystemApplication.java** - Main Spring Boot application

### 2.4 Enums

Create directory: `src/main/java/com/ims/enums/`

Files to create (in any order):
1. **Role.java**
2. **PaymentMethod.java**
3. **SaleStatus.java**
4. **DebtStatus.java**
5. **TransferStatus.java**
6. **StockMovementType.java**

### 2.5 Entities

Create directory: `src/main/java/com/ims/entity/`

Files to create (in this order):
1. **BaseEntity.java** (must be first)
2. **Branch.java**
3. **Category.java**
4. **User.java**
5. **Product.java**
6. **BranchInventory.java**
7. **StockMovement.java**
8. **Sale.java**
9. **SaleItem.java**
10. **CreditAccount.java**
11. **Debt.java**
12. **DebtPayment.java**
13. **StockTransfer.java**
14. **TransferItem.java**
15. **AuditLog.java**

### 2.6 Repositories

Create directory: `src/main/java/com/ims/repository/`

Files to create (in any order):
1. **UserRepository.java**
2. **BranchRepository.java**
3. **CategoryRepository.java**
4. **ProductRepository.java**
5. **BranchInventoryRepository.java**
6. **StockMovementRepository.java**
7. **SaleRepository.java**
8. **SaleItemRepository.java**
9. **CreditAccountRepository.java**
10. **DebtRepository.java**
11. **DebtPaymentRepository.java**
12. **StockTransferRepository.java**
13. **TransferItemRepository.java**
14. **AuditLogRepository.java**

### 2.7 DTOs (Data Transfer Objects)

Create directories:
- `src/main/java/com/ims/dto/request/`
- `src/main/java/com/ims/dto/response/`

**Request DTOs** (in `request/` directory):
1. **LoginRequest.java**
2. **UserRequest.java**
3. **ProductRequest.java**

**Response DTOs** (in `response/` directory):
1. **ApiResponse.java** (generic wrapper)
2. **AuthResponse.java**

### 2.8 Exceptions

Create directory: `src/main/java/com/ims/exception/`

Files to create:
1. **ResourceNotFoundException.java**
2. **BadRequestException.java**
3. **GlobalExceptionHandler.java**

### 2.9 Security

Create directory: `src/main/java/com/ims/security/`

Files to create:
1. **UserDetailsServiceImpl.java**
2. **JwtAuthenticationFilter.java**

### 2.10 Utilities

Create directory: `src/main/java/com/ims/util/`

File to create:
1. **JwtUtil.java**

### 2.11 Configuration

Create directory: `src/main/java/com/ims/config/`

Files to create:
1. **SecurityConfig.java**
2. **AuditorAwareImpl.java**
3. **OpenApiConfig.java**
4. **DataInitializer.java**

### 2.12 Services

Create directory: `src/main/java/com/ims/service/`

Files to create:
1. **AuthService.java**
2. **UserService.java**
3. **ProductService.java**

### 2.13 Controllers

Create directory: `src/main/java/com/ims/controller/`

Files to create:
1. **AuthController.java**
2. **UserController.java**
3. **ProductController.java**

## Step 3: Verify Directory Structure

After copying all files, your structure should be:

```
ims-backend/
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── ims/
        │           ├── InventoryManagementSystemApplication.java
        │           ├── config/
        │           │   ├── AuditorAwareImpl.java
        │           │   ├── DataInitializer.java
        │           │   ├── OpenApiConfig.java
        │           │   └── SecurityConfig.java
        │           ├── controller/
        │           │   ├── AuthController.java
        │           │   ├── ProductController.java
        │           │   └── UserController.java
        │           ├── dto/
        │           │   ├── request/
        │           │   │   ├── LoginRequest.java
        │           │   │   ├── ProductRequest.java
        │           │   │   └── UserRequest.java
        │           │   └── response/
        │           │       ├── ApiResponse.java
        │           │       └── AuthResponse.java
        │           ├── entity/
        │           │   ├── [15 entity files]
        │           ├── enums/
        │           │   ├── [6 enum files]
        │           ├── exception/
        │           │   ├── [3 exception files]
        │           ├── repository/
        │           │   ├── [14 repository files]
        │           ├── security/
        │           │   ├── [2 security files]
        │           ├── service/
        │           │   ├── [3 service files]
        │           └── util/
        │               └── JwtUtil.java
        └── resources/
            └── application.yml
```

## Step 4: Build and Run

### Option 1: Using Your IDE (Recommended)

1. Open your IDE (IntelliJ IDEA, Eclipse, VS Code)
2. File → Open → Select `ims-backend` folder
3. Wait for Maven to download dependencies
4. Right-click `InventoryManagementSystemApplication.java`
5. Click "Run"

### Option 2: Using Command Line

```bash
# On Windows:
mvnw.cmd clean spring-boot:run

# On Mac/Linux:
./mvnw clean spring-boot:run
```

### First Run Setup

The application will:
1. Download all Maven dependencies (takes 2-5 minutes first time)
2. Create an H2 in-memory database
3. Initialize with sample data
4. Start the server on port 8080

## Step 5: Test the Application

### Access Points

1. **Application:** http://localhost:8080
2. **Swagger UI:** http://localhost:8080/swagger-ui.html
3. **H2 Console:** http://localhost:8080/h2-console

### Test Login API

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

Expected response:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGc...",
    "userId": 1,
    "username": "admin",
    "role": "ADMIN"
  }
}
```

## Step 6: Common Issues & Solutions

### Issue: "Maven not found"
**Solution:** Use the Maven wrapper (mvnw) included with the project:
```bash
./mvnw spring-boot:run
```

### Issue: "Port 8080 already in use"
**Solution:** Change the port in `application.yml`:
```yaml
server:
  port: 8081
```

### Issue: "Unable to find main class"
**Solution:** Make sure all files are in the correct package structure:
- Main class should be at: `src/main/java/com/ims/InventoryManagementSystemApplication.java`

### Issue: "Compilation errors"
**Solution:** Clean and rebuild:
```bash
./mvnw clean install -U
```

## Step 7: Default Test Data

After first run, you'll have:

**Users:**
- Username: `admin` | Password: `admin123` | Role: ADMIN
- Username: `manager` | Password: `manager123` | Role: MANAGER
- Username: `seller` | Password: `seller123` | Role: SELLER

**Branches:**
- Main Branch (Yaoundé)
- Downtown Branch (Douala)

**Products:**
- Brake Pads
- Oil Filter
- Shock Absorber

## Step 8: Next Steps

Once the backend is running successfully:

1. ✅ Test all endpoints using Swagger UI
2. 📝 Review API documentation
3. 🔨 Implement additional services as needed
4. 🎨 Connect to your frontend application
5. 🗄️ Switch to PostgreSQL for production

## Need Help?

- Check console logs for errors
- Visit Swagger UI for API testing: http://localhost:8080/swagger-ui.html
- Check H2 console to view database: http://localhost:8080/h2-console

## Summary

You now have a fully functional Spring Boot backend with:
- ✅ JWT Authentication
- ✅ User Management
- ✅ Product Management
- ✅ Complete database schema
- ✅ REST API with documentation
- ✅ Security configuration
- ✅ Sample data for testing

The backend is ready for you to:
- Add more services (Sales, Inventory, etc.)
- Connect to your React frontend
- Deploy to your VPS when ready

**Happy Coding! 🚀**
