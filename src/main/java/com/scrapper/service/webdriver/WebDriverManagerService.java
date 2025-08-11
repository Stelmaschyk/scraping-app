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

    /**
     * Ініціалізує та налаштовує Chrome WebDriver
     * @return налаштований WebDriver
     */
    public WebDriver initializeWebDriver() {
        log.info("🔧 Initializing Chrome WebDriver...");
        
        try {
            // Налаштовуємо ChromeDriver
            WebDriverManager.chromedriver().setup();
            
            // Створюємо ChromeDriver з базовими налаштуваннями
            ChromeDriver driver = new ChromeDriver();
            
            log.info("✅ Chrome WebDriver initialized successfully");
            return driver;
            
        } catch (Exception e) {
            log.error("❌ Failed to initialize Chrome WebDriver: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize WebDriver", e);
        }
    }



    /**
     * Перевіряє чи WebDriver працює коректно
     * @param driver WebDriver для перевірки
     * @return true якщо WebDriver здоровий
     */
    public boolean isWebDriverHealthy(WebDriver driver) {
        if (driver == null) {
            log.warn("⚠️ WebDriver is null");
            return false;
        }

        try {
            // Спробуємо отримати поточний URL
            String currentUrl = driver.getCurrentUrl();
            log.debug("🔍 WebDriver health check - current URL: {}", currentUrl);
            return true;
            
        } catch (Exception e) {
            log.warn("⚠️ WebDriver health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Безпечно закриває WebDriver
     * @param driver WebDriver для закриття
     */
    public void closeWebDriver(WebDriver driver) {
        if (driver == null) {
            log.debug("ℹ️ WebDriver is already null, nothing to close");
            return;
        }

        try {
            driver.quit();
            log.info("🔒 WebDriver closed successfully");
            
        } catch (Exception e) {
            log.warn("⚠️ Error closing WebDriver: {}", e.getMessage());
        }
    }
}
