package showcase.bidi.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.ClassTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Runs the whole test class once per browser (Chrome, Firefox, Edge by default).
 * Every test gets its own {@link BrowserSession} and {@link ConsoleGuard}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ClassTemplate
@ExtendWith(BrowserMatrix.class)
public @interface CrossBrowser {}
