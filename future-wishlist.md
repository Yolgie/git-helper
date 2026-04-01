# Future Wishlist

## Packaging & Distribution

- Package GitHelper as a simple executable artifact so end users can run it without a local Kotlin toolchain.
- Evaluate producing a single runnable Java distribution (fat JAR) for easier cross-platform usage.
- Explore platform-specific distributions:
  - Linux-friendly release artifact (binary/package)
  - Windows-friendly release artifact (executable/package)
- Add a release workflow that builds and uploads these artifacts for tagged versions.

## Build & Dependency Management

- Move dependency and plugin version definitions into a Gradle Version Catalog (`gradle/libs.versions.toml`).
- Standardize plugin/dependency references to use version-catalog aliases for easier upgrades.
- Add dependency update automation (for example, scheduled checks) once versions are centralized.

## CI/CD Improvements

- Extend CI beyond tests to include formatting/linting and static analysis.
- Add a release pipeline that runs tests, builds distributables, and publishes artifacts on release tags.
