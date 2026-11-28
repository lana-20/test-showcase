using Mobium;
using Xunit;

namespace Showcase.Mobium.Xunit;

[Trait("Category", "controls")]
public sealed class ControlsTests(DeviceFixture fixture) : MobiumTest(fixture)
{
    [Theory]
    [InlineData("Mobium was here")]
    [InlineData("and replaced it")]
    public void Fill_replaces_the_field_value(string text) => Run(() =>
    {
        var shown = Scenarios.FillReplacesValue(Device, text);

        Assert.Contains(text, shown);
        Assert.DoesNotContain(Scenarios.FirstFill, shown);
    });

    [Fact]
    public void Set_and_unset_a_checkbox() => Run(() =>
    {
        var states = Scenarios.SetAndUnsetCheckbox(Device);

        Assert.Equal(new Scenarios.CheckboxStates(AfterSet: true, AfterUnset: false), states);
    });

    [Fact]
    public void Map_diff_reports_what_a_tap_changed() => Run(() =>
    {
        var changed = Scenarios.MapDiffAfterToggle(Device);

        Assert.Contains(changed, c => c.IsTheToggle);
    });

    [Fact, Trait("Category", "dialogs")]
    public void Dialog_is_read_and_answered() => Run(() =>
    {
        var outcome = Scenarios.AnswerAlertDialog(Device);

        Assert.Contains("Lorem ipsum", outcome.TextWhileOpen);
        Assert.NotNull(outcome.ListAfterwards);
    });

    [Fact, Trait("Category", "errors")]
    public void Missing_element_is_refused_with_a_named_error() => Run(() =>
    {
        var error = Assert.Throws<NoSuchElementException>(() => Scenarios.TapMissingElement(Device));

        Assert.IsAssignableFrom<MobiumException>(error);
        Assert.Contains("noSuchButton", error.Message);
    });

    [Fact, Trait("Category", "smoke")]
    public void Screenshot_is_a_PNG() => Run(() =>
    {
        var png = Scenarios.Screenshot(Device);

        Assert.Equal(Scenarios.PngSignature, png.Take(8));
    });
}
