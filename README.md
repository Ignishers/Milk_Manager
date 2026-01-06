# MilkManager2 🥛

MilkManager2 is a comprehensive Android application designed to streamline the daily operations of milk distribution businesses. It helps milkmen and dairy distributors manage customers, track daily milk deliveries, calculate monthly dues, and handle payments efficiently.

## 📱 Key Features

### 🛒 Point of Sale (POS)
- **Quick Entry**: Fast and easy interface to add daily milk records (Morning/Evening/Any time).
- **Smart Quantity Input**: Select from preset quantities (e.g., 0.5l, 1l) or enter amounts by price (e.g., 20rs), with automatic conversion.
- **Session Tracking**: Automatic detection of Morning/Evening sessions based on time of day.
- **Customer Dashboard**: View real-time dues, contact details, and transaction history for each customer.

### 👥 Customer Management
- **Detailed Profiles**: Manage customer information including name, mobile, and default quantity.
- **Route Organization**: Group customers by routes for efficient delivery management.
- **Dues Tracking**: Automatic calculation of current and monthly dues.

### 💰 Financial Management
- **Payment Processing**: Record payments received from customers.
- **Rate Management**: flexible milk rate settings.
- **Transaction History**: View and manage all daily entries with options to delete or modify.

### 📊 Reporting
- **Sales Reports**: Generate and view detailed sales reports (`SalesReportActivity`).
- **Monthly Summaries**: View aggregated data for monthly billing (`MonthSummaryDialog`).

## 🛠️ Technical Stack

- **Platform**: Android (Native)
- **Language**: Java
- **UI/UX**: XML Layouts with Material Design 3 components.
- **Architecture**: MVVM (Model-View-ViewModel) pattern for robust state management.
- **Database**: Local SQLite database accessed via Custom DAOs (Data Access Objects).
- **Concurrency**: `ExecutorService` for background database operations.

## 🚀 Getting Started

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/Ignishers/Milk_Manager.git
    ```
2.  **Open in Android Studio**:
    - Launch Android Studio.
    - Select "Open an existing Android Studio project".
    - Navigate to the cloned directory.
3.  **Build and Run**:
    - Sync Gradle files.
    - Connect an Android device or start an emulator.
    - Click the "Run" button (green play icon).

## 📂 Project Structure

-   `com.ignishers.milkmanager2`
    -   `DAO`: Database Access Objects for SQlite operations.
    -   `model`: Data classes (Customer, DailyTransaction, etc.).
    -   `adapter`: RecyclerView adapters for lists.
    -   `utils`: Utility helper classes.
    -   `views`: Custom UI views.
    -   `*Activity.java`: Main entry points for screens.
    -   `*Fragment.java` & `*BottomSheet.java`: UI components for specific interactions.

## 🤝 Contribution

Contributions are welcome! Please feel free to submit a Pull Request.
