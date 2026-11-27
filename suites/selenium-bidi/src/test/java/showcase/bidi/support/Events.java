package showcase.bidi.support;

import io.qameta.allure.Allure;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Waiting on a BiDi event the browser pushes, instead of polling the page for its effect. */
public final class Events {
    private Events() {}

    public static <T> T await(CompletableFuture<T> event, String description) {
        return Allure.step("Wait for the BiDi event: " + description, () -> {
            try {
                return event.get(Config.timeout().toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                throw new AssertionError("No BiDi event within " + Config.timeout().toSeconds() + " s: " + description);
            } catch (ExecutionException e) {
                throw new AssertionError("The handler for \"" + description + "\" failed", e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for " + description, e);
            }
        });
    }
}
