package showcase.bidi.support;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Fails any test during which the page threw an uncaught JavaScript error or
 * called console.error. Registered after {@link BrowserSession}, so it starts
 * listening once the session exists and checks before the session quits.
 */
final class ConsoleGuardExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(ConsoleGuardExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) {
        context.getStore(NS).put(ConsoleGuard.class, new ConsoleGuard(BrowserSession.driver(context)));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        ConsoleGuard guard = context.getStore(NS).remove(ConsoleGuard.class, ConsoleGuard.class);
        if (guard == null) {
            return;
        }
        try (guard) {
            guard.verify();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == ConsoleGuard.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
        return context.getStore(NS).get(ConsoleGuard.class, ConsoleGuard.class);
    }
}
