# CI/CD Pipeline Setup for AionOS

This document explains the CI/CD workflows set up for the AionOS Android project.

## Overview

Three automated workflows have been configured to streamline development, testing, and release processes:

### 1. **Build Workflow** (`build.yml`)
**Triggers:** Push to `main`/`develop` branches, Pull Requests

**What it does:**
- Compiles the Android app in debug mode
- Runs on every push and pull request
- Uploads build artifacts (APK, intermediate files)
- Verifies the codebase compiles correctly

**Key benefits:**
- Catches compilation errors early
- Ensures PRs don't break the build
- Provides artifacts for testing

---

### 2. **Test Workflow** (`test.yml`)
**Triggers:** Push to `main`/`develop` branches, Pull Requests

**What it does:**
- Runs unit tests (`testDebugUnitTest`)
- Runs Android Lint checks for code quality
- Publishes test results to GitHub
- Uploads test reports as artifacts

**Key benefits:**
- Catches bugs before merge
- Enforces code quality standards
- Provides visibility into test coverage
- Lint warnings help prevent common issues

---

### 3. **Release Workflow** (`release.yml`)
**Triggers:** Git tags starting with `v*`, Manual trigger (`workflow_dispatch`)

**What it does:**
- Builds optimized release APK (with ProGuard/R8)
- Builds Android App Bundle (AAB) for Google Play Store
- Uploads both artifact types
- Creates GitHub releases with built artifacts
- Marks alpha/beta versions as pre-releases

**Key benefits:**
- Automated, consistent release builds
- Ready-to-distribute artifacts
- Trackable release history on GitHub
- Supports multiple distribution channels

---

## How to Use

### Automatic Triggers
All workflows run automatically based on their triggers. No manual action needed for build and test workflows.

### Create a Release
To create a release, push a git tag:

```bash
# Create a version tag
git tag v0.1.0

# Push the tag to GitHub
git push origin v0.1.0
```

**Tag naming:**
- `v0.1.0` - Regular release
- `v0.1.0-alpha` - Alpha pre-release
- `v0.1.0-beta` - Beta pre-release

The workflow will automatically:
1. Build release APK and AAB
2. Create a GitHub Release
3. Attach the built files to the release

### Manual Release Build
Click "Run workflow" on the Release workflow in the Actions tab to trigger a manual release build.

---

## Configuration

### Environment
- **Java Version:** 17 (Temurin JDK)
- **Gradle:** Uses local `gradlew` wrapper
- **Android SDK:** As specified in `build.gradle.kts`

### Caching
- Gradle dependencies are cached to speed up builds
- Cache is automatically managed by GitHub Actions

### Artifacts
- **Build outputs:** Retained for 7 days
- **Release artifacts:** Retained for 30 days

---

## Monitoring

### View Workflow Runs
1. Go to your repository
2. Click "Actions" tab
3. Click on a workflow to see run history
4. Click on a run to see detailed logs

### Test Results
- Published as checks on PRs and commits
- Click "Details" to view test reports
- Detailed reports available in run artifacts

### Build Failures
If a workflow fails:
1. Click on the failed run
2. Expand the failed job to see error logs
3. Common issues:
   - Missing JDK: Workflow handles this automatically
   - Gradle cache issues: Workflows auto-clear if needed
   - Test failures: Check test output in logs

---

## Next Steps

### Optional Enhancements
- **Code Coverage:** Add Codecov or Jacoco reports
- **Dependency Updates:** Add Dependabot for automatic updates
- **Android Emulator Tests:** Add emulator-based instrumentation tests
- **Signing:** Configure signing keys for release builds
- **Play Store Upload:** Add automatic upload to Google Play Store
- **Slack Notifications:** Notify team on build failures
- **SAST/Security:** Add security scanning (e.g., Snyk, Semgrep)

### To Add Signing for Release Builds
1. Generate a signing key
2. Encode it as base64
3. Add as a GitHub secret: `RELEASE_KEYSTORE`
4. Update `release.yml` to use the key for signing

---

## Troubleshooting

### Workflow won't start
- Check branch name matches trigger (main/develop)
- Ensure commit message doesn't have `[skip ci]`

### Build fails with Gradle errors
- Check `android/build.gradle.kts` syntax
- Verify dependencies are available in configured repositories
- Check Java version compatibility

### Tests fail
- Run locally: `cd android && ./gradlew testDebugUnitTest`
- Check test logs in workflow run
- Ensure test dependencies are up to date

---

For more information, see [GitHub Actions Documentation](https://docs.github.com/en/actions).
