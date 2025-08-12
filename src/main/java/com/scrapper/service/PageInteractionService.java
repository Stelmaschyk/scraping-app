package com.scrapper.service;

import com.scrapper.util.ScrapingSelectors;
import com.scrapper.validation.Validation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Сервіс для взаємодії зі сторінками
 * Відповідає за Load More кнопки, прокрутку, пошук елементів та альтернативні селектори
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PageInteractionService {

    @Value("${scraping.selenium.scroll.delay:1000}")
    private long scrollDelay;

    @Value("${scraping.selenium.scroll.max-attempts:8}")
    private int maxScrollAttempts;

    @Value("${scraping.selenium.scroll.max-no-new-jobs:2}")
    private int maxNoNewJobsAttempts;

    private static final String LOAD_MORE_SELECTOR = ScrapingSelectors.LOAD_MORE_BUTTON[0];
    private static final String JOB_CARD_SELECTOR = ScrapingSelectors.JOB_CARD[0];

    /**
     * Гібридний підхід: Load More + прокрутка
     */
    public boolean loadContentWithHybridApproach(WebDriver driver, List<String> jobFunctions) {
        log.info("🔄 Starting content loading...");
        
        try {
            // Крок 1: Load More кнопка (один раз)
            clickLoadMoreButtonOnce(driver);
            
            // Крок 2: Прокрутка
            scrollToLoadMore(driver, jobFunctions);
            
            return true;
            
        } catch (Exception e) {
            log.error("❌ Error during content loading: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Натискає кнопку Load More один раз (ОНОВЛЕНА, БІЛЬШ НАДІЙНА ВЕРСІЯ)
     */
    private boolean clickLoadMoreButtonOnce(WebDriver driver) {
        log.info("🔘 Attempting to click Load More button...");
        
        try {
            WebElement loadMoreButton = findLoadMoreButton(driver);
            
            if (loadMoreButton == null) {
                log.info("ℹ️ Load More button not found, skipping");
                return false;
            }

            // КРОК 1: Використовуємо WebDriverWait для очікування клікабельності
            // Чекаємо до 10 секунд, поки кнопка не стане доступною для кліку
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.elementToBeClickable(loadMoreButton));
            
            log.info("✅ Load More button is clickable. Attempting to click via JavaScript.");

            // КРОК 2: Використовуємо JavascriptExecutor для надійного кліку
            // Цей метод спрацює, навіть якщо кнопка чимось перекрита
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", loadMoreButton); // Прокручуємо до кнопки
            sleep(500); // Невелика пауза після скролу
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loadMoreButton);
            
            sleep(2000); // Залишаємо затримку, щоб контент встиг завантажитись
            
            log.info("✅ Load More button successfully clicked via JavaScript.");
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
     * Закриває випадаюче меню job function
     */
    public void closeJobFunctionDropdown(WebDriver driver) {
        log.info("🔍 Attempting to close job function dropdown...");
        try {
            // Клікаємо поза межами випадаючого меню, щоб закрити його
            WebElement body = driver.findElement(By.tagName("body"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", body);
            sleep(1000);
            log.info("✅ Job function dropdown closed successfully");
        } catch (Exception e) {
            log.warn("⚠️ Could not close job function dropdown: {}", e.getMessage());
        }
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
    private boolean scrollToLoadMore(WebDriver driver, List<String> jobFunctions) {
        log.info("📜 Starting scroll for content loading...");
        
        int previousJobCount = countJobCardsWithFilter(driver, jobFunctions);
        int noNewJobsCount = 0;
        int scrollAttempts = 0;
        
        while (scrollAttempts < maxScrollAttempts && noNewJobsCount < maxNoNewJobsAttempts) {
            try {
                // Скролимо вниз
                ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
                sleep(scrollDelay);
                
                // Перевіряємо нові картки з фільтрацією
                int currentJobCount = countJobCardsWithFilter(driver, jobFunctions);
                
                if (currentJobCount > previousJobCount) {
                    log.info("🔄 Jobs loaded: {} -> {} (attempt {})", 
                        previousJobCount, currentJobCount, scrollAttempts + 1);
                    previousJobCount = currentJobCount;
                    noNewJobsCount = 0;
                } else {
                    noNewJobsCount++;
                }
                
                scrollAttempts++;
                
            } catch (Exception e) {
                log.warn("⚠️ Error during scroll attempt {}: {}", scrollAttempts + 1, e.getMessage());
                noNewJobsCount++;
            }
        }
        
        log.info("✅ Scroll completed. Attempts: {}, Final count: {}", scrollAttempts, previousJobCount);
        return previousJobCount > 0;
    }



    /**
     * Рахує кількість карток вакансій з фільтрацією за job functions
     */
    private int countJobCardsWithFilter(WebDriver driver, List<String> jobFunctions) {
        try {
            List<WebElement> cards = driver.findElements(By.cssSelector(JOB_CARD_SELECTOR));
            
            if (jobFunctions == null || jobFunctions.isEmpty()) {
                return cards.size();
            }
            
            int filteredCount = 0;
            for (WebElement card : cards) {
                String cardText = card.getText().toLowerCase();
                boolean hasMatchingFunction = jobFunctions.stream()
                    .anyMatch(function -> cardText.contains(function.toLowerCase()));
                
                if (hasMatchingFunction) {
                    filteredCount++;
                }
            }
            
            return filteredCount;
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
     * Натискає на фільтр job function (наприклад, IT)
     * Крок 1: Натискає на кнопку "Job function" щоб відкрити dropdown
     * Крок 2: Вибирає потрібну опцію з dropdown
     */
    public boolean clickJobFunctionFilter(WebDriver driver, String jobFunction) {
        log.info("🔍 Attempting to click job function filter: '{}'", jobFunction);
        
        try {
            // КРОК 1: Знаходимо та натискаємо на кнопку "Job function"
            WebElement jobFunctionButton = findJobFunctionButton(driver);
            if (jobFunctionButton == null) {
                log.warn("⚠️ Could not find 'Job function' button");
                return false;
            }
            
            log.info("✅ Found 'Job function' button, clicking to open dropdown...");
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", jobFunctionButton);
            Thread.sleep(500);
            
            // Додаткова перевірка, чи кнопка клікабельна
            if (!jobFunctionButton.isEnabled() || !jobFunctionButton.isDisplayed()) {
                log.warn("⚠️ Job function button is not clickable, waiting...");
                Thread.sleep(2000);
            }
            
            // Спробуємо клікнути кілька разів, якщо потрібно
            boolean dropdownOpened = false;
            for (int attempt = 1; attempt <= 3; attempt++) {
                log.info("🔍 Attempt {} to click job function button...", attempt);
                try {
                    jobFunctionButton.click();
                    Thread.sleep(2000); // Чекаємо відкриття dropdown
                    
                    // Перевіряємо, чи dropdown дійсно відкрився
                    List<WebElement> dropdownOptions = driver.findElements(By.cssSelector("div.sc-beqWaB.dfbUjw"));
                    if (!dropdownOptions.isEmpty()) {
                        log.info("✅ Dropdown opened successfully on attempt {}", attempt);
                        dropdownOpened = true;
                        break;
                    } else {
                        log.warn("⚠️ Dropdown not opened on attempt {}, trying again...", attempt);
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Error clicking job function button on attempt {}: {}", attempt, e.getMessage());
                    Thread.sleep(1000);
                }
            }
            
            if (!dropdownOpened) {
                log.error("❌ Failed to open dropdown after 3 attempts");
                return false;
            }
            
            // КРОК 2: Знаходимо та натискаємо на потрібну опцію в dropdown
            WebElement jobFunctionOption = findJobFunctionOption(driver, jobFunction);
            if (jobFunctionOption == null) {
                log.warn("⚠️ Could not find job function option: '{}'", jobFunction);
                return false;
            }
            
            log.info("✅ Found job function option: '{}', clicking...", jobFunctionOption.getText());
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", jobFunctionOption);
            Thread.sleep(500);
            jobFunctionOption.click();
            Thread.sleep(2000); // Чекаємо застосування фільтра
            
            // КРОК 3: Закриваємо випадаюче меню після застосування фільтра
            log.info("🔍 Closing dropdown after applying filter '{}'...", jobFunction);
            closeJobFunctionDropdown(driver);
            
            // Додаткова пауза після закриття меню
            log.info("🔍 Waiting after closing dropdown...");
            Thread.sleep(2000);
            
            log.info("✅ Successfully applied job function filter: '{}'", jobFunction);
            return true;
            
        } catch (Exception e) {
            log.error("❌ Error clicking job function filter '{}': {}", jobFunction, e.getMessage());
            return false;
        }
    }
    
    /**
     * Знаходить кнопку "Job function"
     */
    private WebElement findJobFunctionButton(WebDriver driver) {
        // 1. Спочатку спробуємо точний селектор
        try {
            List<WebElement> elements = driver.findElements(By.cssSelector("div.sc-beqWaB.fmYNJF"));
            for (WebElement element : elements) {
                if (element.getText().contains("Job function") && element.isDisplayed()) {
                    return element;
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Exact selector failed: {}", e.getMessage());
        }
        
        // 2. Спробуємо XPath
        try {
            List<WebElement> elements = driver.findElements(By.xpath("//div[contains(text(), 'Job function')]"));
            for (WebElement element : elements) {
                if (element.isDisplayed()) {
                    return element;
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ XPath selector failed: {}", e.getMessage());
        }
        
        // 3. Спробуємо загальні селектори
        for (String selector : ScrapingSelectors.JOB_FUNCTION_BUTTON) {
            try {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                for (WebElement element : elements) {
                    if (element.getText().contains("Job function") && element.isDisplayed()) {
                        return element;
                    }
                }
            } catch (Exception e) {
                log.debug("⚠️ Selector '{}' failed: {}", selector, e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * Знаходить опцію job function в dropdown
     */
    private WebElement findJobFunctionOption(WebDriver driver, String jobFunction) {
        // Діагностика: виводимо всі доступні опції
        log.info("🔍 Looking for job function option: '{}'", jobFunction);
        
        // 1. Спочатку спробуємо точний селектор
        try {
            List<WebElement> elements = driver.findElements(By.cssSelector("div.sc-beqWaB.dfbUjw"));
            log.info("🔍 Found {} elements with selector 'div.sc-beqWaB.dfbUjw'", elements.size());
            
            for (WebElement element : elements) {
                String elementText = element.getText();
                log.info("🔍 Available option: '{}'", elementText);
                if (elementText.equalsIgnoreCase(jobFunction) && element.isDisplayed()) {
                    return element;
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Exact selector failed: {}", e.getMessage());
        }
        
        // 2. Спробуємо data-testid селектор
        try {
            String testIdSelector = String.format("[data-testid*='job_functions-%s']", jobFunction.replace(" ", "%20"));
            List<WebElement> elements = driver.findElements(By.cssSelector(testIdSelector));
            for (WebElement element : elements) {
                if (element.isDisplayed()) {
                    return element;
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Data-testid selector failed: {}", e.getMessage());
        }
        
        // 3. Спробуємо XPath
        try {
            String xpathSelector = String.format("//div[contains(text(), '%s')]", jobFunction);
            List<WebElement> elements = driver.findElements(By.xpath(xpathSelector));
            for (WebElement element : elements) {
                if (element.isDisplayed()) {
                    return element;
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ XPath selector failed: {}", e.getMessage());
        }
        
        // 4. Спробуємо загальні селектори
        for (String selector : ScrapingSelectors.JOB_FUNCTION_OPTIONS) {
            try {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                for (WebElement element : elements) {
                    String elementText = element.getText();
                    if (elementText.equalsIgnoreCase(jobFunction) && element.isDisplayed()) {
                        return element;
                    }
                }
            } catch (Exception e) {
                log.debug("⚠️ Selector '{}' failed: {}", selector, e.getMessage());
            }
        }
        
        // 5. Якщо не знайдено, спробуємо прокрутити dropdown
        log.info("🔍 Option '{}' not found, attempting to scroll dropdown...", jobFunction);
        try {
            // Знаходимо dropdown контейнер (спробуємо різні селектори)
            WebElement dropdownContainer = null;
            String[] containerSelectors = {
                "div[role='listbox']",
                "div[class*='dropdown']",
                "div[class*='menu']",
                "div[class*='list']",
                "div.sc-beqWaB",
                "ul[role='listbox']",
                "ul[class*='dropdown']"
            };
            
            for (String selector : containerSelectors) {
                try {
                    List<WebElement> containers = driver.findElements(By.cssSelector(selector));
                    for (WebElement container : containers) {
                        if (container.isDisplayed() && container.getSize().height > 100) {
                            dropdownContainer = container;
                            log.info("🔍 Found dropdown container with selector: {}", selector);
                            break;
                        }
                    }
                    if (dropdownContainer != null) break;
                } catch (Exception e) {
                    log.debug("⚠️ Container selector '{}' failed: {}", selector, e.getMessage());
                }
            }
            
            if (dropdownContainer != null) {
                log.info("🔍 Found dropdown container, scrolling down...");
                // Прокручуємо dropdown вниз
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollTop = arguments[0].scrollHeight;", dropdownContainer);
                Thread.sleep(1000);
                
                // Знову шукаємо опцію після прокрутки
                List<WebElement> elements = driver.findElements(By.cssSelector("div.sc-beqWaB.dfbUjw"));
                log.info("🔍 After scrolling, found {} elements", elements.size());
                
                for (WebElement element : elements) {
                    String elementText = element.getText();
                    log.info("🔍 Checking element after scroll: '{}'", elementText);
                    if (elementText.equalsIgnoreCase(jobFunction) && element.isDisplayed()) {
                        log.info("✅ Found job function option after scrolling: '{}'", elementText);
                        return element;
                    }
                }
            } else {
                log.info("🔍 Could not find dropdown container for scrolling");
            }
        } catch (Exception e) {
            log.debug("⚠️ Scrolling dropdown failed: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Натискає кнопку Load More (повна версія)
     */
    public void clickLoadMoreButton(WebDriver driver) {
        log.info("🔄 Looking for Load More button...");
        
        // Чекаємо трохи, щоб сторінка завантажилася після застосування фільтра
        sleep(3000);
        
        // Різні варіанти кнопки "Load More"
        String[] loadMoreSelectors = {
            LOAD_MORE_SELECTOR,
            "button:contains('Load More')",
            "button:contains('Show More')",
            "button:contains('Load')",
            "button:contains('more')",
            "a:contains('Load More')",
            "a:contains('Show More')",
            "[data-testid*='load-more']",
            "[data-testid*='show-more']",
            "[data-testid*='load']",
            "[data-testid*='more']",
            ".load-more",
            ".show-more",
            "button[class*='load']",
            "button[class*='more']",
            "a[class*='load']",
            "a[class*='more']",
            "div[class*='load']",
            "div[class*='more']",
            "span[class*='load']",
            "span[class*='more']"
        };
        
        // Крок 1: Спробуємо CSS селектори
        for (String selector : loadMoreSelectors) {
            try {
                List<WebElement> buttons = driver.findElements(By.cssSelector(selector));
                log.info("🔍 Selector '{}' found {} elements", selector, buttons.size());
                
                for (WebElement button : buttons) {
                    try {
                        boolean isDisplayed = button.isDisplayed();
                        boolean isEnabled = button.isEnabled();
                        String buttonText = button.getText();
                        String buttonDataTestId = button.getAttribute("data-testid");
                        String buttonDataLoading = button.getAttribute("data-loading");
                        
                        log.info("🔍 Button: text='{}', data-testid='{}', data-loading='{}', displayed={}, enabled={}", 
                                buttonText, buttonDataTestId, buttonDataLoading, isDisplayed, isEnabled);
                        
                        if (isDisplayed && isEnabled) {
                            log.info("✅ Load More button found with selector '{}': '{}'", selector, buttonText);
                            
                            try {
                                // КРОК 1: Використовуємо WebDriverWait для очікування клікабельності
                                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                                wait.until(ExpectedConditions.elementToBeClickable(button));
                                
                                // КРОК 2: Скролимо до кнопки та клікаємо через JavaScript
                                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", button);
                                sleep(500);
                                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
                                sleep(scrollDelay);
                                log.info("✅ Load More button clicked successfully via JavaScript");
                                return;
                            } catch (Exception e) {
                                log.warn("⚠️ JavaScript click failed, trying regular click: {}", e.getMessage());
                                // Fallback до звичайного кліку
                                button.click();
                                sleep(scrollDelay);
                                log.info("✅ Load More button clicked successfully via regular click");
                                return;
                            }
                        }
                    } catch (Exception e) {
                        log.debug("⚠️ Error checking button: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.debug("⚠️ Selector '{}' failed: {}", selector, e.getMessage());
            }
        }
        
        // Крок 2: Спробуємо XPath селектори
        String[] xpathSelectors = {
            "//button[contains(text(), 'Load More')]",
            "//button[contains(text(), 'Show More')]",
            "//button[contains(text(), 'Load')]",
            "//button[contains(text(), 'more')]",
            "//a[contains(text(), 'Load More')]",
            "//a[contains(text(), 'Show More')]",
            "//div[contains(text(), 'Load More')]",
            "//div[contains(text(), 'Show More')]",
            "//span[contains(text(), 'Load More')]",
            "//span[contains(text(), 'Show More')]"
        };
        
        for (String xpathSelector : xpathSelectors) {
            try {
                List<WebElement> buttons = driver.findElements(By.xpath(xpathSelector));
                for (WebElement button : buttons) {
                    if (button.isDisplayed() && button.isEnabled()) {
                        String buttonText = button.getText();
                        log.info("✅ Load More button found with XPath '{}': '{}'", xpathSelector, buttonText);
                        
                        try {
                            // КРОК 1: Використовуємо WebDriverWait для очікування клікабельності
                            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                            wait.until(ExpectedConditions.elementToBeClickable(button));
                            
                            // КРОК 2: Скролимо до кнопки та клікаємо через JavaScript
                            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", button);
                            sleep(500);
                            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
                            sleep(scrollDelay);
                            log.info("✅ Load More button clicked successfully via JavaScript (XPath)");
                            return;
                        } catch (Exception e) {
                            log.warn("⚠️ JavaScript click failed for XPath, trying regular click: {}", e.getMessage());
                            // Fallback до звичайного кліку
                            button.click();
                            sleep(scrollDelay);
                            log.info("✅ Load More button clicked successfully via regular click (XPath)");
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("⚠️ XPath selector '{}' failed: {}", xpathSelector, e.getMessage());
            }
        }
        
        // Крок 3: Спробуємо знайти кнопку за текстом серед всіх елементів
        try {
            List<WebElement> allElements = driver.findElements(By.cssSelector("button, a, div, span"));
            for (WebElement element : allElements) {
                try {
                    String elementText = element.getText().toLowerCase();
                    if ((elementText.contains("load") || elementText.contains("more") || elementText.contains("show")) 
                        && element.isDisplayed() && element.isEnabled()) {
                        
                        log.info("✅ Load More button found by text: '{}'", element.getText());
                        
                        try {
                            // КРОК 1: Використовуємо WebDriverWait для очікування клікабельності
                            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                            wait.until(ExpectedConditions.elementToBeClickable(element));
                            
                            // КРОК 2: Скролимо до кнопки та клікаємо через JavaScript
                            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
                            sleep(500);
                            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                            sleep(scrollDelay);
                            log.info("✅ Load More button clicked successfully via JavaScript (text search)");
                            return;
                        } catch (Exception e) {
                            log.warn("⚠️ JavaScript click failed for text search, trying regular click: {}", e.getMessage());
                            // Fallback до звичайного кліку
                            element.click();
                            sleep(scrollDelay);
                            log.info("✅ Load More button clicked successfully via regular click (text search)");
                            return;
                        }
                    }
                } catch (Exception e) {
                    // Ігноруємо помилки для окремих елементів
                    continue;
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Text-based button search failed: {}", e.getMessage());
        }
        
        log.warn("⚠️ No Load More button found");
        
        // Додаткова діагностика: виводимо всі кнопки на сторінці
        try {
            List<WebElement> allButtons = driver.findElements(By.cssSelector("button"));
            log.info("🔍 Found {} buttons on page:", allButtons.size());
            for (int i = 0; i < Math.min(allButtons.size(), 10); i++) {
                WebElement button = allButtons.get(i);
                try {
                    String buttonText = button.getText();
                    boolean isDisplayed = button.isDisplayed();
                    boolean isEnabled = button.isEnabled();
                    log.info("   Button {}: '{}' (displayed: {}, enabled: {})", i + 1, buttonText, isDisplayed, isEnabled);
                } catch (Exception e) {
                    log.info("   Button {}: Error reading text", i + 1);
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Button diagnostics failed: {}", e.getMessage());
        }
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
        
        // Використовуємо гібридний підхід (без фільтрації для загального випадку)
        loadContentWithHybridApproach(driver, null);
    }

    /**
     * Знаходить на сторінці текст "Showing X jobs" і витягує кількість X.
     * Це потрібно для того, щоб знати, скільки всього вакансій очікувати після завантаження.
     */
    public int getTotalJobCountFromTextAfterFiltering(WebDriver driver) {
        log.info("📊 Attempting to extract total job count from page text...");
        try {
            // Спочатку спробуємо точний селектор з класом
            WebElement countElement = driver.findElement(By.cssSelector("div.sc-beqWaB.eJrfpP"));
            String text = countElement.getText(); // Отримуємо текст, наприклад, "Showing 225 jobs"
            log.info("📊 Found element with text: '{}'", text);

            // Використовуємо регулярний вираз для витягнення першого числа з тексту
            Pattern pattern = Pattern.compile("\\d+");
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                int totalJobs = Integer.parseInt(matcher.group(0));
                log.info("✅ Found total declared jobs: {}", totalJobs);
                return totalJobs;
            } else {
                log.warn("⚠️ Could not find a number in the text: '{}'", text);
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not find element with CSS selector, trying XPath...");
            try {
                // Альтернативний XPath селектор
                WebElement countElement = driver.findElement(By.xpath("//div[contains(text(), 'Showing') and contains(text(), 'jobs')]"));
                String text = countElement.getText();
                log.info("📊 Found element with XPath, text: '{}'", text);

                Pattern pattern = Pattern.compile("\\d+");
                Matcher matcher = pattern.matcher(text);

                if (matcher.find()) {
                    int totalJobs = Integer.parseInt(matcher.group(0));
                    log.info("✅ Found total declared jobs: {}", totalJobs);
                    return totalJobs;
                }
            } catch (Exception e2) {
                log.warn("⚠️ Could not find or parse the total job count element. CSS Error: {}, XPath Error: {}", e.getMessage(), e2.getMessage());
            }
        }
        // Повертаємо 0, якщо не вдалося знайти або розпарсити
        return 0;
    }

    /**
     * Основний метод для завантаження контенту.
     * Динамічно завантажує всі вакансії, орієнтуючись на загальну кількість,
     * заявлену на сторінці після застосування фільтрів.
     *
     * @param driver WebDriver
     * @param totalJobsExpected Загальна кількість вакансій, яку потрібно завантажити.
     */
    public void loadAllAvailableJobs(WebDriver driver, int totalJobsExpected) {
        log.info("🔄 Starting dynamic content loading. Expected jobs: {}", totalJobsExpected);
        if (totalJobsExpected == 0) {
            log.warn("⚠️ Expected job count is 0, skipping dynamic loading.");
            // Можна виконати один скрол про всяк випадок, якщо лічильник не знайшовся
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
            sleep(scrollDelay);
            return;
        }

        int currentJobCount = 0;
        int attemptsWithNoNewJobs = 0;
        final int MAX_ATTEMPTS_WITH_NO_NEW_JOBS = 3; // Запобіжник від нескінченного циклу

        // Спочатку спробуємо кнопку "Load More" один раз
        WebElement loadMoreButton = findLoadMoreButton(driver);
        if (loadMoreButton != null && isButtonClickable(loadMoreButton)) {
            log.info("🔘 Found 'Load More' button, clicking once...");
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loadMoreButton);
                sleep(scrollDelay);
            } catch (Exception e) {
                log.warn("⚠️ Could not click 'Load More' button: {}", e.getMessage());
            }
        } else {
            log.info("📜 'Load More' button not found, will use scrolling only.");
        }

        // Тепер використовуємо тільки скролінг для завантаження решти контенту
        while (currentJobCount < totalJobsExpected && attemptsWithNoNewJobs < MAX_ATTEMPTS_WITH_NO_NEW_JOBS) {
            currentJobCount = driver.findElements(By.cssSelector(JOB_CARD_SELECTOR)).size();
            
            // Логуємо тільки кожні 20 вакансій, щоб зменшити спам
            if (currentJobCount % 20 == 0 || currentJobCount >= totalJobsExpected) {
                log.info("... Current job count: {} / {}", currentJobCount, totalJobsExpected);
            }

            if (currentJobCount >= totalJobsExpected) {
                log.info("✅ All expected jobs seem to be loaded.");
                break;
            }
            
            // Просто скролимо вниз
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
            sleep(scrollDelay);

            // Перевірка, чи з'явилися нові вакансії
            int newJobCount = driver.findElements(By.cssSelector(JOB_CARD_SELECTOR)).size();
            if (newJobCount == currentJobCount) {
                attemptsWithNoNewJobs++;
                if (attemptsWithNoNewJobs == 1) {
                    log.warn("⚠️ No new jobs loaded. Attempt {} of {}.", attemptsWithNoNewJobs, MAX_ATTEMPTS_WITH_NO_NEW_JOBS);
                }
            } else {
                attemptsWithNoNewJobs = 0; // Скидаємо лічильник, якщо контент завантажився
            }
        }

        log.info("✅ Content loading finished. Final job card count: {}", driver.findElements(By.cssSelector(JOB_CARD_SELECTOR)).size());
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
