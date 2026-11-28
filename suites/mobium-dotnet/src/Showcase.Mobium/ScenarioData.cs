namespace Showcase.Mobium;

/// <summary>
/// Data for the data-driven tests, in one place. The NUnit [TestCaseSource],
/// xUnit [MemberData] and MSTest [DynamicData] tests all read these, and the
/// inline cases ([TestCase], [InlineData], [DataRow]) repeat the same values.
/// </summary>
public static class ScenarioData
{
    /// <summary>A home category and a row that must be in its list.</summary>
    public static readonly (string Category, string Row)[] Categories =
    [
        ("App", "Activity"),
        ("Graphics", "Arcs"),
        ("Views", "Buttons"),
    ];

    public static IEnumerable<object[]> CategoryRows => Categories.Select(c => new object[] { c.Category, c.Row });
}
