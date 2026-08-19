# Env Var Missing Companion

IntelliJ-family plugin. Cross-checks every real environment-variable
access in your open file (`process.env.PORT`,
`process.env["PORT"]`, `os.environ["DEBUG"]`,
`os.environ.get("DEBUG")`, `os.getenv("TIMEOUT")`) against your
project's real `.env`/`.env.example`/etc. files, and warns on any
variable that's **used in code but never declared anywhere** — with a
real quick-fix to add it. 100% static text analysis of files already
open in your project: no network call, no external process spawned.

## Why it exists

An original idea, not a port of an existing competitor — validated
against `CONSTITUTION.md` §1's "Plan B permanente" discipline before
being built: (1) confirmed no plugin in this catalog or in JetBrains
Marketplace does exactly this cross-check (searches for "env var
missing" surface AI-driven secrets-management tools or run-config
drift detectors, none of which cross-reference actual `process.env`/
`os.environ` accesses in source against `.env*` file contents); (2)
confirmed buildable in the ~10-day budget with techniques this catalog
already has proven — `.env` file parsing adapted from
`env-diff-companion`'s already-shipped `EnvFileParser`, plain-text/
regex scanning of source (same principle as
`circular-dependency-companion`'s Gradle DSL parser and
`dockerfile-layer-size-companion`'s Dockerfile parser), and a
`LocalInspectionTool` + `LocalQuickFix` — the platform's standard
warning-plus-fix mechanism, new to this catalog but a well-documented,
stable API surface (confirmed via `javap` against the pinned platform
jar, not guessed). Complements, doesn't duplicate, `env-diff-companion`
(which compares two `.env` files against *each other*, never against
code). Same "apuesta consciente sin ancla de mercado" treatment as
Refactor Simulator / Test Scaffold Companion / Circular Dependency
Companion / Unused npm Script Companion: v0.1 ships free, no time/
marketing investment disproportionate to real demand signal until
there's evidence of adoption.

## What it detects (v0.1 scope, stated honestly)

**JavaScript/TypeScript:**
- `process.env.VARNAME`
- `process.env["VARNAME"]` / `process.env['VARNAME']`

**Python:**
- `os.environ["VARNAME"]` / `os.environ['VARNAME']`
- `os.environ.get("VARNAME")` (with or without a default argument)
- `os.getenv("VARNAME")` (with or without a default argument)

**Why JS/TS + Python, and not Java/Kotlin in v0.1:** these are the two
ecosystems where reading an environment variable by string key
(`process.env.X`/`os.environ[...]`) is the dominant, idiomatic
pattern. Java/Kotlin's dominant pattern for configuration is Spring's
`@Value("${...}")`/`application.properties` injection (or similar
DI-container config binding), not direct `System.getenv(...)` calls —
so this specific code-vs-`.env`-file cross-check is a comparatively
rare pattern in that ecosystem. Deferred to a possible future version,
not silently ruled out forever.

**Real access only, never a bare mention.** A comment or string that
merely *mentions* "process.env" or "os.environ" in prose (`// Config
values come from process.env at runtime`) produces no warning — every
pattern above requires the real receiver syntax (`.NAME`,
`["NAME"]`, or a quoted first call argument) immediately present, not
just the words appearing somewhere in the line. **Known, documented
limitation:** because detection is plain-text regex (not a real
per-language lexer — see "Why built this way" below), a comment or
string literal whose text happens to *contain* the exact working
syntax (e.g. a code example quoted inside a source file's block
comment reading `// e.g. process.env.PORT`) is indistinguishable from
a real access and will still be flagged. In practice this is rare —
most such examples live in actual `README.md` files, which this
inspection does not scan as source at all.

## Where it looks for `.env` files

The project's content roots, plus the currently open file's own parent
directory (so a monorepo subproject with its own `.env` next to a
nested `package.json` is still picked up even without being a separate
content root). Every `.env`/`.env.example`/`.env.local`/`.env.production`
(any `.env.*` suffix) file found contributes its declared variable
names to one combined set — v0.1 does not distinguish *which* specific
`.env.*` file declares which variable (that's the multi-environment
"which vars are required in `.env.dev` vs. `.env.prod`" feature staged
for a possible future Pro tier, not built here).

## Explicit defaults: still flagged, but less urgently

A variable read with a fallback value right in the same expression —
`process.env.PORT || 3000`, `process.env.PORT ?? 3000`,
`os.environ.get("DEBUG", "false")`, `os.getenv("TIMEOUT", 30)` — is
**less urgent** than one with no fallback: the code still runs
correctly even if nobody ever sets it. Rather than suppress the
finding entirely (silently hiding a real gap felt worse than a softer
signal), it's downgraded to a **weak warning** instead of a full
warning, and still gets the same real quick-fix — declaring it
explicitly in `.env.example` is still good practice even with a
default, since it documents the variable exists at all.

## The quick-fix

**"Add VARNAME to .env.example"** appends `VARNAME=` to your project's
real `.env.example` file. If your project already has one (anywhere
under a content root or next to the open file), the fix appends to
*that* file — it never creates a second, competing `.env.example`
elsewhere. If your project has **no** `.env.example` at all, the fix
creates one from scratch, next to the file you fixed, with a header
comment noting it was generated by the plugin.

## Honest handling of no `.env` files at all

A project with zero `.env*` files anywhere still runs the inspection —
every real reference is reported as undeclared (correctly: there's
nothing declaring it), never a crash and never a silently-skipped
check just because there was nothing to compare against.

## Why built this way

- **Plain-text/regex detection, not JS/Python PSI.** Python PSI needs
  the separate PyCharm/Python bundled plugin (not present in every
  IntelliJ IDEA edition this catalog targets), and JavaScript/
  TypeScript PSI is an Ultimate-only bundled plugin — neither is
  guaranteed available. Same reasoning already documented in
  `http-status-inline-companion`'s `build.gradle.kts` for its own
  Java/Kotlin-only PSI choice, applied here to the inverse case (no
  PSI dependency at all, so the inspection works in any edition,
  against any file type).
- **`.env` file parsing adapted from `env-diff-companion`.** Same
  `KEY=VALUE`/comment/`export`-prefix rules, same catalog, same file
  format — no reason to reinvent it.
- **The file currently open, not the whole project.** A whole-project
  scan cheap enough to run inline on every inspection pass would need
  either a persistent index (real infrastructure, out of v0.1 scope)
  or re-scanning every file in the project on every keystroke — the
  exact cost this design avoids, same "heavy computation off the hot
  path" principle already applied catalog-wide (`CONSTITUTION.md` §6),
  just applied at the scope-selection level here instead of threading.
  "The file already open" is also the real, common use case: a
  developer adding a new `process.env.X` reference wants to know right
  there whether it's declared.
- **`LocalInspectionTool.checkFile`, not `buildVisitor`.** Detection is
  a whole-document regex scan, not a PSI-node-by-PSI-node walk of a
  specific language grammar, so the whole-file entry point is the
  right fit — and it's what lets the inspection apply to any file type
  without a `language` filter in `plugin.xml`.
- **Leaf PSI anchoring for each `ProblemDescriptor`.** A `ProblemDescriptor`/
  `LineMarkerInfo` anchored on a composite PSI node (instead of a real
  leaf token) is a documented platform gotcha (`SDK_GOTCHAS.md` §20) —
  this inspection always walks down to a true leaf element before
  creating a descriptor, using the platform's own relative-`TextRange`
  overload (confirmed via `javap` against the pinned platform jar) to
  point at the exact variable name text within that leaf, not the
  leaf's entire range.

## v0.1 scope

Free, all of it — no paywall, nothing held back for a future tier.
Deferred to a possible future v0.2 Pro tier (not started, not
promised): multi-environment support (rules for which variables are
required in `.env.dev` vs. `.env.staging` vs. `.env.prod`), and
Secrets Manager/Vault integration to validate a variable's existence
without ever exposing its real value.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us
at **gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
