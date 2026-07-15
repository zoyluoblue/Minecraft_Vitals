# Releasing Vitals

## Preconditions

1. Update `mod_version` in `gradle.properties`.
2. Move completed entries from `Unreleased` in `CHANGELOG.md` into the target version.
3. Complete every applicable item in `docs/testing/TEST_PLAN.md`.
4. Verify Chinese and English UI, multiplayer behavior, occlusion, shortcut behavior, and the high-density performance fixture.

## Release gate

~~~bash
./gradlew clean build --no-daemon --stacktrace
~~~

Confirm that `build/libs/` contains exactly one remapped main JAR and one sources JAR, and inspect the actual main JAR:

~~~bash
unzip -t build/libs/vitals-fabric-1.21.3-loader0.18.4-<version>.jar
unzip -p build/libs/vitals-fabric-1.21.3-loader0.18.4-<version>.jar fabric.mod.json
shasum -a 256 build/libs/vitals-fabric-1.21.3-loader0.18.4-<version>.jar
~~~

## Publish

Create and push a SemVer tag matching `mod_version`:

~~~bash
git tag v<version>
git push origin v<version>
~~~

GitHub Actions rebuilds the project, validates the tag/version relationship, verifies the JAR contents, generates SHA-256 checksums, and creates the GitHub Release.

## Rollback

Vitals has no world state or network protocol. Roll back by removing the client JAR or reinstalling an earlier version. Preserve `config/vitals.json` when downgrading unless the release notes say its Schema changed.
