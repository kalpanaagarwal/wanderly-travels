# Wanderly Travels

A desktop Travel & Tourism Management System for Indian (mainly Rajasthan)
tourism, built with **Java Swing** (GUI), **MySQL** (database) and **JDBC**
(to connect them). Built as a B.Tech CSE (IoT) college mini project.

---

## 1. What this project is

A single-window desktop application with 7 pages (Home, Destinations, Tour
Packages, Booking, Login, Registration, Contact), a MySQL-backed user system
with unique usernames, and a booking system where every booking is tied to
the logged-in user's account.

**Important note about the images:** the destination photos in
`src/images/` are clean, stylized graphics (gradient skies + monument
silhouettes + name banners) that were generated for this project because
the build environment had no internet access to download real photographs.
They are real local `.jpg` files, so the app works fully offline and never
shows a broken image icon - but if you want actual photographs for your
final submission, just replace any file (same file name, e.g.
`src/images/jaipur.jpg`) with a real photo of the same size/shape.

---

## 2. Requirements

- **JDK 17 or newer** (the code uses modern Java text blocks; any JDK 17-21
  works). Check with: `java -version` and `javac -version`
- **MySQL Server 8.x** (Community Edition is free)
- **VS Code** with the **"Extension Pack for Java"** (includes the Java
  language server, debugger and test runner) - install from the VS Code
  Extensions panel
- **MySQL Connector/J** driver `.jar` file (instructions below - this could
  not be included directly in the ZIP because it is a large binary file)

---

## 3. Step-by-step setup

### Step 1: Install and start MySQL

If you don't already have MySQL installed, download **MySQL Community
Server** from https://dev.mysql.com/downloads/mysql/ and install it. During
installation you will set a **root password** - remember it, you will need
it in Step 3.

Make sure the MySQL service is running (on Windows, check the "Services"
app for "MySQL80"; on macOS/Linux, `mysql.server start` or
`sudo systemctl start mysql`).

You do **not** need to create the `wanderly_db` database or any tables by
hand - the application does this automatically the first time it runs (see
`src/data/DBInitializer.java`). `sql/schema.sql` is included only as a
backup/reference in case you ever want to create everything manually.

### Step 2: Add the MySQL Connector/J driver

1. Download the **MySQL Connector/J** `.jar` file from:
   https://dev.mysql.com/downloads/connector/j/
   (choose "Platform Independent", download the `.zip` or `.tar.gz`, and
   extract it - inside you'll find a file named something like
   `mysql-connector-j-9.1.0.jar`)
2. Copy that `.jar` file into this project's `lib/` folder.
3. You can then delete `lib/PUT_MYSQL_CONNECTOR_JAR_HERE.txt`.

### Step 3: Configure your database username/password

Open `src/data/DBConfig.java` and edit these two lines to match your own
MySQL installation:

```java
public static final String USERNAME = "root";
public static final String PASSWORD = "your_mysql_password";
```

That is the **only file** you need to edit to get the app talking to your
database. Nothing else needs to change.

### Step 4: Open the project in VS Code

1. Open VS Code -> **File > Open Folder...** -> select the
   `Wanderly_Travels` folder.
