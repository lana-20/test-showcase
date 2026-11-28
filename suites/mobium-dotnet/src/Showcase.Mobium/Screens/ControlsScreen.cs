using Mobium;

namespace Showcase.Mobium;

/// <summary>Views → Controls → 1. Light Theme (.view.Controls1).</summary>
public sealed class ControlsScreen(DeviceSession session)
{
    public const string Activity = ".view.Controls1";
    public const string Edit = "testid=edit";
    public const string Checkbox = "testid=check1";
    public const string Toggle = "testid=toggle1";

    public static ControlsScreen Open(DeviceSession session, string ready = Edit)
    {
        session.OpenActivity(Activity, ready);
        return new ControlsScreen(session);
    }

    public Element Fill(string text)
    {
        session.Fill(Edit, text);
        return session.WaitFor(Edit, Until.Value(text), $"value \"{text}\"")!;
    }

    public Element Check()
    {
        session.Set(Checkbox);
        return session.WaitFor(Checkbox, Until.Checked(), "checked")!;
    }

    public Element Uncheck()
    {
        session.Unset(Checkbox);
        return session.WaitFor(Checkbox, Until.Unchecked(), "unchecked")!;
    }

    public void TapToggle()
    {
        session.Tap(Toggle);
        session.WaitFor(Toggle, Until.Checked(), "checked");
    }
}
