## Summary

<!-- Briefly describe what this PR changes and why. -->

## Related Issue

<!-- Fixes #(issue) -->

## Checklist

- [ ] Built cleanly with `gradle :app:assembleDebug`
- [ ] All tests pass via `gradle :app:testDebugUnitTest --no-daemon`
- [ ] Zero background polling, wake locks, or continuous idle render loops introduced
- [ ] Animations evaluated on GPU (`graphicsLayer` / `offset`) and respect `reducedMotion`
- [ ] New UI controls include unique `Modifier.testTag(...)`, `>= 48.dp` touch targets, and `contentDescription`
- [ ] No `INTERNET` or broad storage permissions added; secrets/keystores remain untouched
