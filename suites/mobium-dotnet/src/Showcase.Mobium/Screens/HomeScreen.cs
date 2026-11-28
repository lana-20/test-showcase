using Mobium;

namespace Showcase.Mobium;

/// <summary>ApiDemos' home list and the category lists under it.</summary>
public sealed class HomeScreen(DeviceSession session)
{
    public static string Row(string label) => $"text=\"{label}\"";

    public IList<Element> Map() => session.Map();

    /// <summary>Taps a row on the current list. Mobium scrolls to it first if it is below the fold.</summary>
    public HomeScreen Open(string label)
    {
        session.Tap(Row(label));
        return this;
    }

    public Element Expect(string label) =>
        session.WaitFor(Row(label)) ?? throw new InvalidOperationException($"no element for {label}");
}
