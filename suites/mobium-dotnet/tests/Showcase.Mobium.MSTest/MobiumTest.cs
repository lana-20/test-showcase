using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace Showcase.Mobium.MSTest;

/// <summary>
/// Base class. MSTest injects TestContext through this property before each
/// test. <see cref="Run"/> reports the test to Allure and attaches a
/// screenshot and the screen map if it fails.
/// </summary>
public abstract class MobiumTest
{
    public TestContext TestContext { get; set; } = null!;

    protected static DeviceSession Device => AssemblySetup.Session;

    protected void Run(Action test, params object?[] arguments) =>
        AllureMSTest.Run(TestContext, GetType(), test, Device.AttachFailureEvidence, arguments);
}
