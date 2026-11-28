using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace Showcase.Mobium.MSTest;

[TestClass]
[TestCategory("home")]
public sealed class HomeTests : MobiumTest
{
    /// <summary>[ClassInitialize] runs once before this class's first test; here it checks the shared session is up.</summary>
    [ClassInitialize]
    public static void SessionIsUp(TestContext context) =>
        Assert.IsNotNull(AssemblySetup.Session, "the device session did not start");

    [TestMethod, TestCategory("smoke")]
    public void Session_is_on_an_Android_emulator() => Run(() =>
    {
        var facts = Scenarios.SessionIsOnAnEmulator(Device);

        Assert.AreEqual("android", facts.Platform);
        Assert.IsTrue(facts.EmulatorListed, "no Android emulator in mobium devices");
        Assert.IsTrue(facts.AppInstalled, "ApiDemos is not installed");
    });

    [TestMethod, TestCategory("smoke")]
    [DataRow("Animation")]
    [DataRow("App")]
    [DataRow("Content")]
    public void Map_lists_a_home_category_with_refs(string category) => Run(() =>
    {
        var elements = Scenarios.MapHome(Device);

        Assert.IsTrue(elements.Any(e => e.Label.StartsWith(category, StringComparison.Ordinal)),
            $"{category} not in {string.Join(", ", elements.Select(e => e.Label))}");
        foreach (var e in elements) StringAssert.Matches(e.Ref, new System.Text.RegularExpressions.Regex("^@e[0-9]+$"));
    }, category);

    [TestMethod]
    public void Tap_scrolls_to_off_screen_rows() => Run(() =>
    {
        var edit = Scenarios.TapThroughToControls(Device);

        StringAssert.StartsWith(edit.Ref, "@e");
    });

    public static IEnumerable<object[]> Categories => ScenarioData.CategoryRows;

    [TestMethod]
    [DynamicData(nameof(Categories))]
    public void Category_opens_its_list(string category, string row) => Run(() =>
    {
        var element = Scenarios.OpenCategory(Device, category, row);

        Assert.AreEqual(row, element.Label);
    }, category, row);
}
