using Mobium;
using NUnit.Framework;

namespace Showcase.Mobium.NUnit;

[Category("controls")]
public sealed class ControlsTests : MobiumTest
{
    [TestCase("Mobium was here")]
    [TestCase("and replaced it")]
    public void Fill_replaces_the_field_value(string text)
    {
        var shown = Scenarios.FillReplacesValue(Device, text);

        Assert.That(shown, Does.Contain(text).And.Not.Contain(Scenarios.FirstFill));
    }

    [Test]
    public void Set_and_unset_a_checkbox()
    {
        var states = Scenarios.SetAndUnsetCheckbox(Device);

        Assert.That(states, Is.EqualTo(new Scenarios.CheckboxStates(AfterSet: true, AfterUnset: false)));
    }

    [Test]
    public void Map_diff_reports_what_a_tap_changed()
    {
        var changed = Scenarios.MapDiffAfterToggle(Device);

        Assert.That(changed, Has.Some.Matches<Scenarios.Change>(c => c.IsTheToggle), "toggle change not in map_diff");
    }

    [Test, Category("dialogs")]
    public void Dialog_is_read_and_answered()
    {
        var outcome = Scenarios.AnswerAlertDialog(Device);

        Assert.That(outcome.TextWhileOpen, Does.Contain("Lorem ipsum"));
        Assert.That(outcome.ListAfterwards, Is.Not.Null);
    }

    [Test, Category("errors")]
    public void Missing_element_is_refused_with_a_named_error()
    {
        var error = Assert.Throws<NoSuchElementException>(() => Scenarios.TapMissingElement(Device));

        Assert.That(error, Is.InstanceOf<MobiumException>());
        Assert.That(error!.Message, Does.Contain("noSuchButton"));
    }

    [Test, Category("smoke")]
    public void Screenshot_is_a_PNG()
    {
        var png = Scenarios.Screenshot(Device);

        Assert.That(png.Take(8), Is.EqualTo(Scenarios.PngSignature));
    }
}
