package showcase.appium.support;

import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

@ExtendWith(AppiumExtension.class)
public abstract class BaseTest {
    protected AndroidDriver driver;

    @BeforeEach
    void bindDriver(ExtensionContext context) {
        driver = AppiumExtension.driver(context);
    }
}
