using Allure.NUnit;
using NUnit.Framework;
using NUnit.Framework.Interfaces;

namespace Showcase.Mobium.NUnit;

/// <summary>
/// Base fixture. NUnit tells [TearDown] how the test ended
/// (TestContext.CurrentContext.Result), so failure evidence is attached there.
/// </summary>
[AllureNUnit]
[NonParallelizable] // one emulator, one session: tests must take turns
public abstract class MobiumTest
{
    protected static DeviceSession Device => DeviceSetup.Session;

    [TearDown]
    public void AttachEvidenceOnFailure()
    {
        if (TestContext.CurrentContext.Result.Outcome.Status == TestStatus.Failed)
            Device.AttachFailureEvidence();
    }
}
