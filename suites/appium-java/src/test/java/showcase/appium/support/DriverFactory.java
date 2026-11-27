package showcase.appium.support;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import java.net.URI;
import java.time.Duration;

public final class DriverFactory {
    public static final String APP_PACKAGE = "io.appium.android.apis";

    private DriverFactory() {}

    public static AndroidDriver create() {
        UiAutomator2Options options = new UiAutomator2Options()
                .setApp(Config.appPath())
                .setAppPackage(APP_PACKAGE)
                .setAppActivity(".ApiDemos")
                .setAutoGrantPermissions(true)
                .setNewCommandTimeout(Duration.ofSeconds(120))
                .setUiautomator2ServerInstallTimeout(Duration.ofSeconds(120))
                .setAdbExecTimeout(Duration.ofSeconds(60))
                .setDisableWindowAnimation(true);
        if (Config.udid() != null) {
            options.setUdid(Config.udid());
        }
        try {
            return new AndroidDriver(URI.create(Config.appiumUrl()).toURL(), options);
        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException("Bad APPIUM_URL: " + Config.appiumUrl(), e);
        }
    }
}
