using System.Collections;
using Mobium;

namespace Showcase.Mobium;

/// <summary>
/// The test scenarios, as plain methods. Each one drives the app and returns
/// what it saw; the test projects decide how to assert on it, in their own
/// runner's style. Waiting is Mobium's: every action re-resolves its target
/// and every check is a WaitFor, so nothing here sleeps.
/// </summary>
public static class Scenarios
{
    public sealed record SessionFacts(string Platform, bool EmulatorListed, bool AppInstalled);

    public sealed record CheckboxStates(bool? AfterSet, bool? AfterUnset);

    public sealed record DialogOutcome(string TextWhileOpen, Element ListAfterwards);

    /// <summary>One entry of map_diff's "changed" list: the element after the change, and what changed.</summary>
    public sealed record Change(string Locator, string Label, string What)
    {
        public bool IsTheToggle => Locator.Contains("toggle1") || Label is "ON" or "OFF";
    }

    /// <summary>The session is on an Android emulator, and ApiDemos is installed on it.</summary>
    public static SessionFacts SessionIsOnAnEmulator(DeviceSession s) => new(
        s.Device.Session?.Platform ?? "",
        s.Devices().Any(d => d.Platform == "android" && d.Emulator),
        Adb.Run("shell", "pm", "list", "packages", DeviceSession.Package).Contains(DeviceSession.Package));

    /// <summary>Maps the home screen of a freshly launched app.</summary>
    public static IList<Element> MapHome(DeviceSession s) => s.LaunchFresh().Map();

    /// <summary>
    /// Views → Controls → 1. Light Theme. "Views" is below the fold, so the
    /// first tap only works because Mobium scrolls to it. Returns the text field.
    /// </summary>
    public static Element TapThroughToControls(DeviceSession s)
    {
        var home = s.LaunchFresh();
        home.Open("Views").Open("Controls").Open("1. Light Theme");
        return s.WaitFor(ControlsScreen.Edit)!;
    }

    /// <summary>Opens a home category and returns one row expected in its list.</summary>
    public static Element OpenCategory(DeviceSession s, string category, string expectedRow) =>
        s.LaunchFresh().Open(category).Expect(expectedRow);

    /// <summary>Fills the field twice and returns it after the second fill: fill replaces, it does not append.</summary>
    public static Element FillReplacesValue(DeviceSession s, string text)
    {
        s.LaunchFresh();
        var controls = ControlsScreen.Open(s);
        controls.Fill("placeholder text");
        return controls.Fill(text);
    }

    public static CheckboxStates SetAndUnsetCheckbox(DeviceSession s)
    {
        s.LaunchFresh();
        var controls = ControlsScreen.Open(s, ControlsScreen.Checkbox);
        var set = controls.Check();
        var unset = controls.Uncheck();
        return new CheckboxStates(set.Checked, unset.Checked);
    }

    /// <summary>Maps, taps the toggle, then returns what map_diff says changed.</summary>
    public static IList<Change> MapDiffAfterToggle(DeviceSession s)
    {
        s.LaunchFresh();
        ControlsScreen.Open(s, ControlsScreen.Toggle);
        s.Map();
        new ControlsScreen(s).TapToggle();
        var diff = s.MapDiff();
        var changed = new List<Change>();
        if (diff.TryGetValue("changed", out var raw) && raw is IEnumerable entries)
        {
            foreach (var entry in entries.OfType<IDictionary<string, object?>>())
            {
                var after = entry.TryGetValue("after", out var a) ? a as IDictionary<string, object?> : null;
                var locator = after != null && after.TryGetValue("locator", out var l) && l is IDictionary<string, object?> loc
                    ? $"{Get(loc, "kind")}={Get(loc, "value")}"
                    : "";
                var label = after == null ? "" : Get(after, "label");
                var what = entry.TryGetValue("what", out var w) && w is IEnumerable list && w is not string
                    ? string.Join(",", list.Cast<object?>())
                    : w?.ToString() ?? "";
                changed.Add(new Change(locator, label, what));
            }
        }
        return changed;
    }

    public static DialogOutcome AnswerAlertDialog(DeviceSession s)
    {
        s.LaunchFresh();
        var dialogs = AlertDialogsScreen.Open(s);
        var text = dialogs.OpenOkCancelDialog();
        return new DialogOutcome(text, dialogs.AnswerOk());
    }

    /// <summary>Taps something that is not there. Mobium refuses with NoSuchElementException.</summary>
    public static void TapMissingElement(DeviceSession s)
    {
        s.LaunchFresh();
        s.Tap("testid=noSuchButton");
    }

    public static byte[] Screenshot(DeviceSession s)
    {
        s.LaunchFresh();
        var png = s.Screenshot();
        Steps.Attach("home screen", "image/png", png, ".png");
        return png;
    }

    static string Get(IDictionary<string, object?> map, string key) =>
        map.TryGetValue(key, out var value) ? value?.ToString() ?? "" : "";

    public static readonly byte[] PngSignature = [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A];
}
