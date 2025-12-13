# 🗽 NYC Airbnb Database Project

This project features a persistent, full-stack application for querying and managing a **MSSQL\*\*** database containing New York City Airbnb data. The system is split into a Java-based backend and a Streamlit-based web frontend.

---

## 💻 Live Web Console Access

A secure, persistent version of the application console is hosted live on an AWS EC2 instance. Use the password "Guest".

- **Live Console URL:** **http://35.182.117.19:8501/**

### 🔒 Security and Access (Guest Mode)

The web console provides two tiers of access, demonstrating proper security logic:

- The system actively **blocks** destructive commands (`DROP`, `DELETE`, `R`epopulate, etc.) when running in Guest Mode.

| Role      | Password               | Permissions                                        |
| :-------- | :--------------------- | :------------------------------------------------- |
| **Guest** | `guest`                | Read-only access to all queries and quick actions. |
| **Admin** | _[Secret Placeholder]_ | Full access (for internal use/debugging).          |

---

## 🛠 Project Components

### 1. Java Backend (`SQLServerMain.java`)

- **Role:** Handles direct connections to the database (either shared U of M server or a locally hosted instance), executes custom SQL logic, and manages command-line parsing and paginated output.

### 2. Streamlit Frontend (`airbnb_dashboard_live.py`)

- **Role:** Provides the secure, compact web interface. It handles user authentication, manages the persistent Java process, and controls the UI (quick action buttons, pagination, and session log display).

---

## 🚀 How to Run Locally

You can run the Java console application or the Streamlit web application.

### A. Run Java Console Application (Direct)

1.  Copy `auth.cfg.example` → `auth.cfg`
2.  Put your real CS username and password in `auth.cfg`
3.  **Compile:**
    ```bash
    javac -cp "mssql-jdbc-13.2.1.jre11.jar" *.java
    ```
4.  **Run:**
    ```bash
    java -cp ".;mssql-jdbc-13.2.1.jre11.jar" SQLServerMain
    ```

### B. Run Streamlit Web Console (Requires Python/Streamlit)

1.  Ensure Python libraries are installed: `pip install streamlit`
2.  Run the secured dashboard file:
    ```bash
    streamlit run airbnb_dashboard_live.py
    ```
