# Lab 1: Building and Packaging a Java Application with Maven

A simple command-line calculator built and packaged with Apache Maven. The lab covers the full workflow: setting up the environment, running unit tests, building the artifact, and verifying the app works.

> **Source:** the application code is from [Ibrahim-Adel15/CalculatorMavenApp](https://github.com/Ibrahim-Adel15/CalculatorMavenApp). This lab focuses on building, testing, and packaging it.

## Environment

| Tool   | Version                  |
|--------|--------------------------|
| Java   | OpenJDK 17.0.20.1        |
| Maven  | 3.6.3                    |
| OS     | Ubuntu 22.04 (Linux)     |

## Steps

### 1. Install Java and Maven

```bash
sudo apt update
sudo apt install openjdk-17-jdk maven
```

Verify the installation:

```bash
java -version
mvn -version
```

```
openjdk version "17.0.20.1" 2026-08-18
Apache Maven 3.6.3
Java version: 17.0.20.1, vendor: Ubuntu, runtime: /usr/lib/jvm/java-17-openjdk-amd64
```

### 2. Clone the source code

```bash
git clone https://github.com/Ibrahim-Adel15/CalculatorMavenApp.git
cd CalculatorMavenApp
```

### 3. Run the unit tests

```bash
mvn test
```

```
[INFO] Running com.example.calculator.CalculatorTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

All 5 tests pass.

### 4. Build the application

```bash
mvn package
```

```
[INFO] --- maven-jar-plugin:3.4.1:jar (default-jar) @ calculator ---
[INFO] BUILD SUCCESS
```

The artifact is generated at `target/calculator.jar`.

### 5. Run the application

```bash
java -jar target/calculator.jar
```

### 6. Verify the app is working

Example run with the inputs `3` and `7`:

```
Enter first number: 3
Enter second number: 7
a = 3.0, b = 7.0
Sum: 10.0
Subtract: -4.0
Multiply: 21.0
Divide: 0.42857142857142855
```

All four operations return the expected results, so the app works correctly.

## Project Structure

```
CalculatorMavenApp
├── pom.xml
├── src
│   ├── main/java/com/example/calculator
│   │   ├── Calculator.java
│   │   └── Main.java
│   └── test/java/com/example/calculator
│       └── CalculatorTest.java
└── target
    └── calculator.jar
```
