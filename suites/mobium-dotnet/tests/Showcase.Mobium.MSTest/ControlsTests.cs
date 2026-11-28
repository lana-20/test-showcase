using Microsoft.VisualStudio.TestTools.UnitTesting;
using Mobium;

namespace Showcase.Mobium.MSTest;

[TestClass]
[TestCategory("controls")]
public sealed class ControlsTests : MobiumTest
{
    [TestMethod]
    [DataRow("Mobium was here")]
    [DataRow("and replaced it")]
    public void Fill_replaces_the_field_value(string text) => Run(() =>
    {
        var field = Scenarios.FillReplacesValue(Device, text);

        Assert.AreEqual(text, field.Value);
    }, text);

    [TestMethod]
    public void Set_and_unset_a_checkbox() => Run(() =>
    {
        var states = Scenarios.SetAndUnsetCheckbox(Device);

        Assert.AreEqual(new Scenarios.CheckboxStates(AfterSet: true, AfterUnset: false), states);
    });

    [TestMethod]
    public void Map_diff_reports_what_a_tap_changed() => Run(() =>
    {
        var changed = Scenarios.MapDiffAfterToggle(Device);

        Assert.IsTrue(changed.Any(c => c.IsTheToggle), $"toggle change not in map_diff: {string.Join("; ", changed)}");
    });

    [TestMethod, TestCategory("dialogs")]
    public void Dialog_is_read_and_answered() => Run(() =>
    {
        var outcome = Scenarios.AnswerAlertDialog(Device);

        StringAssert.Contains(outcome.TextWhileOpen, "Lorem ipsum");
        Assert.IsNotNull(outcome.ListAfterwards);
    });

    [TestMethod, TestCategory("errors")]
    public void Missing_element_is_refused_with_a_named_error() => Run(() =>
    {
        var error = Assert.ThrowsExactly<NoSuchElementException>(() => Scenarios.TapMissingElement(Device));

        Assert.IsInstanceOfType<MobiumException>(error);
        StringAssert.Contains(error.Message, "noSuchButton");
    });

    [TestMethod, TestCategory("smoke")]
    public void Screenshot_is_a_PNG() => Run(() =>
    {
        var png = Scenarios.Screenshot(Device);

        CollectionAssert.AreEqual(Scenarios.PngSignature, png.Take(8).ToArray());
    });
}
