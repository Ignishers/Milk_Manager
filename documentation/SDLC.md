# Software Development Life Cycle (SDLC) - Milk Manager 2

This document outlines the SDLC phases applied to the Milk Manager 2 Android application.

## 1. Requirement Analysis
- **Objective**: Digitize the manual milk management system for local milkmen.
- **Key Requirements**:
  - Manage a list of customers with contact details, address, and default milk quantity.
  - Record daily milk transactions (morning and evening shifts).
  - Calculate and manage monthly bills based on predefined rates.
  - Generate PDF bills for customers.
  - Track payments and maintain balances.

## 2. System Design
- **Architecture**: Model-View-ViewModel (MVVM) architecture to separate business logic from UI.
- **Database**: SQLite (managed via SQLiteOpenHelper) for local offline storage.
- **UI Design**: Bottom Navigation, Bottom Sheets, and Dialogs for quick data entry and navigation.

## 3. Implementation (Development)
- **Language**: Java
- **Framework**: Android SDK
- **Key Modules**:
  - `activities`: Dashboard, PointOfSale, SalesReport, BillActivity.
  - `database`: DAO layers for Customers, Transactions, Rates, and Routes.
  - `adapters`: RecyclerView adapters for POS, Transactions, and Navigation.
  - `utils`: Helpers for PDF generation and formatting.

## 4. Testing
- **Unit Testing**: Validating DAOs and calculation utilities.
- **Integration Testing**: Ensuring End-to-End flow from customer creation to bill generation.
- **User Acceptance Testing (UAT)**: Real-world testing with local milk distributors for usability feedback.

## 5. Deployment
- **Target**: Android Devices (Smartphones and Tablets).
- **Distribution**: Direct APK installation or via Google Play Store.

## 6. Maintenance
- **Bug Fixes**: Addressing user-reported issues.
- **Enhancements**: Adding features like cloud sync, dark mode, or SMS integration based on future requirements.
