# Mobium · C# (.NET): one suite, three runners

The same Mobium device tests, written once in C# and run under the three main .NET test runners:
**NUnit**, **xUnit** and **MSTest**. They drive Appium's ApiDemos app on an Android 15 emulator through
[Mobium's .NET client](https://github.com/mobiumdev/mobium/tree/v0.1.0-rc1/clients/dotnet).

```
src/Showcase.Mobium/          shared: device session helper, screen objects, scenarios, test data
tests/Showcase.Mobium.NUnit/  NUnit 4  + Allure.NUnit
tests/Showcase.Mobium.Xunit/  xUnit v2 + Allure.Xunit
tests/Showcase.Mobium.MSTest/ MSTest 4 + a small adapter on Allure.Net.Commons (see below)
merge_results.py              merges the three Allure result sets, labeled by runner
fetch-client.sh               checks out the Mobium .NET client source at the pinned tag
```

The scenarios in `Scenarios.cs` are plain methods. Each one drives the app and returns what it saw. Each test
project asserts on that in its own runner's style, so the runner is the only difference between the three.
Every device call is an Allure step, and nothing sleeps: Mobium re-resolves and scrolls to each target, and
every check is a `WaitFor` with an `Until` condition.

## The scenarios (15 test cases per runner)

| Scenario | Cases |
|---|---|
| The session is on an Android emulator, with ApiDemos installed | 1 |
| The home map lists a category, and every element has an `@e` ref | 3 (inline data) |
| A tap scrolls to rows below the fold (Views → Controls → Light Theme) | 1 |
| A category opens its list | 3 (shared data source) |
| Fill replaces a field's value instead of appending | 2 (inline data) |
| Set and unset a checkbox, confirmed by `Until.Checked` / `Until.Unchecked` | 1 |
| `MapDiff` reports what a tap on a toggle changed | 1 |
| An alert dialog is read and answered | 1 |
| A missing element is refused with `NoSuchElementException` | 1 |
| A screenshot is a PNG (and is attached) | 1 |

On failure, each runner attaches a screenshot and the screen map (`Map()`) to the Allure test.

The session declares one dialog rule (`AddDialogRule("responding", "Wait")`). CI emulators sometimes raise a
system "isn't responding" dialog over the app, and Mobium's actions and waits answer it where it appears,
instead of the test failing or being retried.

## How the runners compare

| | NUnit | xUnit | MSTest |
|---|---|---|---|
| **Run-wide setup** | `[SetUpFixture]` with `[OneTimeSetUp]` / `[OneTimeTearDown]` | `ICollectionFixture<DeviceFixture>`; the fixture is `IAsyncLifetime` | `[AssemblyInitialize]` / `[AssemblyCleanup]` (static), plus `[ClassInitialize]` per class |
| **Per-test state** | One fixture instance for the class; `[SetUp]` / `[TearDown]` | A new class instance per test: constructor and `Dispose` | A new instance per test; `[TestInitialize]` / `[TestCleanup]` and an injected `TestContext` |
| **Data-driven tests** | `[TestCase(...)]`, `[TestCaseSource]` with `TestCaseData` | `[Theory]` with `[InlineData]`, `[MemberData]` | `[DataRow(...)]`, `[DynamicData]` |
| **Categories** | `[Category("smoke")]` | `[Trait("Category", "smoke")]` | `[TestCategory("smoke")]` |
| **Filter example** | `--filter TestCategory=smoke` | `--filter Category=smoke` | `--filter TestCategory=smoke` |
| **Parallelism (default)** | Serial unless you opt in with `[Parallelizable]`; here `[NonParallelizable]` says so | Test collections run in parallel, and each class is its own collection | Serial unless you opt in with `[Parallelize]`; here `[assembly: DoNotParallelize]` |
| **Assertions** | `Assert.That(actual, Is.EqualTo(x))`, constraints such as `Has.Some`, `Is.All.Match`, `Assert.Multiple` | `Assert.Equal`, `Assert.Contains(collection, predicate)`, `Assert.All`, `Assert.Throws<T>` | `Assert.AreEqual`, `StringAssert`, `CollectionAssert`, `Assert.ThrowsExactly<T>` |
| **Knows the outcome in teardown** | Yes: `TestContext.CurrentContext.Result` | No (v2), so the test body is wrapped to attach evidence | Yes: `TestContext.CurrentTestOutcome`, but the Allure wrapper already sees the exception |
| **Allure** | Allure.NUnit (`[AllureNUnit]`) | Allure.Xunit (reporter forced in `xunit.runsettings`) | No adapter exists; `AllureMSTest.cs` writes results through Allure.Net.Commons |

**Why parallelism is off.** There is one emulator and one Mobium session, and two tests acting on one screen at
once would tap over each other. xUnit is the runner where this matters: by default it runs test collections in
parallel, and every class is its own collection. So every xUnit class joins one `[Collection("Device")]`,
which also lets them share a single `DeviceFixture`. On top of that, parallelization is turned off for the
assembly and in `xunit.runner.json`, so a new class that forgets the attribute still cannot run alongside
another. NUnit and MSTest are serial by default; the attributes make it explicit.

## Reporting

Each runner writes its own Allure results. `merge_results.py` copies them into one set and sets `parentSuite`
to the runner, so the report's **Suites** tab reads *runner → test class → test*. It also adds the runner to
each test's history id, so the same test under two runners is two tests with their own history. Every run
also writes a TRX file per runner (`out/trx/mobium-dotnet/`), uploaded with the CI artifact. The status JSON
has a `runners` object with the counts per runner.

**MSTest and Allure.** allure-csharp 2.15 has adapters for NUnit, xUnit, Reqnroll and SpecFlow, but not MSTest.
Instead of converting TRX after the run, the MSTest project has a short adapter (`AllureMSTest.cs`) on
Allure.Net.Commons, the library the other two adapters are built on. It gives MSTest the same steps, attachments,
parameters and tags (from `[TestCategory]`) as the other two runners.

**Versions.** .NET 10 (the current LTS), NUnit 4.6, xUnit 2.9 with its 2.8 VSTest adapter, MSTest 4.5,
allure-csharp 2.15. xUnit stays on v2 because Allure.Xunit is a v2 runner reporter, and the 3.x VSTest adapter
does not load it.

## Mobium's .NET client

The client is not on NuGet yet. `fetch-client.sh` clones `mobiumdev/mobium` at `v0.1.0-rc1`, the tag of the
pinned binary, checks that the tag points to the expected commit, and the solution builds the client as a
project reference. It is the client's real API: `Device.Builder().Platform("android").Start()`, `Install`,
`Launch`, `Terminate`, `Map`, `MapDiff`, `Tap`, `Fill`, `Set`/`Unset`, `WaitFor` with `Until`, `Text`,
`Screenshot`, `Devices`, and the typed `MobiumException` subclasses. The one thing it leaves to the platform
here is opening an activity by class name, which uses `adb shell am start`, as the Python suite does.

## Run it locally

Needs the .NET 10 SDK, an Android emulator or device (`adb devices`), and Mobium:

```sh
ci/android/install-mobium.sh                 # pinned v0.1.0-rc1 release, checksum-verified (Linux)
ci/run-mobium-dotnet.sh                      # fetches ApiDemos and the client, builds, runs all three
```

One runner, or one category, by hand:

```sh
suites/mobium-dotnet/fetch-client.sh
export APP_PATH=$PWD/apps/ApiDemos-debug.apk  # from ci/android/fetch-apk.sh
cd suites/mobium-dotnet
dotnet test tests/Showcase.Mobium.NUnit --filter TestCategory=smoke
dotnet test tests/Showcase.Mobium.Xunit --filter Category=smoke
dotnet test tests/Showcase.Mobium.MSTest --logger "trx;LogFileName=mstest.trx"
```

View the merged report: `npx allure-commandline@2.46.2 serve out/allure-results/mobium-dotnet`.
