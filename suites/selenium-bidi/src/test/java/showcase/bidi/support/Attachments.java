package showcase.bidi.support;

import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;
import java.util.Base64;

/** BiDi returns screenshots as base64 PNG; Allure wants bytes. */
public final class Attachments {
    private Attachments() {}

    public static byte[] png(String name, String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        Allure.addAttachment(name, "image/png", new ByteArrayInputStream(bytes), "png");
        return bytes;
    }
}
