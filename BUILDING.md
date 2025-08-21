# Building SG_RLGL

This document provides detailed instructions for building the SG_RLGL plugin from source.

## Prerequisites

### Required Software

1. **Java Development Kit (JDK) 17 or higher**
   - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)
   - Verify installation: `java -version` and `javac -version`

2. **Apache Maven 3.6.0 or higher**
   - Download from [Maven website](https://maven.apache.org/download.cgi)
   - Verify installation: `mvn -version`

3. **Git** (for cloning the repository)
   - Download from [Git website](https://git-scm.com/)

### Environment Setup

Ensure your `JAVA_HOME` environment variable points to your JDK installation:

**Windows:**
```cmd
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
```

**macOS/Linux:**
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

## Building the Plugin

### 1. Clone the Repository

```bash
git clone <repository-url>
cd SG_RLGL
```

### 2. Build the Plugin

#### Standard Build
```bash
mvn clean package
```

This will:
- Clean any previous build artifacts
- Compile the Java source code
- Run tests (if any)
- Package the plugin into a JAR file
- Apply Maven Shade plugin to include dependencies

#### Build without Tests
```bash
mvn clean package -DskipTests
```

#### Build with Verbose Output
```bash
mvn clean package -X
```

### 3. Locate the Built JAR

After a successful build, you'll find the plugin JAR at:
```
target/SG_RLGL-1.1.0.jar
```

## Development Builds

### Compile Only (No Packaging)
```bash
mvn clean compile
```

### Install to Local Repository
```bash
mvn clean install
```

This installs the plugin to your local Maven repository (`~/.m2/repository/`).

### Generate Sources and Javadoc JARs
```bash
mvn clean package -Psources,javadoc
```

## IDE Integration

### IntelliJ IDEA

1. Open IntelliJ IDEA
2. Choose "Open" and select the project root directory
3. IntelliJ will automatically detect the Maven project
4. Wait for dependency resolution to complete
5. Build using: `Build` → `Build Project` (Ctrl+F9)

### Eclipse

1. Open Eclipse
2. Choose `File` → `Import` → `Existing Maven Projects`
3. Browse to the project root directory
4. Eclipse will import the project and resolve dependencies
5. Build using: `Project` → `Build Project`

### Visual Studio Code

1. Install the "Extension Pack for Java" extension
2. Open the project folder in VS Code
3. VS Code will automatically detect the Maven project
4. Use `Ctrl+Shift+P` → "Java: Build Projects" to build

## Build Profiles

The project includes several Maven profiles for different build scenarios:

### Development Profile
```bash
mvn clean package -Pdevelopment
```
- Includes debug information
- Skips optimization
- Faster build times

### Release Profile
```bash
mvn clean package -Prelease
```
- Optimized build
- Includes sources and javadoc
- Signs artifacts (if configured)

## Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=GameManagerTest
```

### Run Tests with Coverage
```bash
mvn clean test jacoco:report
```

Coverage reports will be generated in `target/site/jacoco/`.

## Troubleshooting

### Common Build Issues

#### 1. Java Version Mismatch
**Error:** `Unsupported class file major version`
**Solution:** Ensure you're using JDK 17 or higher

#### 2. Maven Not Found
**Error:** `mvn: command not found`
**Solution:** Install Maven and add it to your PATH

#### 3. Dependency Resolution Failures
**Error:** `Could not resolve dependencies`
**Solutions:**
- Check internet connection
- Clear Maven cache: `mvn dependency:purge-local-repository`
- Update Maven: `mvn -U clean package`

#### 4. Out of Memory Errors
**Error:** `java.lang.OutOfMemoryError`
**Solution:** Increase Maven memory:
```bash
export MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m"
```

#### 5. Paper API Not Found
**Error:** `Could not find artifact io.papermc.paper:paper-api`
**Solution:** The Paper repository should resolve automatically. If not, check your internet connection and try:
```bash
mvn clean package -U
```

### Build Verification

After building, verify the JAR file:

```bash
# Check JAR contents
jar -tf target/SG_RLGL-1.1.0.jar

# Verify plugin.yml is included
jar -tf target/SG_RLGL-1.1.0.jar | grep plugin.yml

# Check file size (should be reasonable, not too small/large)
ls -lh target/SG_RLGL-1.1.0.jar
```

### Performance Tips

1. **Use Maven Daemon** (if available):
   ```bash
   mvnd clean package
   ```

2. **Parallel Builds**:
   ```bash
   mvn clean package -T 4
   ```

3. **Offline Mode** (after dependencies are cached):
   ```bash
   mvn clean package -o
   ```

## Continuous Integration

For CI/CD pipelines, use:

```bash
# CI-friendly build
mvn clean verify -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn

# With test reports
mvn clean verify -B surefire-report:report
```

## Advanced Build Options

### Custom Properties
```bash
mvn clean package -Dpaper.version=1.21.1-R0.1-SNAPSHOT
```

### Skip Specific Plugins
```bash
mvn clean package -Dmaven.javadoc.skip=true -Dmaven.source.skip=true
```

### Debug Maven Execution
```bash
mvn clean package -X -e
```

This provides detailed debug output and full stack traces for any errors.