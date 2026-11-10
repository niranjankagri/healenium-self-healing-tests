package com.niranjankagri.healing.tests;

import com.epam.healenium.SelfHealingDriver;
import com.niranjankagri.healing.app.DemoShop;
import com.niranjankagri.healing.app.Release;
import com.niranjankagri.healing.driver.DriverFactory;
import com.niranjankagri.healing.pages.LoginPage;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Life cycle shared by every test class: one shop for the whole suite, the release a class asks for,
 * and a fresh browser per test (with a screenshot when the test fails).
 */
public abstract class BaseTest {

    private static DemoShop shop;

    protected WebDriver driver;

    /** The release this class tests. */
    protected abstract Release release();

    /** Self-healing or plain driver. */
    protected abstract DriverFactory.Kind driverKind();

    @BeforeSuite(alwaysRun = true)
    public void startShop() {
        shop = DemoShop.start(Release.V1);
    }

    @AfterSuite(alwaysRun = true)
    public void stopShop() {
        if (shop != null) {
            shop.close();
        }
    }

    /** "Deploys" this class's release before its first test, at the same URL as before. */
    @BeforeClass(alwaysRun = true)
    public void deployRelease() {
        shop.deploy(release());
    }

    @BeforeMethod(alwaysRun = true)
    public void openBrowser() {
        driver = DriverFactory.create(driverKind());
    }

    @AfterMethod(alwaysRun = true)
    public void closeBrowser(ITestResult result) {
        if (driver == null) {
            return;
        }
        try {
            if (!result.isSuccess()) {
                saveScreenshot(result);
            }
        } finally {
            driver.quit();
            driver = null;
        }
    }

    /** The sign-in page of the shop, already loaded. */
    protected LoginPage openShop() {
        return new LoginPage(driver).open(shop.baseUrl());
    }

    /** Best effort: a screenshot that cannot be taken must not turn into a second failure. */
    private void saveScreenshot(ITestResult result) {
        try {
            // SelfHealingDriver is a proxy that does not implement TakesScreenshot; the ChromeDriver inside does
            WebDriver real = driver instanceof SelfHealingDriver healing ? healing.getDelegate() : driver;
            byte[] png = ((TakesScreenshot) real).getScreenshotAs(OutputType.BYTES);
            Path folder = Files.createDirectories(Path.of("target", "screenshots"));
            String name = getClass().getSimpleName() + "." + result.getMethod().getMethodName() + ".png";
            Files.write(folder.resolve(name), png);
        } catch (IOException | RuntimeException e) {
            System.err.println("No screenshot for " + result.getName() + ": " + e);
        }
    }
}
