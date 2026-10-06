# 🏢 Enterprise Resource Planning (ERP) Software

An all-in-one Enterprise Resource Planning (ERP) web application built using **Java 17**, **Spring Boot 3**, **Spring Data JPA**, **Thymeleaf**, and an embedded **H2 Database** (configured in PostgreSQL mode).

---

## 🗂️ Project Architecture: Where is Frontend & Backend?

This project follows the standard Spring Boot monolithic architecture:

```
erpsoftware/
├── pom.xml                                  <-- Maven build & dependency configuration
│
├── 🎨 FRONTEND (UI & Client-side)
│   └── src/main/resources/
│       ├── templates/                       <-- HTML Pages & Views (Thymeleaf)
│       │   ├── fragments/                   <-- Reusable UI components (navbar, sidebar, head, footer)
│       │   ├── booking-request-*.html       <-- Booking Request views
│       │   ├── sales-*.html                 <-- Challan / Sales Order views
│       │   ├── purchase-order-*.html        <-- Purchase Order views
│       │   ├── Current-stock.html           <-- Real-time Stock Inventory
│       │   ├── account-*.html               <-- Account Payable & Receivable views
│       │   └── *-form.html / *-list.html    <-- Vendor, Item, Unit, Supplier, User CRUD forms
│       └── static/                          <-- Static Web Assets
│           ├── css/                         <-- Custom CSS styles
│           ├── js/                          <-- Custom JavaScript scripts
│           ├── images/                      <-- Project logos and icons
│           └── vendors/                     <-- Third-party UI libraries (Bootstrap 4, DataTables, Select2)
│
├── ⚙️ BACKEND (Java & Business Logic)
│   └── src/main/java/com/erp/erpsoftware/
│       ├── ErpsoftwareApplication.java      <-- Spring Boot Entry Point & DB Schema Initializer
│       ├── controller/                      <-- Web Controllers (Handles HTTP routing & request handling)
│       ├── service/                         <-- Business Logic Services
│       ├── repository/                      <-- Spring Data JPA Data Access Layer
│       ├── entity/                          <-- JPA Entities (Database tables & relationships)
│       ├── bean/                            <-- DTOs & Form backing objects
│       └── config/                          <-- Security & Application configuration
│
└── 🗄️ DATABASE & CONFIGURATION
    └── src/main/resources/
        └── application.properties           <-- Server port (8081), Database connection, JPA configuration
```

---

## 🚀 How to Run the Project Locally

### 1. Prerequisites
- **Java JDK 17** or higher installed (`java -version`)
- **Git** installed

### 2. Clone the Repository
```bash
git clone https://github.com/molijain31/erpsoftware.git
cd erpsoftware
```

### 3. Open in Your Code Editor
- **VS Code / Antigravity IDE**: Open the editor, click **File > Open Folder...**, and select the `erpsoftware` folder.

### 4. Start the Application
Open a terminal in the project root and run the Maven wrapper:

- **On Windows (PowerShell / Command Prompt)**:
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
- **On Linux / macOS**:
  ```bash
  chmod +x mvnw
  ./mvnw spring-boot:run
  ```

---

## 🌐 Accessing the Application

Once the console displays `Tomcat started on port 8081 (http)`:

| Service | URL | Credentials |
| :--- | :--- | :--- |
| **ERP Dashboard** | [http://localhost:8081/sales](http://localhost:8081/sales) | Open directly (or `beena` / `Beena@123`) |
| **Home Page** | [http://localhost:8081](http://localhost:8081) | Redirects automatically to `/sales` |
| **H2 Database Console** | [http://localhost:8081/h2-console](http://localhost:8081/h2-console) | **JDBC URL**: `jdbc:h2:file:./data/erp_software`<br>**User**: `sa`<br>**Password**: *(leave blank)* |

---

## 💼 Core Features & Modules

1. **Master Management**:
   - Vendor Master (`/vendors`)
   - Item Master (`/items`)
   - Unit Master (`/units`)
   - Type Master (`/types`)
   - Supplier Master (`/suppliers`)
   - User Master (`/users`)

2. **Transactions**:
   - **Current Stock**: Live inventory tracking (`/stocks`)
   - **Booking Request**: Client order booking management (`/bookingrequest`)
   - **Challan Order (Sales)**: Sales delivery order generation (`/sales`)
   - **Delivered Quantity**: Track dispatched and received quantities (`/delivered-sales`)
   - **Purchase Order**: Vendor procurement and inventory inflow (`/purchaseorders`)
   - **Account Payable**: Vendor payment tracking (`/accounts`)
   - **Account Receivable**: Supplier payment tracking (`/supplier-accounts`)

3. **Financial Reports & Stock Ledgers**:
   - Stock movement ledger (`/reports/ledger-stock`)
   - Account ledger report (`/reports/account-ledger`)

---

## 🛠️ Tech Stack
- **Backend**: Spring Boot 3.2.1, Spring Data JPA, Hibernate, Spring Security
- **Frontend**: Thymeleaf, HTML5, CSS3, JavaScript, jQuery, Bootstrap 4, DataTables
- **Database**: H2 Database Engine (embedded file-based storage with PostgreSQL compatibility)
- **Build Tool**: Maven Wrapper (`mvnw`)
