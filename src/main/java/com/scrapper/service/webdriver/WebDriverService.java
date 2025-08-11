package com.scrapper.service.webdriver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;

/**
 * Головний сервіс WebDriver який об'єднує всі інші сервіси
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebDriverService {

    private final WebDriverManagerService webDriverManagerService;
    private final WebDriverConfigService webDriverConfigService;

    /**
     * Створює та налаштовує WebDriver для скрапінгу
     *
     * @return налаштований WebDriver
     */
    public WebDriver createWebDriver() {
        log.info("Creating WebDriver for scraping...");
        var chromeOptions = webDriverConfigService.createChromeOptions();
        return new ChromeDriver(chromeOptions);
    }

    /**
     * Безпечно закриває WebDriver
     *
     * @param driver WebDriver для закриття
     */
    public void closeWebDriver(WebDriver driver) {
        webDriverManagerService.closeWebDriver(driver);
    }

    /**
     * Отримує поточний URL сторінки
     */
    public String getCurrentUrl(WebDriver driver) {
        try {
            return driver.getCurrentUrl();
        } catch (Exception e) {
            log.warn("⚠️ Could not get current URL: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Перевіряє чи WebDriver здоровий
     */
    public boolean isWebDriverHealthy(WebDriver driver) {
        return webDriverManagerService.isWebDriverHealthy(driver);
    }


}




