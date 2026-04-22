# Software Requirements Specification (SRS) - Milk Manager 2

## 1. Introduction
### 1.1 Purpose
The purpose of this SRS is to define the requirements, architecture, and constraints of the Milk Manager 2 application. This app is designed to help milk vendors digitally manage their customer database, daily deliveries, and monthly billing.

### 1.2 Scope
Milk Manager 2 provides an offline-first Android solution. It replaces traditional pen-and-paper ledgers, reducing calculation errors and saving time in generating monthly bills.

## 2. Overall Description
### 2.1 User Characteristics
The intended users are local milk vendors and dairy farm owners who require a simple, fast, and mobile interface to log daily entries while on the go.

### 2.2 Operating Environment
- **Platform**: Android OS (Minimum SDK 24 recommended).
- **Storage**: Local SQLite Database.
- **Permissions**: Internet (for future sync/analytics), Storage (for saving PDFs).

## 3. Functional Requirements
1. **Customer Management**: Ability to add, edit, or remove customers. Customers can be assigned to specific "Route Groups."
2. **Daily Entry**: A fast Point of Sale (POS) interface to log milk deliveries per customer for morning/evening shifts.
3. **Billing System**: Automatic calculation of monthly bills considering daily entries, default quantities, and per-liter rates.
4. **Payment Tracking**: Ability to log payments received and update the customer's outstanding balance.
5. **PDF Generation**: Generation of shareable PDF receipts for individual customers.
6. **Reporting**: Visual representation (charts) of total sales and collection over a given period.

## 4. Non-Functional Requirements
1. **Performance**: Screens must load data in under 1 second.
2. **Usability**: UI must be heavily icon-driven and support easy numeric entry to assist users wearing gloves or during active delivery.
3. **Reliability**: Data must be safely persisted offline without data loss upon app closure.
