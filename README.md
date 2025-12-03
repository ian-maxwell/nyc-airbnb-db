# COMP 3380 – Airbnb Database Project (University of Manitoba)

Java console application that queries the shared `cs3380` database on `uranium.cs.umanitoba.ca`.

## How to run locally

1. Copy `auth.cfg.example` → `auth.cfg`
2. Put your real CS username and password in `auth.cfg`
3. Run:
```bash
javac -cp "mssql-jdbc-13.2.1.jre11.jar" *.java
java -cp ".;mssql-jdbc-13.2.1.jre11.jar" SQLServerMain