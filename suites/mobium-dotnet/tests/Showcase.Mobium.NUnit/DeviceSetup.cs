using NUnit.Framework;

namespace Showcase.Mobium.NUnit;

/// <summary>
/// NUnit: a [SetUpFixture] in the test namespace runs its [OneTimeSetUp] once
/// before any fixture in that namespace, and its [OneTimeTearDown] after the last.
/// One device, one session, for the whole run.
/// </summary>
[SetUpFixture]
public sealed class DeviceSetup
{
    public static DeviceSession Session { get; private set; } = null!;

    [OneTimeSetUp]
    public void StartSession() => Session = DeviceSession.Open();

    [OneTimeTearDown]
    public void EndSession() => Session?.Dispose();
}
