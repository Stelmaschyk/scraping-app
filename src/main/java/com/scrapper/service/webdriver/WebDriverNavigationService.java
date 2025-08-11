package com.scrapper.service.webdriver;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Сервіс для навігації по сторінках через WebDriver
 * Відповідає за перехід на URL, очікування завантаження та базову валідацію
 */
@Service
@Slf4j
public class WebDriverNavigationService {

    @Value("${scraping.selenium.timeout:30}")
    private long defaultTimeout;

    @Value("${scraping.selenium.page.load.delay:3000}")
    private long pageLoadDelay;

    /**
     * Переходить на вказаний URL
     * @param driver WebDriver для навігації
     * @param url URL для переходу
     * @return true якщо перехід успішний
     */
    public boolean navigateToUrl(WebDriver driver, String url) {
        if (driver == null || url == null || url.trim().isEmpty()) {
            log.error("❌ Invalid parameters for navigation: driver={}, url={}", driver, url);
            return false;
        }

        try {
            log.info("🌐 Navigating to URL: {}", url);

            // Переходимо на URL
            driver.get(url);

            // Чекаємо завантаження сторінки
            waitForPageLoad(driver);

            // Перевіряємо поточний URL
            String currentUrl = driver.getCurrentUrl();
            log.info("✅ Navigation successful. Current URL: {}", currentUrl);

            return true;

        } catch (WebDriverException e) {
            log.error("❌ Navigation failed: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("❌ Unexpected error during navigation: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Переходить на URL з перевіркою редиректу
     * @param driver WebDriver для навігації
     * @param url URL для переходу
     * @param expectedUrl очікуваний URL після редиректу
     * @return true якщо перехід успішний та URL співпадає
     */
    public boolean navigateToUrlWithRedirectCheck(WebDriver driver, String url, String expectedUrl) {
        if (!navigateToUrl(driver, url)) {
            return false;
        }

        try {
            // Чекаємо додатковий час для редиректу
            Thread.sleep(2000);

            String currentUrl = driver.getCurrentUrl();
            boolean urlMatches = currentUrl.contains(expectedUrl) || currentUrl.equals(expectedUrl);

            if (urlMatches) {
                log.info("✅ Redirect check passed. Expected: {}, Current: {}", expectedUrl, currentUrl);
                return true;
            } else {
                log.warn("⚠️ Redirect check failed. Expected: {}, Current: {}", expectedUrl, currentUrl);
                return false;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("⚠️ Navigation interrupted during redirect check");
            return false;
        }
    }

    /**
     * Очікує завантаження сторінки
     * @param driver WebDriver для очікування
     */
    public void waitForPageLoad(WebDriver driver) {
        try {
            log.debug("⏳ Waiting for page to load...");
            
            // Встановлюємо таймаут для завантаження
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(defaultTimeout));
            
            // Чекаємо додатковий час для JavaScript
            Thread.sleep(pageLoadDelay);
            
            log.debug("✅ Page load wait completed");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("⚠️ Page load wait interrupted");
        } catch (Exception e) {
            log.warn("⚠️ Error during page load wait: {}", e.getMessage());
        }
    }

    /**
     * Перевіряє чи сторінка завантажилася коректно
     * @param driver WebDriver для перевірки
     * @return true якщо сторінка завантажилася
     */
    public boolean isPageLoaded(WebDriver driver) {
        if (driver == null) {
            return false;
        }

        try {
            // Перевіряємо готовність сторінки
            String readyState = (String) ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("return document.readyState");

            boolean isComplete = "complete".equals(readyState);

            if (isComplete) {
                log.debug("✅ Page is fully loaded (readyState: {})", readyState);
            } else {
                log.debug("⏳ Page is still loading (readyState: {})", readyState);
            }

            return isComplete;

        } catch (Exception e) {
            log.warn("⚠️ Could not check page ready state: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Перевіряє чи сторінка містить очікуваний контент
     * @param driver WebDriver для перевірки
     * @param expectedTitle очікуваний заголовок сторінки
     * @return true якщо заголовок співпадає
     */
    public boolean isPageContentValid(WebDriver driver, String expectedTitle) {
        if (driver == null || expectedTitle == null) {
            return false;
        }

        try {
            String actualTitle = driver.getTitle();
            boolean titleMatches = actualTitle != null &&
                                 (actualTitle.contains(expectedTitle) || expectedTitle.contains(actualTitle));

            if (titleMatches) {
                log.info("✅ Page content validation passed. Expected: '{}', Actual: '{}'",
                        expectedTitle, actualTitle);
            } else {
                log.warn("⚠️ Page content validation failed. Expected: '{}', Actual: '{}'",
                        expectedTitle, actualTitle);
            }

            return titleMatches;

        } catch (Exception e) {
            log.warn("⚠️ Error during page content validation: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Перевіряє чи сторінка не є порожньою
     * @param driver WebDriver для перевірки
     * @return true якщо сторінка містить контент
     */
    public boolean isPageNotEmpty(WebDriver driver) {
        if (driver == null) {
            return false;
        }

        try {
            // Перевіряємо кількість елементів на сторінці
            int elementCount = driver.findElements(org.openqa.selenium.By.cssSelector("*")).size();

            boolean hasContent = elementCount > 50; // Мінімальна кількість елементів для валідної сторінки

            if (hasContent) {
                log.debug("✅ Page has content ({} elements)", elementCount);
            } else {
                log.warn("⚠️ Page seems empty ({} elements)", elementCount);
            }

            return hasContent;

        } catch (Exception e) {
            log.warn("⚠️ Error checking page content: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Оновлює сторінку
     * @param driver WebDriver для оновлення
     * @return true якщо оновлення успішне
     */
    public boolean refreshPage(WebDriver driver) {
        if (driver == null) {
            return false;
        }

        try {
            log.info("🔄 Refreshing page...");
            driver.navigate().refresh();

            // Чекаємо завантаження після оновлення
            waitForPageLoad(driver);

            log.info("✅ Page refreshed successfully");
            return true;

        } catch (Exception e) {
            log.error("❌ Failed to refresh page: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Повертається на попередню сторінку
     * @param driver WebDriver для навігації
     * @return true якщо повернення успішне
     */
    public boolean goBack(WebDriver driver) {
        if (driver == null) {
            return false;
        }

        try {
            log.info("⬅️ Going back to previous page...");
            driver.navigate().back();

            // Чекаємо завантаження
            waitForPageLoad(driver);

            log.info("✅ Successfully went back to previous page");
            return true;

        } catch (Exception e) {
            log.error("❌ Failed to go back: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Переходить на наступну сторінку (якщо можливо)
     * @param driver WebDriver для навігації
     * @return true якщо перехід успішний
     */
    public boolean goForward(WebDriver driver) {
        if (driver == null) {
            return false;
        }

        try {
            log.info("➡️ Going forward to next page...");
            driver.navigate().forward();

            // Чекаємо завантаження
            waitForPageLoad(driver);

            log.info("✅ Successfully went forward to next page");
            return true;

        } catch (Exception e) {
            log.error("❌ Failed to go forward: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Отримує поточний URL сторінки
     * @param driver WebDriver для отримання URL
     * @return поточний URL або null у разі помилки
     */
    public String getCurrentUrl(WebDriver driver) {
        if (driver == null) {
            return null;
        }

        try {
            String currentUrl = driver.getCurrentUrl();
            log.debug("🔍 Current URL: {}", currentUrl);
            return currentUrl;

        } catch (Exception e) {
            log.warn("⚠️ Could not get current URL: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Отримує заголовок поточної сторінки
     * @param driver WebDriver для отримання заголовка
     * @return заголовок сторінки або null у разі помилки
     */
    public String getPageTitle(WebDriver driver) {
        if (driver == null) {
            return null;
        }

        try {
            String title = driver.getTitle();
            log.debug("📄 Page title: {}", title);
            return title;

        } catch (Exception e) {
            log.warn("⚠️ Could not get page title: {}", e.getMessage());
            return null;
        }
    }
}
