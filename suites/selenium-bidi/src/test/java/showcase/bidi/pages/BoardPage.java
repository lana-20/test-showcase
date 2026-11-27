package showcase.bidi.pages;

import io.qameta.allure.Allure;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public final class BoardPage extends Page {
    private static final By HEADING = By.cssSelector("h1#title");
    private static final By GREETING = testId("greeting");
    private static final By NEW_TASK = testId("new-task");
    private static final By TASK_LIST = testId("task-list");
    private static final By TASK_TITLE = testId("task-title");
    private static final By REMAINING = testId("remaining");
    private static final By TODAY = testId("today");

    public BoardPage(WebDriver driver) {
        super(driver);
    }

    public BoardPage open() {
        Allure.step("Open the task board", () -> driver.get(url("board.html")));
        return waitUntilShown();
    }

    public BoardPage waitUntilShown() {
        wait.until(ExpectedConditions.textToBe(HEADING, "Tasks"));
        return this;
    }

    public String greeting() {
        return visible(GREETING).getText();
    }

    public BoardPage addTask(String title) {
        Allure.step("Add the task \"" + title + "\"", () -> {
            int before = driver.findElements(TASK_TITLE).size();
            visible(NEW_TASK).sendKeys(title, Keys.ENTER);
            wait.until(ExpectedConditions.numberOfElementsToBe(TASK_TITLE, before + 1));
        });
        return this;
    }

    public List<String> taskTitles() {
        return driver.findElements(TASK_TITLE).stream().map(WebElement::getText).toList();
    }

    public void waitForRemaining(String text) {
        waitForText(REMAINING, text);
    }

    public WebElement taskList() {
        return visible(TASK_LIST);
    }

    public String today() {
        return visible(TODAY).getText();
    }
}
