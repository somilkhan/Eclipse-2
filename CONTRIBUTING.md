# Contributing to Eclipse 2

Thanks for helping. Eclipse 2 is maintained at
[somilkhan/Eclipse-2](https://github.com/somilkhan/Eclipse-2) as the continuation of the
project created by [ReSo7200](https://github.com/ReSo7200).

## Reporting bugs

1. Search [existing issues](https://github.com/somilkhan/Eclipse-2/issues) first.
2. Use the bug report template and include:
   - Eclipse 2 version and Instagram version (Instagram build matters: hooks are found per build).
   - Root (LSPosed) or no-root (LSPatch), Android version and device.
   - The log: companion app -> Logs -> **Save to storage**, then attach the `.txt` file. It already
     contains a header with app, Instagram and Android versions. Remove anything private first.

## Proposing features

Open a feature request. Features that need device fingerprint spoofing, "anti-ban" evasion, or
automating actions at scale will not be accepted. Bulk tools must keep safe pacing on by default.

## Development setup

- JDK 17, Android SDK (compileSdk 34), no extra tooling.
- Build and test:

  ```sh
  ./gradlew assembleDebug testDebugUnitTest
  ```

- Install the debug APK, enable it in LSPosed for Instagram (or patch with LSPatch), force-stop
  Instagram and reopen it.
- Hooks locate Instagram classes with DexKit at runtime. Prefer string/structure matchers over
  obfuscated class names, and log a clear "not found" line instead of crashing when a match fails.

## Pull requests

- Branch from `main`, keep each PR focused on one change.
- Put pure logic in small classes without Android/Xposed dependencies and cover it with JUnit
  tests under `app/src/test`. CI runs `testDebugUnitTest` and `assembleDebug` on every PR.
- Keep user-facing strings in `res/values/strings*.xml`; translations are welcome in the
  matching `values-xx` folders.
- Describe what you tested and on which Instagram version.
- Do not bump `versionCode`/`versionName` or edit `CHANGELOG.md` release headings; maintainers
  do that when releasing (see [RELEASING.md](RELEASING.md)).

## License

Eclipse 2 is licensed under the GNU General Public License v3.0 (see [LICENSE](LICENSE) and
[NOTICE](NOTICE)). By opening a pull request you agree that your contribution is released under
GPL-3.0-only. Do not copy in code that has an incompatible license. If you adapt code from another
project, say where it came from in the pull request and keep its copyright header.
