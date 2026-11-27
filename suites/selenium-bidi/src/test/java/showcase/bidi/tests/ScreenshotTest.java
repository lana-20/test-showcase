package showcase.bidi.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.bidi.browsingcontext.BrowsingContext;
import org.openqa.selenium.bidi.browsingcontext.CaptureScreenshotParameters;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import showcase.bidi.pages.BoardPage;
import showcase.bidi.pages.LoginPage;
import showcase.bidi.support.Attachments;
import showcase.bidi.support.CrossBrowser;

/** browsingContext.captureScreenshot: an element clip and the whole document, attached to Allure. */
@CrossBrowser
@Epic("Tidepool web app")
@Feature("Screenshots")
class ScreenshotTest {
    private BoardPage board;

    @BeforeEach
    void boardWithTasks(RemoteWebDriver driver) {
        LoginPage.useStoredSession(driver);
        board = new BoardPage(driver).open()
                .addTask("Screenshot the task list")
                .addTask("Compare viewport and document");
    }

    @Test
    @DisplayName("An element screenshot is exactly the task list")
    void element_screenshot(RemoteWebDriver driver, BrowsingContext tab) throws IOException {
        WebElement list = board.taskList();
        BufferedImage shot = image(Attachments.png("task list (element screenshot)",
                tab.captureElementScreenshot(((RemoteWebElement) list).getId())));

        double dpr = devicePixelRatio(driver);
        Rectangle box = list.getRect();
        assertEquals(box.getWidth() * dpr, shot.getWidth(), 2 * dpr + 1, "width");
        assertEquals(box.getHeight() * dpr, shot.getHeight(), 2 * dpr + 1, "height");
    }

    @Test
    @DisplayName("A document screenshot covers the whole page, beyond the viewport")
    void full_page_screenshot(RemoteWebDriver driver, BrowsingContext tab) throws IOException {
        tab.setViewport(900, 320);
        BufferedImage viewport = image(Attachments.png("viewport", tab.captureScreenshot()));
        BufferedImage document = image(Attachments.png("full page (origin: document)",
                tab.captureScreenshot(new CaptureScreenshotParameters().origin(CaptureScreenshotParameters.Origin.DOCUMENT))));

        double dpr = devicePixelRatio(driver);
        long pageHeight = ((Number) driver.executeScript("return document.documentElement.scrollHeight")).longValue();
        assertTrue(pageHeight > 320, "the page should be taller than the viewport, was " + pageHeight);
        assertEquals(320 * dpr, viewport.getHeight(), dpr + 1, "viewport screenshot height");
        assertEquals(pageHeight * dpr, document.getHeight(), 2 * dpr + 1, "document screenshot height");
    }

    private static BufferedImage image(byte[] png) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        assertTrue(img != null, "not a PNG");
        return img;
    }

    private static double devicePixelRatio(RemoteWebDriver driver) {
        return ((Number) driver.executeScript("return window.devicePixelRatio")).doubleValue();
    }
}
