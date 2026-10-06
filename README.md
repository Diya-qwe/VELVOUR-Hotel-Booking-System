# VELVOUR Hotel Management (JavaFX)

Login: `admin / admin123` (also `manager / manager123`, `reception / reception123`)

## Requirements
- JDK 17 or newer (21 is fine)
- Maven 3.8+ (or let VS Code's Java extensions handle it)
- VS Code extension: "Extension Pack for Java"

## Run in VS Code
1. File > Open Folder > the `velvour` folder (the one containing `pom.xml`).
2. Wait for Java to finish importing the Maven project (bottom-right status).
3. Open `src/main/java/com/velvour/Launcher.java` and click **Run** above `main`
   (or press F5 and pick "Velvour").

## Run from a terminal
    mvn javafx:run

## Run the tests
    mvn test
