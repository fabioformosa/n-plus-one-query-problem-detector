# Project Help and Guidance

This file is intended to provide help and guidance on how to develop and maintain the project.

# Release Process
To release a new version of the project, follow these steps:
1. Update the `CHANGELOG.md` file with the changes included in the new release.
2. Update manually the version number in the `VERSION` file.
3. Apply a git tag in the format `vX.Y.Z`, where `X`, `Y`, and `Z` are the major, minor, and patch version numbers respectively.
4. Create manually a new release on GitHub using the created tag.
5. Publishing the new draft GitHub release will trigger a GitHub Actions workflow that will automatically build and publish the release artifacts into Maven Central Repository.
6. Update manually the new snaphot version number in the `VERSION` file.
7. Add a new entry in the `CHANGELOG.md` file for the next version.
8. (In the future) a snapshot release may be automatically published to a snapshot repository.

# Changelog Conventions
The `CHANGELOG.md` file follows the "Keep a Changelog" format. Each entry should include:
- A version header (e.g., `## 1.0.0 - 2024-01-01`)
- A list of changes categorized by type: 
  - `NEW FEATURE` for new features
  - `BREAKING CHANGE` for changes in existing functionality
  - `DEPRECATION` for soon-to-be removed features
  - `FIX` for bug fixes
  - `SECURITY` for security-related changes



