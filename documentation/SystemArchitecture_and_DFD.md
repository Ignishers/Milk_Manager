# Data Flow Diagrams (DFD) and System Architecture

This document uses Mermaid.js syntax to visually represent the data pipelines and entity interactions in Milk Manager 2.

## Level 0 DFD (Context Diagram)

The system boundaries and basic inputs/outputs.

```mermaid
graph LR
  vendor(Milk Vendor)
  sys((Milk Manager 2))
  db[(Local Database)]
  report[PDF Reports]

  vendor -- Setup Route/Customer --> sys
  vendor -- Daily Entry (Morning/Evening) --> sys
  vendor -- Log Payment --> sys

  sys -- Store Data --> db
  db -- Retrieve State --> sys

  sys -- Generate Invoice --> report
  report -- Shared with --> Customer(Customer)
```

## Level 1 DFD (Activity Breakdown)

Breaking down the main modules of the system.

```mermaid
graph TD
  vendor[Milk Vendor]

  subgraph Mobile App
    custMgmt(1. Customer Management)
    dailyTnx(2. POS Interface / Daily Transaction)
    billing(3. Billing & Payments)
    reports(4. Reporting & Analytics)
  end

  db[(SQLite / DAOs)]

  vendor -- Create/Edit Customer --> custMgmt
  vendor -- Input Quantity/Shift --> dailyTnx
  vendor -- Record Payment --> billing
  vendor -- View Dashboards --> reports

  custMgmt -- Writes Customer Data --> db
  dailyTnx -- Writes Transaction Data --> db
  billing -- Updates Current Due --> db
  db -- Supplies Chart Data --> reports

  billing -- Yields --> Output[PDF Bill]
```

## Entity-Relationship Diagram (ERD)

How the core entities in the `database` relate to each other.

```mermaid
erDiagram
    RouteGroup {
        long id PK
        string name
    }
    
    Customer {
        long id PK
        string name
        string mobile
        string address
        double currentDue
        double defaultQuantity
        long routeGroupId FK
    }
    
    DailyTransaction {
        long id PK
        string date
        int shift
        double quantity
        long customerId FK
    }
    
    MilkRate {
        long id PK
        double ratePerLiter
        string effectiveDate
    }

    RouteGroup ||--o{ Customer : "contains"
    Customer ||--o{ DailyTransaction : "has"
```

## Architecture: MVVM Structure Flow

```mermaid
sequenceDiagram
    participant View (Activity/Fragment)
    participant ViewModel
    participant Repository (DAO layer)
    participant SQLite DB

    View->>ViewModel: Action triggered (e.g., Load Customers)
    ViewModel->>Repository: Method Request (e.g., getAllCustomers())
    Repository->>SQLite DB: Execute Query
    SQLite DB-->>Repository: Return Cursor/Models
    Repository-->>ViewModel: Return Data List
    ViewModel-->>View: LiveData Observes changes
```
