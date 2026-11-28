using Xunit;

namespace Showcase.Mobium.Xunit;

[Trait("Category", "home")]
public sealed class HomeTests(DeviceFixture fixture) : MobiumTest(fixture)
{
    [Fact, Trait("Category", "smoke")]
    public void Session_is_on_an_Android_emulator() => Run(() =>
    {
        var facts = Scenarios.SessionIsOnAnEmulator(Device);

        Assert.Equal("android", facts.Platform);
        Assert.True(facts.EmulatorListed, "no Android emulator in mobium devices");
        Assert.True(facts.AppInstalled, "ApiDemos is not installed");
    });

    [Theory, Trait("Category", "smoke")]
    [InlineData("Animation")]
    [InlineData("App")]
    [InlineData("Content")]
    public void Map_lists_a_home_category_with_refs(string category) => Run(() =>
    {
        var elements = Scenarios.MapHome(Device);

        Assert.Contains(elements, e => e.Label.StartsWith(category, StringComparison.Ordinal));
        Assert.All(elements, e => Assert.Matches("^@e[0-9]+$", e.Ref));
    });

    [Fact]
    public void Tap_scrolls_to_off_screen_rows() => Run(() =>
    {
        var edit = Scenarios.TapThroughToControls(Device);

        Assert.StartsWith("@e", edit.Ref);
    });

    public static IEnumerable<object[]> Categories => ScenarioData.CategoryRows;

    [Theory]
    [MemberData(nameof(Categories))]
    public void Category_opens_its_list(string category, string row) => Run(() =>
    {
        var element = Scenarios.OpenCategory(Device, category, row);

        Assert.Equal(row, element.Label);
    });
}
