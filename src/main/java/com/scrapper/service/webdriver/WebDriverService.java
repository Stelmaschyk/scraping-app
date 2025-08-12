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
     * @return налаштований WebDriver
     */
    public WebDriver createWebDriver() {
        log.info("Creating WebDriver for scraping...");
        var chromeOptions = webDriverConfigService.createChromeOptions();
        return new ChromeDriver(chromeOptions);
    }

    /**
     * Закриває WebDriver
     * @param driver WebDriver для закриття
     */
    public void closeWebDriver(WebDriver driver) {
        webDriverManagerService.closeWebDriver(driver);
    }
}




