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
- suggested release tag
- Artifactory build URL

Preferred release process:

1. Merge the PR and wait for the default-branch `Build` workflow to finish promoting successfully.
2. Run `.github/workflows/release-operator.yml` from **Run workflow**.

`Release Operator` finds the promoted build of the default-branch head, reads `compliance-reports/gradle.properties` from that
commit, derives the release version (for example `2.0.0.383`) and starts `.github/workflows/release.yml` with it. The release
workflow, which uses `SonarSource/gh-action_release` v7, creates a draft GitHub release for that version, publishes the
artifacts and publishes the release (and its tag) once every step succeeded.

Workflow inputs:

- `commit_sha` is optional and, when given, must be the head of the default branch: the release workflow runs on it, so it
has to be promoted. An older promoted commit cannot be released.
- `dryRun` runs the release workflow without publishing anything.

Skip and rerun behavior:

- If a published release already exists for the version, the operator skips it.
- If the release workflow fails, the GitHub release stays a draft. Fix the cause and use **Re-run jobs** on the failed
`Release` workflow run: the build is not rebuilt and the draft is reused. To abandon it, run
`gh release delete <version> --cleanup-tag --yes`.
- If the `Release` run ends red but the GitHub release is already published, Maven Central rejected the bundle after the release
was promoted: do not re-run it. Synchronize with `scripts/manual-maven-central-sync.sh <project> <build number>` from
[gh-action_release](https://github.com/SonarSource/gh-action_release).

`.github/workflows/release.yml` can also be started directly from **Run workflow** with the full `version` of a promoted
build (and `dryRun`). Publishing a GitHub release by hand no longer starts a release.
