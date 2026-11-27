package showcase.bidi.support;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContext;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContextProvider;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

/** One invocation of a {@link CrossBrowser} class per browser in {@link Config#browsers()}. */
public final class BrowserMatrix implements ClassTemplateInvocationContextProvider {

    @Override
    public boolean supportsClassTemplate(ExtensionContext context) {
        return true;
    }

    @Override
    public Stream<ClassTemplateInvocationContext> provideClassTemplateInvocationContexts(ExtensionContext context) {
        return Config.browsers().stream().map(BrowserMatrix::on);
    }

    private static ClassTemplateInvocationContext on(Browser browser) {
        return new ClassTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return browser.displayName();
            }

            // Order matters: the guard's afterEach runs before the session quits.
            @Override
            public List<Extension> getAdditionalExtensions() {
                return List.of(new BrowserSession(browser), new ConsoleGuardExtension());
            }
        };
    }
}
