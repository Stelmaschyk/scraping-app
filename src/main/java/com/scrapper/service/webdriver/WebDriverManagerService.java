package com.scrapper.service.webdriver;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;
import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Сервіс для управління життєвим циклом WebDriver
 * Відповідає за створення, перевірку та закриття WebDriver
 */
@Service
@Slf4j
public class WebDriverManagerService {

    public WebDriver initializeWebDriver() {
        try {
            WebDriverManager.chromedriver().setup();
            return new ChromeDriver();
        } catch (Exception e) {
            log.error("❌ Failed to initialize WebDriver: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize WebDriver", e);
        }
    }



    public boolean isWebDriverHealthy(WebDriver driver) {
        if (driver == null) {
            return false;
        }
        try {
            driver.getCurrentUrl();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void closeWebDriver(WebDriver driver) {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                log.warn("⚠️ Error closing WebDriver: {}", e.getMessage());
            }
        }
    }
}
