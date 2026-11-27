package showcase.appium.support;

import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(AppiumExtension.class)
public abstract class BaseTest {
    protected AndroidDriver driver;

    @BeforeEach
    void bindDriver(AndroidDriver driver) {
        this.driver = driver;
    }
}
