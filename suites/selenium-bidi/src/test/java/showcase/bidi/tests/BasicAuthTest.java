package showcase.bidi.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.UsernameAndPassword;
import org.openqa.selenium.bidi.module.Network;
import org.openqa.selenium.bidi.network.AddInterceptParameters;
import org.openqa.selenium.bidi.network.InterceptPhase;
import org.openqa.selenium.bidi.network.ResponseData;
import org.openqa.selenium.bidi.network.ResponseDetails;
import org.openqa.selenium.remote.RemoteWebDriver;
import showcase.bidi.pages.LabPage;
import showcase.bidi.support.CrossBrowser;
import showcase.bidi.support.Events;

/**
 * The demo server protects /lab with HTTP Basic auth. A browser would show a
 * native login dialog that WebDriver cannot type into; BiDi answers the
 * challenge (network.authRequired) inside the browser instead.
 */
@CrossBrowser
@Epic("Tidepool web app")
@Feature("Basic auth")
class BasicAuthTest {
    // Demo credentials, served in the open by demo-app/serve.mjs.
    private static final UsernameAndPassword LAB = new UsernameAndPassword("lab", "reef-pass");

    @Test
    @DisplayName("The BiDi authentication handler answers the lab's Basic auth challenge")
    void auth_handler_signs_in(RemoteWebDriver driver) {
        Allure.step("Register an authentication handler for /" + LabPage.PATH, () ->
                driver.network().addAuthenticationHandler(uri -> uri.getPath().endsWith("/" + LabPage.PATH), LAB));

        assertEquals("Signed in to the device lab.", new LabPage(driver).open().status());
    }

    @Test
    @DisplayName("A canceled challenge leaves the server's 401, seen in the authRequired event")
    void canceled_challenge_shows_401(RemoteWebDriver driver) {
        try (Network network = new Network(driver)) {
            network.addIntercept(new AddInterceptParameters(InterceptPhase.AUTH_REQUIRED));
            CompletableFuture<ResponseDetails> challenge = new CompletableFuture<>();
            CompletableFuture<ResponseDetails> completed = new CompletableFuture<>();
            network.onAuthRequired(e -> {
                challenge.complete(e);
                network.cancelAuth(e.getRequest().getRequestId());
            });
            network.onResponseCompleted(e -> {
                if (e.getRequest().getUrl().endsWith("/" + LabPage.PATH)) completed.complete(e);
            });

            LabPage lab = new LabPage(driver).open();
            Events.await(challenge, "authRequired for /" + LabPage.PATH);

            ResponseData response = Events.await(completed, "responseCompleted for /" + LabPage.PATH).getResponseData();
            assertEquals(401, response.getStatus());
            String challengeHeader = response.getHeaders().stream()
                    .filter(h -> h.getName().equalsIgnoreCase("www-authenticate"))
                    .map(h -> h.getValue().getValue())
                    .findFirst()
                    .orElse("");
            assertTrue(challengeHeader.startsWith("Basic realm=\"Tidepool lab\""), "WWW-Authenticate: " + challengeHeader);
            assertEquals("authentication required", lab.bodyText());
        }
    }
}
