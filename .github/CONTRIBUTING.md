# Contributing to LiveStreamBlock

Thank you for your interest in contributing to **LiveStreamBlock**! We welcome contributions, bug reports, and suggestions from the community.

---

## 🛠️ Development Setup

### Prerequisites

- **Java Development Kit (JDK) 17** or higher
- **Git**

### Building the Project

Clone the repository and build using Gradle:

```bash
git clone https://github.com/PeterGamez/LiveStreamBlock.git
cd LiveStreamBlock

# Build the project (both :main and :plugin modules)
./gradlew build

# Run unit tests
./gradlew test
```

The compiled plugin jar will be generated in:
```
build/libs/livestreamblock-plugin-<version>.jar
```

---

## 📦 Project Structure

- **`:main`**: Core stream classification and detection engine (`LiveStreamDetector`, `DetectionResult`, `LiveStreamBlockConfig`, `LiveStreamBlockStats`). Independent of Lavalink server classes.
- **`:plugin`**: Lavalink v4 plugin wrapper (`LiveStreamBlockResponseBodyAdvice`, `LiveStreamBlockPlayerEventHandler`, `LiveStreamBlockRestHandler`, Spring Boot configuration).

---

## 🚀 Submitting Contributions

1. **Fork the repository** on GitHub.
2. **Create a branch** for your feature or bug fix:
   ```bash
   git checkout -b feature/my-cool-feature
   # or
   git checkout -b fix/issue-description
   ```
3. **Write clean and readable code**:
   - Follow standard Java code style conventions.
   - Maintain fast, non-blocking evaluation in detection algorithms to keep node latency low.
4. **Add or update tests**:
   - Ensure all existing unit tests pass (`./gradlew test`).
   - Add new tests covering edge cases, regex patterns, or new streaming sources.
5. **Update documentation**:
   - Update `README.md` if your change adds new configuration properties or changes behavior.
6. **Submit a Pull Request**:
   - Fill out the Pull Request template completely.
   - Reference any relevant issues (e.g., `Fixes #123`).

---

## 🐛 Reporting Bugs

If you find a bug, please open an issue using the [Bug Report template](https://github.com/PeterGamez/LiveStreamBlock/issues/new?template=bug_report.yml). Provide as much detail as possible, including:
- Steps to reproduce
- Lavalink version and Java version
- Relevant log output and `application.yml` configuration (with sensitive credentials redacted)

---

## 💡 Suggesting Features

We welcome new feature ideas! Please submit an issue using the [Feature Request template](https://github.com/PeterGamez/LiveStreamBlock/issues/new?template=feature_request.yml) describing the motivation and proposed behavior.
