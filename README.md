# Apartment Tracker

A Java 17 and Spring Boot society-management MVP built for a clear, demonstrable viva scope.

## What is included

- Database-backed form login with `ADMIN`, `RESIDENT`, and `MAINTENANCE_HEAD` roles.
- Public resident sign-up collects account credentials, full name, phone, apartment/flat number, email, and optional emergency contact. Passwords are BCrypt-hashed; duplicate usernames and email addresses are rejected.
- Residents can submit validated complaints and see only their own requests.
- Complaints receive a unique category-prefixed reference such as `P1` (Plumbing) or `L1` (Lift); existing complaints receive references on startup.
- Administrators can view all complaints and advance them through Submitted, Assigned, In Progress, Resolved, and Closed.
- Administrators can publish and delete notices; all signed-in roles can read the notice board.
- Administrators can assign active staff to complaints without managing maintenance-head team directories.
- Maintenance heads can add and deactivate staff under their own team, assign unassigned complaints to that team, and track their team's assigned work.
- Maintenance staff receive stable department-specific identifiers such as `SP1` for Plumbing and `SE1` for Electrical. Maintenance heads can view the all-department staff directory while managing only staff added under their own account. Admin and maintenance complaint lists support search by complaint ID, category, issue, resident, status, or assigned staff; maintenance search remains department-scoped.
- Every complaint category routes to its matching maintenance department; department heads see only their categories, and only matching department staff can be assigned.
- Residents can confirm staff-resolved complaints or reopen resolved/closed complaints with a reason; the assigned team can then continue the work.
- Complaint timelines record submission, assignment, work-status changes, resident confirmation, and reopen reasons. History is available in a compact expandable column. Each role only sees the complaints already available to that role.
- Residents can book seeded society facilities (Gym, Clubhouse, Swimming Pool, Community Hall, and Tennis Court); overlapping active bookings are prevented, adjacent time slots are allowed, and residents can cancel their own bookings. Administrators can view and cancel bookings.
- Administrators can create one monthly bill per flat with itemized charges and a due date. Residents see bills for their registered flat, balance and payment history, and can record simulated partial or full payments. Administrators see billed, collected, and outstanding totals. Simulated payments do not transfer money.
- Resident apartment selection uses one block from A-Z and one apartment number from 1-8 (for example `A-1`). Apartment assignment is fixed at registration; contact the administrator to correct a registered flat.
- All signed-in users can view their own profile in read-only mode and choose **Edit profile** to update their name, phone, apartment/flat number, email, and emergency contact.
- Residents can attach one JPEG or PNG photo (up to 5 MB) to a complaint; authorized users can view its thumbnail.
- Spring Data JPA persistence, with H2 for an easy local demo and PostgreSQL/Supabase available through environment variables.

Resident accounts and profiles are stored in the configured database. Admin and department-specific maintenance-head demo accounts are created on startup from environment-configurable credentials. Public sign-up always creates a resident account; it cannot grant administrator or maintenance-head privileges. Passwords are BCrypt-hashed. Each signed-in role can view and update only its own profile. A maintenance head can manage only staff added under their username and work on complaints belonging to their department. Complaint photos are stored in the local `uploads/` folder (configurable with `UPLOADS_DIRECTORY`); for a multi-instance deployment, use shared object storage. Payment entries in this version are simulation records only; there is no payment provider integration. QR passes, notifications, and analytics are outside this version.

## Run locally

The default configuration uses a local file-backed H2 database in `data/`, so resident accounts and profiles remain available after restarting the app:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Open `http://localhost:8080`.

| Role | Username | Password |
|---|---|---|
| Administrator | `admin` | `admin123` |
| Electrical head | `electrical` | `electrical123` |
| Plumbing head | `plumbing` | `plumbing123` |
| Water head | `water` | `water12345` |
| Cleaning head | `cleaning` | `cleaning123` |
| Lift head | `lift` | `lift12345` |
| Security head | `security` | `security123` |
| General maintenance head | `general` | `general123` |

Create a resident account from the **Create an account** link on the sign-in page. Override demo account passwords using `DEMO_ADMIN_PASSWORD`, `DEMO_ELECTRICAL_PASSWORD`, `DEMO_PLUMBING_PASSWORD`, `DEMO_WATER_PASSWORD`, `DEMO_CLEANING_PASSWORD`, `DEMO_LIFT_PASSWORD`, `DEMO_SECURITY_PASSWORD`, and `DEMO_GENERAL_PASSWORD` before starting the application. The older `maintenance` / `maintenance123` demo login remains available for compatibility.

## Use Supabase PostgreSQL

Set the connection values in the PowerShell window used to run the application. Use the JDBC URL, database username, and database password from your Supabase project's database connection settings; require SSL and do not commit credentials:

```powershell
$env:SUPABASE_DB_URL = "jdbc:postgresql://<host>:5432/postgres?sslmode=require"
$env:SUPABASE_DB_USERNAME = "<database-username>"
$env:SUPABASE_DB_PASSWORD = "<database-password>"
.\mvnw.cmd spring-boot:run
```

The schema is managed by Hibernate with `spring.jpa.hibernate.ddl-auto=update`. For a deployed or shared environment, replace the demo accounts and use a deliberate schema-migration strategy.

The next billing enhancement could integrate a real payment provider. That should be done only after choosing a provider and adding verified payment webhooks, idempotency, and reconciliation; the current payment entry is explicitly a local demo record and does not transfer money.
