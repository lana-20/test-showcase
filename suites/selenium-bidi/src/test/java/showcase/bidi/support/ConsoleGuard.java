package showcase.bidi.support;

import io.qameta.allure.Allure;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.openqa.selenium.bidi.log.BaseLogEntry;
import org.openqa.selenium.bidi.log.LogLevel;
import org.openqa.selenium.bidi.log.StackFrame;
import org.openqa.selenium.bidi.log.StackTrace;
import org.openqa.selenium.bidi.module.LogInspector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.FluentWait;

/**
 * What the page reported through BiDi {@code log.entryAdded} during one test:
 * uncaught JavaScript errors and console.error calls. {@link ConsoleGuardExtension}
 * starts one before each test (before the first navigation) and calls
 * {@link #verify()} after it, so any such problem fails the test.
 * A test that causes errors on purpose takes the guard as a parameter, waits
 * for them with {@link #awaitProblem}, and {@link #acknowledge}s them.
 */
public final class ConsoleGuard implements AutoCloseable {

    /** One thing the page did that a healthy app should not. */
    public record Problem(String kind, String text, String where) {
        @Override
        public String toString() {
            return kind + ": " + text + (where == null ? "" : " (" + where + ")");
        }
    }

    private final List<Problem> problems = new CopyOnWriteArrayList<>();
    private final LogInspector inspector;

    ConsoleGuard(RemoteWebDriver driver) {
        inspector = new LogInspector(driver);
        inspector.onJavaScriptException(e -> problems.add(new Problem("uncaught error", e.getText(), where(e))));
        inspector.onConsoleEntry(e -> {
            if (e.getLevel() == LogLevel.ERROR) {
                problems.add(new Problem("console.error", e.getText(), where(e)));
            }
        });
    }

    /** Throws if the page has reported any problem not yet acknowledged. */
    public void verify() {
        if (problems.isEmpty()) {
            return;
        }
        String report = problems.stream().map(Problem::toString).collect(Collectors.joining("\n  "));
        Allure.addAttachment("console problems", report);
        throw new AssertionError("The page reported " + problems.size() + " JavaScript problem(s):\n  " + report);
    }

    /** Waits for a BiDi log event that matches; the events arrive asynchronously. */
    public Problem awaitProblem(String description, Predicate<Problem> matching) {
        return Allure.step("Wait for the log.entryAdded event: " + description, () ->
                new FluentWait<>(problems)
                        .withTimeout(Config.timeout())
                        .pollingEvery(Duration.ofMillis(50))
                        .withMessage("no log.entryAdded event for " + description)
                        .until(list -> list.stream().filter(matching).findFirst().orElse(null)));
    }

    public List<Problem> problems() {
        return List.copyOf(problems);
    }

    /** Marks problems a test caused on purpose, so the after-test check does not fail on them. */
    public void acknowledge(Problem... expected) {
        Allure.step("Acknowledge " + expected.length + " expected problem(s)", () -> {
            for (Problem p : expected) {
                problems.remove(p);
            }
        });
    }

    @Override
    public void close() {
        inspector.close();
    }

    /** The top stack frame, when the browser sends one. */
    private static String where(BaseLogEntry e) {
        StackTrace trace = e.getStackTrace();
        if (trace == null || trace.getCallFrames() == null || trace.getCallFrames().isEmpty()) {
            return null;
        }
        StackFrame top = trace.getCallFrames().get(0);
        return top.getUrl() + ":" + (top.getLineNumber() + 1);
    }
}
