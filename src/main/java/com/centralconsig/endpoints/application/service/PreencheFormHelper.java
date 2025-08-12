package com.centralconsig.endpoints.application.service;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class PreencheFormHelper {
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final Actions actions;

    public PreencheFormHelper(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
        this.actions = new Actions(driver);
    }

    public WebElement findElement(By locator) {
        WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', behavior: 'smooth'});", element
        );

        wait.until(ExpectedConditions.visibilityOf(element));
        wait.until(ExpectedConditions.elementToBeClickable(element));

        return element;
    }

    public void click(By locator) {
        WebElement element = findElement(locator);
        try {
            actions.moveToElement(element).pause(Duration.ofMillis(200)).click().perform();
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }

    public void sendKeys(By locator, String text) {
        WebElement element = findElement(locator);
        element.clear();
        element.sendKeys(text);
    }

    public WebElement findFieldByPossibleLabels(String... possibleLabels) {
        List<WebElement> allInputs = driver.findElements(By.xpath("//input | //textarea | //select"));

        for (WebElement input : allInputs) {
            try {
                String aria = input.getAttribute("aria-label");
                String name = input.getAttribute("name");
                String placeholder = input.getAttribute("placeholder");
                String id = input.getAttribute("id");

                for (String label : possibleLabels) {
                    if ((aria != null && aria.toLowerCase().contains(label.toLowerCase())) ||
                            (name != null && name.toLowerCase().contains(label.toLowerCase())) ||
                            (placeholder != null && placeholder.toLowerCase().contains(label.toLowerCase())) ||
                            (id != null && id.toLowerCase().contains(label.toLowerCase()))) {

                        wait.until(ExpectedConditions.elementToBeClickable(input));
                        return input;
                    }
                }
            } catch (StaleElementReferenceException ignored) {}
        }

        throw new NoSuchElementException("Nenhum campo encontrado para labels: " + Arrays.toString(possibleLabels));
    }

    public void fillField(String value, String... possibleLabels) {
        WebElement field = findFieldByPossibleLabels(possibleLabels);

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', behavior: 'smooth'});", field
        );

        wait.until(ExpectedConditions.elementToBeClickable(field));

        try {
            field.clear();
            field.click();
            field.sendKeys(value);
        } catch (ElementNotInteractableException e) {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
                    field, value
            );
        }
    }
}
