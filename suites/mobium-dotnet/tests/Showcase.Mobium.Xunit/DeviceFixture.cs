using Xunit;

// xUnit runs test collections in parallel by default, and by default every
// test class is its own collection. There is one emulator and one Mobium
// session, and two tests acting on one screen at once would fight over it,
// so parallelism is off for the assembly (and in xunit.runner.json), and every
// class joins the one "Device" collection below as well.
[assembly: CollectionBehavior(DisableTestParallelization = true)]

namespace Showcase.Mobium.Xunit;

/// <summary>
/// xUnit has no setup methods: shared state is a fixture object, and
/// IAsyncLifetime gives it an async start and end. As a collection fixture it
/// is created once for every class in the collection, not once per class.
/// </summary>
public sealed class DeviceFixture : IAsyncLifetime
{
    public DeviceSession Session { get; private set; } = null!;

    public Task InitializeAsync() => Task.Run(() => Session = DeviceSession.Open());

    public Task DisposeAsync()
    {
        Session?.Dispose();
        return Task.CompletedTask;
    }
}

[CollectionDefinition(Name)]
public sealed class DeviceCollection : ICollectionFixture<DeviceFixture>
{
    public const string Name = "Device";
}

/// <summary>
/// Base class. xUnit creates a new instance for every test and hands it the
/// fixture through the constructor. xUnit v2 does not tell a test how it ended,
/// so the evidence is attached by <see cref="Run"/>, which wraps the test body.
/// </summary>
[Collection(DeviceCollection.Name)]
public abstract class MobiumTest(DeviceFixture fixture)
{
    protected DeviceSession Device => fixture.Session;

    protected void Run(Action test)
    {
        try
        {
            test();
        }
        catch
        {
            Device.AttachFailureEvidence();
            throw;
        }
    }
}
