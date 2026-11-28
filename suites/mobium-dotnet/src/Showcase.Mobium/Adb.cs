using System.Diagnostics;

namespace Showcase.Mobium;

/// <summary>
/// adb for the two things Mobium leaves to the platform here: opening an
/// activity by its class name, and listing installed packages in a sanity check.
/// </summary>
public static class Adb
{
    public static string Run(params string[] args)
    {
        var info = new ProcessStartInfo("adb")
        {
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
        };
        var serial = Environment.GetEnvironmentVariable("ANDROID_SERIAL");
        if (!string.IsNullOrEmpty(serial))
        {
            info.ArgumentList.Add("-s");
            info.ArgumentList.Add(serial);
        }
        foreach (var a in args) info.ArgumentList.Add(a);

        using var process = Process.Start(info) ?? throw new InvalidOperationException("could not start adb");
        var stdout = process.StandardOutput.ReadToEndAsync();
        var stderr = process.StandardError.ReadToEndAsync();
        if (!process.WaitForExit(TimeSpan.FromSeconds(60)))
        {
            process.Kill(entireProcessTree: true);
            throw new TimeoutException($"adb {string.Join(' ', args)} did not finish in 60 s");
        }
        if (process.ExitCode != 0)
            throw new InvalidOperationException($"adb {string.Join(' ', args)} exited {process.ExitCode}: {stderr.Result}");
        return stdout.Result;
    }
}
