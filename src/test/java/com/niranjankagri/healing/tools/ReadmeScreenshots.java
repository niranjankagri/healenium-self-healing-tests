package com.niranjankagri.healing.tools;

import com.niranjankagri.healing.app.DemoShop;
import com.niranjankagri.healing.app.Release;
import com.niranjankagri.healing.app.ShopSession;
import com.niranjankagri.healing.data.User;
import com.niranjankagri.healing.driver.DriverFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Not a test: takes the screenshots in {@code docs/images/} for the README. Run after a test run, with the
 * Healenium backend still up, so the report page has data:
 *
 * <pre>
 *   ./mvnw -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
 *   java -cp "target/classes;target/test-classes;$(cat target/cp.txt)" com.niranjankagri.healing.tools.ReadmeScreenshots
 * </pre>
 *
 * Uses plain locators of each release on purpose (it only arranges pages for a picture; it tests nothing).
 */
public final class ReadmeScreenshots {

    private static final Path OUT = Path.of("docs", "images");

    private ReadmeScreenshots() {
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(OUT);
        WebDriver driver = DriverFactory.create(DriverFactory.Kind.PLAIN);
        try (DemoShop shop = DemoShop.start(Release.V1)) {
            for (Release release : Release.values()) {
                shop.deploy(release);
                driver.get(shop.baseUrl() + "index.html");
                // Show the error box too: type a wrong password through the release's own form
                boolean v1 = release == Release.V1;
                driver.findElement(By.id(v1 ? "email" : "user-email")).sendKeys(User.wrongPassword().email());
                driver.findElement(By.id(v1 ? "password" : "user-password")).sendKeys(User.wrongPassword().password());
                driver.findElement(By.id(v1 ? "login-button" : "sign-in-button")).click();
                save(driver, "login-" + release.folder() + ".png");

                ShopSession.signInWithoutForm(driver, User.customer());
                waitFor(driver, "products.html");
                driver.findElement(By.cssSelector(v1 ? ".add-to-cart" : ".btn-add")).click();
                save(driver, "products-" + release.folder() + ".png");
            }
            // The report opens on the newest session; expand its first row to show one healing in full
            driver.get("http://localhost:7878/healenium/report");
            // The report's own toggle needs jQuery from a CDN; removing the "collapsed" class works offline too
            ((JavascriptExecutor) driver).executeScript(
                    "const row = document.querySelector('.selector-title-container.content-hidden');"
                            + "if (row) row.classList.remove('content-hidden');");
            save(driver, "healenium-report.png");
        } finally {
            driver.quit();
        }
    }

    private static void waitFor(WebDriver driver, String file) {
        long end = System.currentTimeMillis() + 5000;
        while (!driver.getCurrentUrl().endsWith(file) && System.currentTimeMillis() < end) {
            Thread.onSpinWait();
        }
    }

    private static void save(WebDriver driver, String name) throws IOException {
        // Full page height, so nothing below the fold is cut off
        Object height = ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(1280, Math.max(700, ((Number) height).intValue() + 40)));
        Files.write(OUT.resolve(name), ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        System.out.println("saved " + OUT.resolve(name));
    }
}
