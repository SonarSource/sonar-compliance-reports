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

Preferred release process:

1. Merge the PR and wait for the default-branch `Build` workflow to finish promoting successfully.
2. Run `.github/workflows/release-operator.yml` from **Run workflow**.
3. Leave both inputs empty to release the latest promoted default-branch commit with both visibilities.

`Release Operator` resolves the promoted commit/build itself, reads `compliance-reports/gradle.properties` from that commit,
derives the release version, and creates the required release tags automatically.

Workflow inputs:

- `commit_sha` is optional. When omitted, the operator selects the latest successfully promoted commit reachable from the default branch.
When provided, it must be a promoted commit that is reachable from the default branch.
- `visibility` defaults to `both`. `both` runs the private release first and the public release second for the same promoted commit/build.
`private` or `public` limit the workflow to that visibility only.

Skip and rerun behavior:

- If the requested visibility already has a successful prior release, the operator skips it.
- If the GitHub release record already exists but no successful prior release was recorded, the operator fails fast and reports the
`version` and `releaseId` to use with `.github/workflows/release.yml`.
- If `visibility=both` and the private release succeeds while the public release fails, the workflow stops after the public failure.
Rerun `.github/workflows/release.yml` for the public tag only.

`.github/workflows/release.yml` remains the rerun path for kept releases and still supports the existing
`release: published` trigger for manually created GitHub releases.
