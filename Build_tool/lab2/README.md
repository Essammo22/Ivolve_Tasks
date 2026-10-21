# Lab 2: Building and Packaging a Java Application with Gradle

A simple command-line calculator built and packaged with Gradle. The lab covers the full workflow: installing Gradle, running unit tests, building the artifact, and verifying the app works.

> **Source:** the application code is from [Ibrahim-Adel15/CalculatorGradleApp](https://github.com/Ibrahim-Adel15/CalculatorGradleApp). This lab focuses on building, testing, and packaging it.

## Environment

| Tool   | Version                  |
|--------|--------------------------|
| Java   | OpenJDK 17.0.20.1        |
| Gradle | 8.14.3                   |
| OS     | Ubuntu 22.04 (Linux)     |

## Steps

### 1. Install Gradle

The version available in Ubuntu's package repository (4.4.1) is too old to work with Java 17, so Gradle 8.14.3 was installed manually from the official distribution into `/opt/gradle-8.14.3`.

```bash
wget https://services.gradle.org/distributions/gradle-8.14.3-bin.zip
sudo unzip gradle-8.14.3-bin.zip -d /opt
export PATH=/opt/gradle-8.14.3/bin:$PATH
```

Verify the installation:

```bash
gradle -version
```

### 2. Clone the source code

```bash
git clone https://github.com/Ibrahim-Adel15/CalculatorGradleApp.git
cd CalculatorGradleApp
```

### 3. Run the unit tests

```bash
gradle test
```

```
BUILD SUCCESSFUL in 12s
3 actionable tasks: 3 executed
```

### 4. Build the application

```bash
gradle build
```

```
BUILD SUCCESSFUL in 1s
7 actionable tasks: 4 executed, 3 up-to-date
```

The artifact is generated at `build/libs/calculator.jar`.

### 5. Run the application

```bash
java -jar build/libs/calculator.jar
```

### 6. Verify the app is working

Example run with the inputs `6` and `8`:

```
Enter first number: 6
Enter second number: 8
a = 6.0, b = 8.0
Sum: 14.0
Subtract: -2.0
Multiply: 48.0
Divide: 0.75
```

All four operations return the expected results, so the app works correctly.
