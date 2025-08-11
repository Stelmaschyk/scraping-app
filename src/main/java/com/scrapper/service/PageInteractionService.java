package com.scrapper.service;

import com.scrapper.util.ScrapingSelectors;
import com.scrapper.validation.Validation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервіс для взаємодії зі сторінками
 * Відповідає за Load More кнопки, прокрутку, пошук елементів та альтернативні селектори
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PageInteractionService {

    @Value("${scraping.selenium.scroll.delay:10000}")
    private long scrollDelay;

    @Value("${scraping.selenium.scroll.max-attempts:20}")
    private int maxScrollAttempts;

    @Value("${scraping.selenium.scroll.max-no-new-jobs:3}")
    private int maxNoNewJobsAttempts;

    private static final String LOAD_MORE_SELECTOR = ScrapingSelectors.LOAD_MORE_BUTTON[0];
    private static final String JOB_CARD_SELECTOR = ScrapingSelectors.JOB_CARD[0];

    /**
     * Гібридний підхід: Load More + нескінченна прокрутка
     */
    public boolean loadContentWithHybridApproach(WebDriver driver) {
        log.info("🔄 Starting hybrid content loading approach...");
        
        try {
            // Крок 1: Load More кнопка (один раз)
            boolean loadMoreClicked = clickLoadMoreButtonOnce(driver);
            
            // Крок 2: Нескінченна прокрутка
            boolean scrollSuccess = scrollToLoadMore(driver);
            
            boolean success = loadMoreClicked || scrollSuccess;
            log.info("✅ Hybrid content loading completed. Load More: {}, Scroll: {}", loadMoreClicked, scrollSuccess);
            
            return success;
            
        } catch (Exception e) {
            log.error("❌ Error during hybrid content loading: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Натискає кнопку Load More один раз
     */
    private boolean clickLoadMoreButtonOnce(WebDriver driver) {
        log.info("🔘 Attempting to click Load More button once...");
        
        try {
            WebElement loadMoreButton = findLoadMoreButton(driver);
            
            if (loadMoreButton == null) {
                log.info("ℹ️ Load More button not found, skipping");
                return false;
            }
            
            if (!isButtonClickable(loadMoreButton)) {
                log.info("⚠️ Load More button is not clickable");
                return false;
            }
            
            // Скролимо до кнопки та натискаємо
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", loadMoreButton);
            sleep(1000);
            
            loadMoreButton.click();
            sleep(scrollDelay);
            
            log.info("✅ Load More button clicked successfully");
            return true;
            
        } catch (Exception e) {
            log.warn("⚠️ Error clicking Load More button: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Знаходить кнопку Load More
     */
    private WebElement findLoadMoreButton(WebDriver driver) {
        String[] loadMoreSelectors = {
            LOAD_MORE_SELECTOR,
            "button:contains('Load More')",
            "button:contains('Show More')",
            "button:contains('Load')",
            "a:contains('Load More')",
            "a:contains('Show More')",
            "[data-testid*='load-more']",
            "[data-testid*='show-more']",
            ".load-more",
            ".show-more",
            "button[class*='load']",
            "button[class*='more']",
            "a[class*='load']",
            "a[class*='more']"
        };
        
        for (String selector : loadMoreSelectors) {
            try {
                List<WebElement> buttons = driver.findElements(By.cssSelector(selector));
                if (!buttons.isEmpty()) {
                    WebElement button = buttons.get(0);
                    if (button.isDisplayed() && button.isEnabled()) {
                        log.info("✅ Load More button found with selector: '{}'", selector);
                        return button;
                    }
                }
            } catch (Exception e) {
                log.debug("⚠️ Selector '{}' failed: {}", selector, e.getMessage());
            }
        }
        return null;
    }

    /**
     * Перевіряє, чи можна натиснути кнопку
     */
    private boolean isButtonClickable(WebElement button) {
        try {
            return button.isDisplayed() && button.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Нескінченна прокрутка для завантаження контенту
     */
    private boolean scrollToLoadMore(WebDriver driver) {
        log.info("📜 Starting infinite scroll for content loading...");
        
        int previousJobCount = 0;
        int noNewJobsCount = 0;
        int scrollAttempts = 0;
        
        while (scrollAttempts < maxScrollAttempts && noNewJobsCount < maxNoNewJobsAttempts) {
            try {
                // Поточна кількість карток
                int currentJobCount = countJobCards(driver);
                
                if (currentJobCount > previousJobCount) {
                    log.info("🔄 New jobs loaded: {} -> {} (attempt {})", 
                        previousJobCount, currentJobCount, scrollAttempts + 1);
                    previousJobCount = currentJobCount;
                    noNewJobsCount = 0;
                } else {
                    noNewJobsCount++;
                    log.info("⚠️ No new jobs after scroll attempt {} (no new jobs count: {})", 
                        scrollAttempts + 1, noNewJobsCount);
                }
                
                // Скролимо вниз
                ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
                sleep(scrollDelay);
                
                scrollAttempts++;
                
            } catch (Exception e) {
                log.warn("⚠️ Error during scroll attempt {}: {}", scrollAttempts + 1, e.getMessage());
                noNewJobsCount++;
            }
        }
        
        log.info("✅ Infinite scroll completed. Total attempts: {}, Final job count: {}", 
            scrollAttempts, previousJobCount);
        
        return previousJobCount > 0;
    }

    /**
     * Рахує кількість карток вакансій на сторінці
     */
    private int countJobCards(WebDriver driver) {
        try {
            return driver.findElements(By.cssSelector(JOB_CARD_SELECTOR)).size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Спробуємо альтернативні селектори
     */
    public void tryAlternativeSelectors(WebDriver driver) {
        log.info("🔍 Testing alternative selectors...");
        
        // Отримуємо HTML сторінки для аналізу
        String pageSource = driver.getPageSource();
        log.info("📄 Page source length: {} characters", pageSource.length());
        
        // ОПТИМІЗОВАНО: Залишено тільки працюючі селектори
        String[] alternativeSelectors = {
            "div[class*='job']",
            "div[class*='position']", 
            "div[class*='vacancy']",
            "div[class*='card']",
            "div[class*='item']",
            "div[class*='listing']",
            "div[class*='posting']",
            ".job-card",
            ".position-card",
            ".vacancy-card",
            ".job-item",
            ".position-item"
        };
        
        for (String selector : alternativeSelectors) {
            try {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                if (!elements.isEmpty()) {
                    log.info("✅ Alternative selector '{}' found {} elements", selector, elements.size());
                }
            } catch (Exception e) {
                log.debug("⚠️ Alternative selector '{}' failed: {}", selector, e.getMessage());
            }
        }
    }

    /**
     * Пошук карток вакансій з кількома стратегіями
     */
    public List<WebElement> findJobCardsWithMultipleStrategies(WebDriver driver) {
        log.info("🔍 Finding job cards with multiple strategies...");
        
        // Спочатку тестуємо найбільш ймовірний селектор
        String primarySelector = "[class*='job-card']";
        try {
            List<WebElement> elements = driver.findElements(By.cssSelector(primarySelector));
            log.info("🔍 Primary selector '{}' -> found {} elements", primarySelector, elements.size());
            
            if (!elements.isEmpty()) {
                // Валідація елементів - фільтруємо неправильні
                List<WebElement> validElements = Validation.filterValidJobCards(elements);
                log.info("🔍 After validation: {} valid elements out of {} total", validElements.size(), elements.size());
                
                if (!validElements.isEmpty()) {
                    log.info("✅ Found {} valid job cards with primary selector: '{}'", validElements.size(), primarySelector);
                    return validElements;
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Primary selector '{}' failed: {}", primarySelector, e.getMessage());
        }
        
        // Спробуємо інші селектори з ScrapingSelectors.JOB_CARD
        for (int i = 0; i < ScrapingSelectors.JOB_CARD.length; i++) {
            String selector = ScrapingSelectors.JOB_CARD[i];
            
            // Пропускаємо основний селектор, який вже перевірили
            if (selector.equals(primarySelector)) {
                continue;
            }
            
            try {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                log.info("🔍 Selector {}: '{}' -> found {} elements", i + 1, selector, elements.size());
                
                if (!elements.isEmpty()) {
                    // Валідація елементів - фільтруємо неправильні
                    List<WebElement> validElements = Validation.filterValidJobCards(elements);
                    log.info("🔍 After validation: {} valid elements out of {} total", validElements.size(), elements.size());
                    
                    if (!validElements.isEmpty()) {
                        log.info("✅ Found {} valid job cards with selector: '{}'", validElements.size(), selector);
                        return validElements;
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ Selector '{}' failed: {}", selector, e.getMessage());
            }
        }
        
        // Аналіз всіх div елементів для діагностики
        List<WebElement> allDivs = driver.findElements(By.tagName("div"));
        int sampleSize = Math.min(10, allDivs.size());
        for (int i = 0; i < sampleSize; i++) {
            try {
                WebElement div = allDivs.get(i);
                String className = div.getAttribute("class");
                String dataTestId = div.getAttribute("data-testid");
                String tagName = div.getTagName();
                String text = div.getText();
                log.info("🔍 Div {}: tag='{}', class='{}', data-testid='{}', text='{}'",
                    i + 1, tagName, className, dataTestId,
                    text.length() > 50 ? text.substring(0, 50) + "..." : text);
            } catch (Exception e) {
                log.warn("⚠️ Error analyzing div {}: {}", i + 1, e.getMessage());
            }
        }
        
        // Повертаємо перші 50 div елементів для аналізу
        return allDivs.subList(0, Math.min(50, allDivs.size()));
    }

    /**
     * Пошук карток вакансій на сторінці компанії
     */
    public List<WebElement> findJobCardsOnCompanyPage(WebDriver driver) {
        List<WebElement> jobCards = new ArrayList<>();

        try {
            for (String selector : ScrapingSelectors.COMPANY_PAGE_JOBS) {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                if (!elements.isEmpty()) {
                    jobCards.addAll(elements);
                    log.info("✅ Found {} job cards with selector: {}", elements.size(), selector);
                }
            }

            // Фільтруємо неправильні елементи
            jobCards = Validation.filterValidJobCards(jobCards);

        } catch (Exception e) {
            log.warn("⚠️ Error finding job cards on company page: {}", e.getMessage());
        }

        return jobCards;
    }

    /**
     * Знаходить пряме посилання на вакансію в картці
     */
    public String findDirectJobUrl(WebElement jobCard) {
        try {
            // Стратегія 1: Шукаємо посилання за унікальним атрибутом data-testid
            try {
                WebElement specificLink = jobCard.findElement(By.cssSelector("a[data-testid='job-card-link']"));
                String url = specificLink.getAttribute("href");
                if (url != null && !url.isBlank()) {
                    log.debug("🔍 Стратегія 1: Знайдено URL за data-testid: {}", url);
                    return url;
                }
            } catch (Exception e) {
                // Стратегія 1 не спрацювала
            }

            // Стратегія 2: Перевіряємо, чи є батьківський елемент картки посиланням
            try {
                WebElement parent = jobCard.findElement(By.xpath(".."));
                if (parent != null && "a".equals(parent.getTagName())) {
                    String url = parent.getAttribute("href");
                    if (url != null && !url.isBlank()) {
                        log.debug("🔍 Стратегія 2: Знайдено URL у батьківського елемента: {}", url);
                        return url;
                    }
                }
            } catch (Exception e) {
                // Стратегія 2 не спрацювала
            }

            // Стратегія 3: Шукаємо посилання в дочірніх елементах
            try {
                List<WebElement> links = jobCard.findElements(By.tagName("a"));
                for (WebElement link : links) {
                    String url = link.getAttribute("href");
                    if (url != null && !url.isBlank() && url.contains("jobs.techstars.com")) {
                        log.debug("🔍 Стратегія 3: Знайдено URL у дочірньому посиланні: {}", url);
                        return url;
                    }
                }
            } catch (Exception e) {
                // Стратегія 3 не спрацювала
            }

            // Стратегія 4: Шукаємо посилання за класом
            try {
                List<WebElement> links = jobCard.findElements(By.cssSelector("a[class*='job'], a[class*='card'], a[class*='link']"));
                for (WebElement link : links) {
                    String url = link.getAttribute("href");
                    if (url != null && !url.isBlank()) {
                        log.debug("🔍 Стратегія 4: Знайдено URL за класом: {}", url);
                        return url;
                    }
                }
            } catch (Exception e) {
                // Стратегія 4 не спрацювала
            }

        } catch (Exception e) {
            log.warn("⚠️ Error finding direct job URL: {}", e.getMessage());
        }

        log.warn("⚠️ No direct job URL found in job card");
        return null;
    }

    /**
     * Натискає кнопку Load More (повна версія)
     */
    public void clickLoadMoreButton(WebDriver driver) {
        log.info("🔄 Looking for Load More button...");
        
        // Різні варіанти кнопки "Load More"
        String[] loadMoreSelectors = {
            LOAD_MORE_SELECTOR,
            "button:contains('Load More')",
            "button:contains('Show More')",
            "button:contains('Load')",
            "a:contains('Load More')",
            "a:contains('Show More')",
            "[data-testid*='load-more']",
            "[data-testid*='show-more']",
            ".load-more",
            ".show-more",
            "button[class*='load']",
            "button[class*='more']",
            "a[class*='load']",
            "a[class*='more']"
        };
        
        for (String selector : loadMoreSelectors) {
            try {
                List<WebElement> buttons = driver.findElements(By.cssSelector(selector));
                if (!buttons.isEmpty()) {
                    WebElement button = buttons.get(0);
                    if (button.isDisplayed() && button.isEnabled()) {
                        log.info("✅ Load More button found with selector: '{}'", selector);
                        
                        // Скролимо до кнопки перед кліком
                        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", button);
                        sleep(1000);
                        
                        button.click();
                        sleep(scrollDelay);
                        log.info("✅ Load More button clicked successfully");
                        return;
                    }
                }
            } catch (Exception e) {
                log.debug("⚠️ Selector '{}' failed: {}", selector, e.getMessage());
            }
        }
        
        // Спробуємо знайти кнопку за текстом
        try {
            List<WebElement> allButtons = driver.findElements(By.cssSelector("button, a"));
            for (WebElement button : allButtons) {
                String buttonText = button.getText().toLowerCase();
                if (buttonText.contains("load") || buttonText.contains("more") || buttonText.contains("show")) {
                    if (button.isDisplayed() && button.isEnabled()) {
                        log.info("✅ Load More button found by text: '{}'", buttonText);
                        
                        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", button);
                        sleep(1000);
                        
                        button.click();
                        sleep(scrollDelay);
                        log.info("✅ Load More button clicked successfully");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Text-based button search failed: {}", e.getMessage());
        }
        
        log.warn("⚠️ No Load More button found");
    }

    /**
     * Скролить до низу сторінки
     */
    public void scrollToBottom(WebDriver driver) {
        log.info("📜 Starting scroll to bottom process...");
        
        // Перевіряємо початкову кількість карток
        int initialJobCount = driver.findElements(By.cssSelector(JOB_CARD_SELECTOR)).size();
        log.info("🔍 Initial job cards found: {}", initialJobCount);
        
        // Спробуємо альтернативні селектори
        if (initialJobCount == 0) {
            log.warn("⚠️ Primary selector found 0 cards, trying alternatives...");
            tryAlternativeSelectors(driver);
        }
        
        // Використовуємо гібридний підхід
        loadContentWithHybridApproach(driver);
    }

    /**
     * Утиліта для затримки
     */
    public void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("⚠️ Sleep interrupted");
        }
    }
}
