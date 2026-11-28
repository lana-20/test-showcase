using Microsoft.VisualStudio.TestTools.UnitTesting;

// One emulator and one Mobium session: no parallel test methods or classes.
[assembly: DoNotParallelize]

namespace Showcase.Mobium.MSTest;

/// <summary>
/// MSTest: [AssemblyInitialize] runs once before the first test in the
/// assembly and [AssemblyCleanup] once after the last. Both must be static and
/// live in a [TestClass].
/// </summary>
[TestClass]
public sealed class AssemblySetup
{
    public static DeviceSession Session { get; private set; } = null!;

    [AssemblyInitialize]
    public static void StartSession(TestContext context) => Session = DeviceSession.Open();

    [AssemblyCleanup]
    public static void EndSession() => Session?.Dispose();
}
