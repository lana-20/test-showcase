using Allure.Net.Commons;

namespace Showcase.Mobium;

/// <summary>
/// Allure steps that are safe to call from anywhere. Inside a test the call
/// becomes a step in the report; outside one (a one-time fixture, for example)
/// it simply runs, because there is no test to attach a step to.
/// </summary>
public static class Steps
{
    static bool InTest
    {
        get
        {
            var context = AllureLifecycle.Instance.Context;
            return context.HasTest || context.HasFixture;
        }
    }

    public static void Run(string name, Action action)
    {
        if (InTest) AllureApi.Step(name, action);
        else action();
    }

    public static T Run<T>(string name, Func<T> function) =>
        InTest ? AllureApi.Step(name, function) : function();

    public static void Attach(string name, string type, byte[] content, string extension)
    {
        if (InTest) AllureApi.AddAttachment(name, type, content, extension);
    }
}
