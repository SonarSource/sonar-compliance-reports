# sonar-compliance-reports

Holds the `compliance-reports` Java library.

## Layout

- `compliance-reports/`: published library module
- `build.gradle`, `settings.gradle`, `gradle/`: shared Gradle build, publishing, signing, and dependency catalog configuration
- `Makefile`: single entrypoint for local development and CI tasks

## Local development

Run the library test suite and generate Jacoco coverage:

```bash
make test
```

## Publishing and releases

Published artifact:

- `org.sonarsource.compliancereports:compliance-reports`

The module version is defined in `compliance-reports/gradle.properties` as `x.y.z-SNAPSHOT`.
At CI time, the build number is appended, producing a version such as `2.0.0.383`.

Creating a GitHub release triggers `.github/workflows/release.yml`:

1. Create a tag like `2.0.0.383` for a public release.
2. Create a tag like `private-2.0.0.383` for an Artifactory-only release.
3. Publish the GitHub release for that tag.

If the shared release workflow keeps the GitHub release after a failure, rerun `.github/workflows/release.yml`
from **Run workflow** with the `version` and `releaseId` values reported by the failed run.
