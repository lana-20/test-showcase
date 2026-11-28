using System.Reflection;
using Allure.Net.Commons;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace Showcase.Mobium.MSTest;

/// <summary>
/// A small Allure adapter for MSTest. allure-csharp ships adapters for NUnit,
/// xUnit, Reqnroll and SpecFlow, not MSTest, so this writes the same result
/// files through Allure.Net.Commons, the library those adapters are built on.
/// </summary>
/// <remarks>
/// The whole test runs inside <see cref="Run"/>, on one call stack, so the
/// Allure context (an AsyncLocal) is the same for the test start, every step
/// and the stop, however MSTest schedules [TestInitialize] and [TestCleanup].
/// [TestCategory] values become Allure tags, and the method's arguments become
/// parameters, so each [DataRow] is its own test in the report.
/// </remarks>
static class AllureMSTest
{
    public static void Run(TestContext context, Type testClass, Action test, Action onFailure, object?[] arguments)
    {
        var method = context.TestName ?? "test";
        var args = string.Join(", ", arguments.Select(a => a?.ToString()));
        var fullName = $"{testClass.FullName}.{method}" + (arguments.Length > 0 ? $"({args})" : "");
        var labels = new List<Label>
        {
            Label.Framework("MSTest"), Label.Language(), Label.Host(), Label.Thread(),
            Label.Package(testClass.Namespace ?? ""), Label.TestClass(testClass.Name), Label.TestMethod(method),
            Label.Suite(testClass.Name),
        };
        labels.AddRange(Categories(testClass, method).Select(Label.Tag));

        var result = new Allure.Net.Commons.TestResult
        {
            uuid = Guid.NewGuid().ToString(),
            name = arguments.Length > 0 ? $"{method}({args})" : method,
            fullName = fullName,
            historyId = fullName,
            testCaseId = $"{testClass.FullName}.{method}",
            labels = labels,
            parameters = arguments.Select((a, i) => new Parameter { name = $"arg{i}", value = a?.ToString() }).ToList(),
        };

        var allure = AllureLifecycle.Instance;
        allure.StartTestCase(result);
        try
        {
            test();
            allure.StopTestCase(r => r.status = Status.passed);
        }
        catch (Exception e)
        {
            onFailure();
            allure.StopTestCase(r =>
            {
                r.status = e is AssertFailedException ? Status.failed : Status.broken;
                r.statusDetails = new StatusDetails { message = e.Message, trace = e.ToString() };
            });
            throw;
        }
        finally
        {
            allure.WriteTestCase();
        }
    }

    static IEnumerable<string> Categories(Type testClass, string method)
    {
        var onClass = testClass.GetCustomAttributes<TestCategoryAttribute>(inherit: true);
        var onMethod = testClass.GetMethods().Where(m => m.Name == method)
            .SelectMany(m => m.GetCustomAttributes<TestCategoryAttribute>(inherit: true));
        return onClass.Concat(onMethod).SelectMany(a => a.TestCategories).Distinct();
    }
}
