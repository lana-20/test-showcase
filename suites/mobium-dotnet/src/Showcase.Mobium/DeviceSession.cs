using Mobium;

namespace Showcase.Mobium;

/// <summary>
/// The device fixture helper: one Mobium session on the Android emulator for
/// a whole test assembly, with ApiDemos installed once and launched fresh for
/// every test. Each runner owns its lifetime in its own way (NUnit
/// [OneTimeSetUp], an xUnit collection fixture, MSTest [AssemblyInitialize]);
/// this class is what they all hold.
/// </summary>
/// <remarks>
/// Every device call goes through <see cref="Step{T}"/>, so it shows up as an
/// Allure step named after the Mobium method, as in the Python suite.
/// </remarks>
public sealed class DeviceSession : IDisposable
{
    public const string Package = "io.appium.android.apis";
    public const string HomeReady = "text=\"Animation\"";

    public Device Device { get; }
    public string ApkPath { get; }

    DeviceSession(Device device, string apkPath)
    {
        Device = device;
        ApkPath = apkPath;
    }

    /// <summary>Starts the session, installs ApiDemos and returns once both are done.</summary>
    public static DeviceSession Open()
    {
        var apk = Path.GetFullPath(Environment.GetEnvironmentVariable("APP_PATH") ?? "apps/ApiDemos-debug.apk");
        if (!File.Exists(apk))
            throw new FileNotFoundException($"ApiDemos APK not found at {apk}; set APP_PATH", apk);

        var builder = Device.Builder()
            .Platform("android")
            // Well above the longest Until.Timeout used here; a call that runs
            // out ends the connection, so it is a backstop, not a wait.
            .CallTimeout(TimeSpan.FromMinutes(5));
        var serial = Environment.GetEnvironmentVariable("ANDROID_SERIAL");
        if (!string.IsNullOrEmpty(serial)) builder.OnDevice(serial);

        var device = builder.Start();
        try
        {
            device.Install(apk);
        }
        catch
        {
            device.Quit();
            throw;
        }
        return new DeviceSession(device, apk);
    }

    /// <summary>ApiDemos, stopped and launched again, on its home list.</summary>
    public HomeScreen LaunchFresh()
    {
        Step("Terminate", () => Device.Terminate(Package), Package);
        Step("Launch", () => Device.Launch(Package), Package);
        WaitFor(HomeReady);
        return new HomeScreen(this);
    }

    /// <summary>Opens one ApiDemos activity directly, then waits for it to show <paramref name="ready"/>.</summary>
    public void OpenActivity(string activity, string ready) =>
        Steps.Run($"Open {activity}", () =>
        {
            Adb.Run("shell", "am", "start", "-W", "-n", $"{Package}/{activity}");
            WaitFor(ready);
        });

    // -- Mobium calls, each as an Allure step ------------------------------

    public Element? WaitFor(string target) => Step("WaitFor", () => Device.WaitFor(target), target);

    public Element? WaitFor(string target, Until until, string condition) =>
        Step("WaitFor", () => Device.WaitFor(target, until), target, condition);

    public void Tap(string target) => Step("Tap", () => Device.Tap(target), target);

    public void Fill(string target, string text) => Step("Fill", () => Device.Fill(target, text), target, text);

    public void Set(string target) => Step("Set", () => Device.Set(target), target);

    public void Unset(string target) => Step("Unset", () => Device.Unset(target), target);

    public IList<Element> Map() => Step("Map", () => Device.Map());

    public IDictionary<string, object?> MapDiff() => Step("MapDiff", () => Device.MapDiff());

    public string Text() => Step("Text", () => Device.Text());

    public byte[] Screenshot() => Step("Screenshot", () => Device.Screenshot());

    public IList<DeviceInfo> Devices() => Step("Devices", () => Device.Devices());

    static void Step(string method, Action call, params string[] args) =>
        Steps.Run(Describe(method, args), call);

    static T Step<T>(string method, Func<T> call, params string[] args) =>
        Steps.Run(Describe(method, args), call);

    static string Describe(string method, string[] args) =>
        $"mobium {method}({string.Join(", ", args.Select(a => "\"" + a + "\""))})";

    /// <summary>
    /// Attaches a screenshot and the screen map to the current test. Called by
    /// each runner's failure hook; never throws, because it runs while a test
    /// is already failing.
    /// </summary>
    public void AttachFailureEvidence()
    {
        try
        {
            Steps.Attach("screenshot on failure", "image/png", Device.Screenshot(), ".png");
            var map = string.Join("\n", Device.Map().Select(e => e.ToString() + "  " + e.Locator));
            Steps.Attach("screen map on failure", "text/plain", System.Text.Encoding.UTF8.GetBytes(map), ".txt");
        }
        catch (Exception e)
        {
            Steps.Attach("evidence unavailable", "text/plain", System.Text.Encoding.UTF8.GetBytes(e.ToString()), ".txt");
        }
    }

    public void Dispose() => Device.Quit();
}