2. Wait a moment for the "Language Support for Java" extension to detect
   the project (you'll see a progress notification in the bottom-right).
3. If VS Code doesn't automatically pick up the `lib/*.jar`, open
   `.vscode/settings.json` (already included) and confirm it lists
   `lib` as a referenced library - it does, by default.

### Step 5: Run the project

The easiest way:
1. Open `src/Main.java` in VS Code.
2. Click the **Run** button that appears above the `public static void main`
   line (or press `F5`).

If you prefer the terminal instead, from the project's root folder:

**Windows (PowerShell/cmd):**
```
javac -d bin -cp "lib/*" (Get-ChildItem -Recurse -Filter *.java src | % { $_.FullName })
java -cp "bin;lib/*;src" Main
```

**macOS / Linux:**
```
find src -name "*.java" > sources.txt
javac -d bin -cp "lib/*" @sources.txt
java -cp "bin:lib/*:src" Main
```

(The `src` folder is included on the runtime classpath so the app can find
the `images/` folder using simple relative paths like `images/jaipur.jpg`.)

The very first time you run it, the app will:
- Connect to your MySQL server
- Create the `wanderly_db` database if it doesn't exist
- Create all 5 tables if they don't exist
- Insert the starter destinations and tour packages if the tables are empty

If something is wrong with your MySQL connection, you'll see a clear error
dialog explaining what to check, instead of the app crashing silently.

---

## 4. Project structure

```
Wanderly_Travels/
├── src/
│   ├── Main.java              Entry point - starts the DB, then the GUI
│   ├── model/                 Plain data classes (User, Destination, ...)
│   ├── data/                  Everything related to MySQL/JDBC
│   │   ├── DBConfig.java          <- edit your username/password here
│   │   ├── DBConnection.java      Hands out JDBC connections
│   │   ├── DBInitializer.java     Creates DB/tables + seeds starter data
│   │   ├── UserDAO.java           Register / login / uniqueness check
│   │   ├── DestinationDAO.java
│   │   ├── PackageDAO.java
│   │   ├── BookingDAO.java
│   │   └── ContactDAO.java
│   ├── util/
│   │   ├── Session.java           Tracks who is currently logged in
│   │   └── PasswordUtil.java      SHA-256 password hashing
│   ├── ui/                    All Swing screens and reusable components
│   │   ├── MainFrame.java         The window + CardLayout page switching
│   │   ├── NavBar.java            Top navigation bar
│   │   ├── HomePanel.java
│   │   ├── DestinationsPanel.java
│   │   ├── PackagesPanel.java
│   │   ├── BookingPanel.java
│   │   ├── LoginPanel.java
│   │   ├── RegisterPanel.java
│   │   ├── ContactPanel.java
│   │   ├── DecorativeSidePanel.java  Gradient side panel for auth/contact
│   │   ├── CardFactory.java          Builds destination/package cards
│   │   ├── RoundedButton.java        Custom button (hover/press effects)
│   │   ├── RoundedPanel.java         Custom rounded "card" panel + shadow
│   │   ├── Logo.java                 Reusable logo + wordmark
│   │   ├── ImageUtil.java            Safe image loading/caching
│   │   └── Theme.java                Colours & fonts used everywhere
│   └── images/                Local destination photos + logo (offline-safe)
├── lib/                        Put mysql-connector-j-x.x.x.jar here
├── sql/
│   └── schema.sql              Reference copy of the database structure
├── bin/                        Compiled .class files go here (auto-created)
└── README.md
```

---

## 5. How to explain this project in your viva

**Why Java Swing?** It is Java's built-in GUI toolkit, works on any OS with
a JVM, and needs no external framework - a good fit for a desktop
application built with plain Java.

**Why MySQL?** A free, widely-used relational database that supports
constraints (like `UNIQUE` on username), foreign keys, and multi-user data
- exactly what a booking system with accounts needs.

**What does JDBC do?** JDBC (Java Database Connectivity) is the standard
Java API for sending SQL to a database and reading results back. Every
database call in this project goes through `DBConnection.java`, which
centralises how connections are created, and through DAO
("Data Access Object") classes like `UserDAO`, which centralise the SQL for
one table each.

**How does login work?** `LoginPanel` collects a username and password,
hashes the password with SHA-256 (`PasswordUtil`), and asks `UserDAO.login()`
to look up that username and compare the stored hash. If they match, the
returned `User` object is stored in `Session` (a simple static holder) for
the rest of the running application.

**How does registration work, and why is the username unique?** `RegisterPanel`
validates the form, then calls `UserDAO.register()`, which first checks
`usernameExists()` and, if free, inserts the new row. The `username` column
also has a **database-level `UNIQUE` constraint** (see `schema.sql` /
`DBInitializer.java`), so even if two people somehow tried to register the
same username at the exact same instant, MySQL itself would reject the
second insert (`SQLIntegrityConstraintViolationException`), which the code
catches and reports as "Username already exists."

**How are bookings stored per-user?** Every row in the `bookings` table has
a `user_id` foreign key. `BookingPanel` always reads
`Session.getCurrentUser().getUserId()` for the currently logged-in person
and passes it into every insert and every "my bookings" query
(`BookingDAO.getBookingsForUser(userId)`), so one user's bookings can never
appear for another user.

**How does Java communicate with MySQL?** Through the JDBC driver
(MySQL Connector/J). `DBConnection.getConnection()` opens a connection using
the URL/username/password in `DBConfig`; DAO classes then use
`PreparedStatement` objects (never string-concatenated SQL) to safely run
queries and updates.

**What OOP concepts are used?**
- **Encapsulation**: model classes (`User`, `Booking`, etc.) keep fields
  `private` with public getters/setters.
- **Classes & objects**: every screen, every table, every helper is its own
  class with a clear single responsibility.
- **Constructors**: used throughout to set up panels and objects.
- **Inheritance**: `RoundedButton extends JButton`, `RoundedPanel extends
  JPanel`, every screen `extends JPanel`.
- **Encapsulated static utility**: `Session`, `PasswordUtil`, `ImageUtil`,
  `CardFactory` group related static helper methods.
- **Event handling**: `ActionListener`/`MouseListener` lambdas respond to
  button clicks, hovering, and typing Enter in a password field.

**How is the project structured?** Into `model` (data), `data` (database
access), `util` (small shared helpers) and `ui` (everything visual) - a
simple layered structure that is easy to explain and easy to extend.

---

## 6. Troubleshooting

| Problem | Likely cause / fix |
|---|---|
| "Could not connect to MySQL" dialog on startup | MySQL service isn't running, or the username/password in `DBConfig.java` is wrong |
| `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | The connector `.jar` isn't in `lib/`, or it isn't on your run classpath |
| Images look like simple graphics, not real photos | Expected - see the note at the top of this README; replace the files in `src/images/` with real photos if you want |
| "Username already exists" when registering | Someone (maybe you, testing) already registered that username - pick another one |
| Blank/garbled Segoe UI font on Linux/macOS | Cosmetic only; Swing will substitute a similar system font automatically |
