using NUnit.Framework;

namespace Showcase.Mobium.NUnit;

[Category("home")]
public sealed class HomeTests : MobiumTest
{
    [Test, Category("smoke")]
    public void Session_is_on_an_Android_emulator()
    {
        var facts = Scenarios.SessionIsOnAnEmulator(Device);

        Assert.Multiple(() =>
        {
            Assert.That(facts.Platform, Is.EqualTo("android"));
            Assert.That(facts.EmulatorListed, Is.True, "no Android emulator in mobium devices");
            Assert.That(facts.AppInstalled, Is.True, "ApiDemos is not installed");
        });
    }

    [TestCase("Animation")]
    [TestCase("App")]
    [TestCase("Content")]
    [Category("smoke")]
    public void Map_lists_a_home_category_with_refs(string category)
    {
        var elements = Scenarios.MapHome(Device);

        Assert.That(elements.Select(e => e.Label), Has.Some.StartsWith(category));
        Assert.That(elements.Select(e => e.Ref), Is.All.Match("^@e[0-9]+$"));
    }

    [Test]
    public void Tap_scrolls_to_off_screen_rows()
    {
        var edit = Scenarios.TapThroughToControls(Device);

        Assert.That(edit.Ref, Does.StartWith("@e"));
    }

    static IEnumerable<TestCaseData> Categories() =>
        ScenarioData.Categories.Select(c => new TestCaseData(c.Category, c.Row).SetArgDisplayNames(c.Category, c.Row));

    [TestCaseSource(nameof(Categories))]
    public void Category_opens_its_list(string category, string row)
    {
        var element = Scenarios.OpenCategory(Device, category, row);

        Assert.That(element.Label, Is.EqualTo(row));
    }
}
