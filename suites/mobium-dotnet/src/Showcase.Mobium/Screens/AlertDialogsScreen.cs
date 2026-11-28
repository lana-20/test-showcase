using Mobium;

namespace Showcase.Mobium;

/// <summary>App → Alert Dialogs (.app.AlertDialogSamples).</summary>
public sealed class AlertDialogsScreen(DeviceSession session)
{
    public const string Activity = ".app.AlertDialogSamples";
    public const string TwoButtons = "testid=two_buttons";
    const string Ok = "text=\"OK\"";

    public static AlertDialogsScreen Open(DeviceSession session)
    {
        session.OpenActivity(Activity, TwoButtons);
        return new AlertDialogsScreen(session);
    }

    /// <summary>Opens the OK/Cancel dialog and returns all the text on screen while it shows.</summary>
    public string OpenOkCancelDialog()
    {
        session.Tap(TwoButtons);
        session.WaitFor(Ok);
        return session.Text();
    }

    /// <summary>Presses OK and waits for the dialog to go and the list to be back.</summary>
    public Element AnswerOk()
    {
        session.Tap(Ok);
        session.WaitFor(Ok, Until.Hidden(), "hidden");
        return session.WaitFor(TwoButtons)!;
    }
}
