# Project Instructions

## Mandatory Verification

After any opencode-made code change in this repository, run SonarLint before final response:

```powershell
.\gradlew.bat sonarlintMain sonarlintTest
```

If SonarLint reports issues caused by the current changes, fix them and rerun the command until it passes. If the command cannot be run, state the blocker clearly in the final response.

For Java changes, also run the most focused relevant tests before the final response.
