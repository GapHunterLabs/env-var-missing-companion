<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Env Var Missing Companion Changelog

## [Unreleased]

## [0.1.1]

### Added

- Review/star CTA: after 10 distinct real findings, a one-time
  notification asks whether to rate the plugin on Marketplace, with a
  permanent "Don't ask again" option. Standard mechanism used
  catalog-wide since 2026-08-24, rolled out
  to this plugin now.

## [0.1.0]

### Added

- **Inspection that cross-checks every real environment-variable access
  in the currently open file against the project's real
  `.env`/`.env.example`/etc. files**, warning on any variable that's
  used in code but never declared anywhere.
- **Patterns detected**: JavaScript/TypeScript
  (`process.env.VARNAME`, `process.env["VARNAME"]`,
  `process.env['VARNAME']`) and Python (`os.environ["VARNAME"]`,
  `os.environ['VARNAME']`, `os.environ.get("VARNAME")`,
  `os.getenv("VARNAME")`) -- real property/index access only, never a
  bare textual mention in a comment or string.
- **Real quick-fix**: "Add VARNAME to .env.example" appends
  `VARNAME=` to the project's real `.env.example`, or creates one from
  scratch (with a generated-by-plugin header comment) if the project
  has none at all.
- **Explicit-default awareness**: a variable with a fallback right in
  the code (`process.env.PORT || 3000`, `os.environ.get("DEBUG",
  "false")`, `os.getenv("TIMEOUT", 30)`) is flagged as a weak warning
  instead of a full warning -- still real, but less urgent since the
  code runs fine without it declared.
- **Scope, deliberate for v0.1**: the file currently open in the
  editor, not a whole-project scan on every keystroke; JavaScript/
  TypeScript and Python only (Java/Kotlin's dominant pattern is
  Spring `@Value`/`application.properties`, not direct
  `System.getenv`, so this specific pattern is far less common there).
- `.env`/`.env.example`/`.env.local`/etc. parsing adapted from
  `env-diff-companion`'s proven `EnvFileParser` (same catalog, same
  file format).
- 100% static text analysis of files already open in the project --
  no network call, no external process spawned.

[Unreleased]: https://github.com/GapHunterLabs/env-var-missing-companion/compare/0.1.1...HEAD
[0.1.1]: https://github.com/GapHunterLabs/env-var-missing-companion/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/GapHunterLabs/env-var-missing-companion/commits/0.1.0
