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

The post-merge `Build` workflow writes release information in the `Promote` job summary for default-branch pushes.
That summary includes:

- commit SHA
- promoted build number
- derived release version
- suggested public tag
- suggested private tag
- Artifactory build URL

Manual release process:

1. Merge the PR and wait for the default-branch `Build` workflow to finish successfully.
2. Open the `Promote` job summary and copy the suggested release version or tag for that merged commit.
3. Create a tag like `2.0.0.383` for a public release, or `private-2.0.0.383` for an Artifactory-only release.
4. Publish the GitHub release for that tag.

Creating the GitHub release triggers `.github/workflows/release.yml`.
That workflow now validates that the tag build number matches the promoted build recorded for the tagged commit 
before running the shared release steps.

If the shared release workflow keeps the GitHub release after a failure, rerun `.github/workflows/release.yml`
from **Run workflow** with the `version` and `releaseId` values reported by the failed run.
