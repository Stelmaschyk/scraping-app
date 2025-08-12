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
                        log.debug("✅ Load More button found with selector: '{}'", selector);
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
                ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body"
                    + ".scrollHeight);");
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
                log.warn("⚠️ Error during scroll attempt {}: {}", scrollAttempts + 1,
                    e.getMessage());
                noNewJobsCount++;
            }
        }

        log.info("✅ Scroll completed. Attempts: {}, Final count: {}", scrollAttempts,
            previousJobCount);
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
     * Пошук карток вакансій з кількома стратегіями
     */
    public List<WebElement> findJobCardsWithMultipleStrategies(WebDriver driver) {
        log.info("🔍 Finding job cards with multiple strategies...");

        // Спочатку тестуємо найбільш ймовірний селектор
        String primarySelector = "[class*='job-card']";
        try {
            List<WebElement> elements = driver.findElements(By.cssSelector(primarySelector));
            log.info("🔍 Primary selector '{}' -> found {} elements", primarySelector,
                elements.size());

            if (!elements.isEmpty()) {
                // Валідація елементів - фільтруємо неправильні
                List<WebElement> validElements = Validation.filterValidJobCards(elements);
                log.info("🔍 After validation: {} valid elements out of {} total",
                    validElements.size(), elements.size());

                if (!validElements.isEmpty()) {
                    log.info("✅ Found {} valid job cards with primary selector: '{}'",
                        validElements.size(), primarySelector);
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
                log.info("🔍 Selector {}: '{}' -> found {} elements", i + 1, selector,
                    elements.size());

                if (!elements.isEmpty()) {
                    // Валідація елементів - фільтруємо неправильні
                    List<WebElement> validElements = Validation.filterValidJobCards(elements);
                    log.info("🔍 After validation: {} valid elements out of {} total",
                        validElements.size(), elements.size());

                    if (!validElements.isEmpty()) {
                        log.info("✅ Found {} valid job cards with selector: '{}'",
                            validElements.size(), selector);
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


    /**
     * Знаходить пряме посилання на вакансію в картці
     */
    public String findDirectJobUrl(WebElement jobCard) {
        try {
            // Стратегія 1: Шукаємо посилання за унікальним атрибутом data-testid
            try {
                WebElement specificLink = jobCard.findElement(By.cssSelector("a[data-testid='job"
                    + "-card-link']"));
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
                List<WebElement> links = jobCard.findElements(By.cssSelector("a[class*='job'], "
                    + "a[class*='card'], a[class*='link']"));
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
        log.debug("🔍 Attempting to click job function filter: '{}'", jobFunction);

        try {
            // КРОК 1: Знаходимо та натискаємо на кнопку "Job function"
            WebElement jobFunctionButton = findJobFunctionButton(driver);
            if (jobFunctionButton == null) {
                log.warn("⚠️ Could not find 'Job function' button");
                return false;
            }

            log.debug("✅ Found 'Job function' button, clicking to open dropdown...");
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);",
                jobFunctionButton);
            Thread.sleep(500);

            // Додаткова перевірка, чи кнопка клікабельна
            if (!jobFunctionButton.isEnabled() || !jobFunctionButton.isDisplayed()) {
                log.warn("⚠️ Job function button is not clickable, waiting...");
                Thread.sleep(2000);
            }

            // Спробуємо клікнути кілька разів, якщо потрібно
            boolean dropdownOpened = false;
            for (int attempt = 1; attempt <= 3; attempt++) {
                log.debug("🔍 Attempt {} to click job function button...", attempt);
                try {
                    jobFunctionButton.click();
                    Thread.sleep(2000); // Чекаємо відкриття dropdown

                    // Перевіряємо, чи dropdown дійсно відкрився
                    List<WebElement> dropdownOptions = driver.findElements(By.cssSelector("div"
                        + ".sc-beqWaB.dfbUjw"));
                    if (!dropdownOptions.isEmpty()) {
                        log.debug("✅ Dropdown opened successfully on attempt {}", attempt);
                        dropdownOpened = true;
                        break;
                    } else {
                        log.debug("⚠️ Dropdown not opened on attempt {}, trying again...", attempt);
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Error clicking job function button on attempt {}: {}", attempt,
                        e.getMessage());
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

            log.debug("✅ Found job function option: '{}', clicking...",
                jobFunctionOption.getText());
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);",
                jobFunctionOption);
            Thread.sleep(500);
            jobFunctionOption.click();
            Thread.sleep(2000); // Чекаємо застосування фільтра

            // КРОК 3: Закриваємо випадаюче меню після застосування фільтра
            log.debug("🔍 Closing dropdown after applying filter '{}'...", jobFunction);
            closeJobFunctionDropdown(driver);

            // Додаткова пауза після закриття меню
            log.debug("🔍 Waiting after closing dropdown...");
            Thread.sleep(2000);

            log.debug("✅ Successfully applied job function filter: '{}'", jobFunction);
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
            List<WebElement> elements = driver.findElements(By.xpath("//div[contains(text(), 'Job"
                + " function')]"));
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
            String testIdSelector = String.format("[data-testid*='job_functions-%s']",
                jobFunction.replace(" ", "%20"));
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
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollTop = "
                    + "arguments[0].scrollHeight;", dropdownContainer);
                Thread.sleep(1000);

                // Знову шукаємо опцію після прокрутки
                List<WebElement> elements = driver.findElements(By.cssSelector("div.sc-beqWaB"
                    + ".dfbUjw"));
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
                WebElement countElement = driver.findElement(By.xpath("//div[contains(text(), "
                    + "'Showing') and contains(text(), 'jobs')]"));
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
                log.warn("⚠️ Could not find or parse the total job count element. CSS Error: {}, "
                    + "XPath Error: {}", e.getMessage(), e2.getMessage());
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
     * @param driver            WebDriver
     * @param totalJobsExpected Загальна кількість вакансій, яку потрібно завантажити.
     */
    public void loadAllAvailableJobs(WebDriver driver, int totalJobsExpected) {
        log.info("🔄 Loading jobs: expected {}", totalJobsExpected);
        if (totalJobsExpected == 0) {
            log.warn("⚠️ Expected job count is 0, skipping dynamic loading.");
            // Можна виконати один скрол про всяк випадок, якщо лічильник не знайшовся
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body"
                + ".scrollHeight);");
            sleep(scrollDelay);
            return;
        }

        int currentJobCount = 0;
        int attemptsWithNoNewJobs = 0;
        final int MAX_ATTEMPTS_WITH_NO_NEW_JOBS = 3; // Запобіжник від нескінченного циклу

        // Спочатку спробуємо кнопку "Load More" один раз
        WebElement loadMoreButton = findLoadMoreButton(driver);
        if (loadMoreButton != null && isButtonClickable(loadMoreButton)) {
            log.debug("🔘 Found 'Load More' button, clicking once...");
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();",
                    loadMoreButton);
                sleep(scrollDelay);
            } catch (Exception e) {
                log.warn("⚠️ Could not click 'Load More' button: {}", e.getMessage());
            }
        } else {
            log.debug("📜 'Load More' button not found, will use scrolling only.");
        }

        // Тепер використовуємо тільки скролінг для завантаження решти контенту
        while (currentJobCount < totalJobsExpected && attemptsWithNoNewJobs < MAX_ATTEMPTS_WITH_NO_NEW_JOBS) {
            currentJobCount = driver.findElements(By.cssSelector(JOB_CARD_SELECTOR)).size();

            // Логуємо тільки кожні 50 вакансій, щоб зменшити спам
            if (currentJobCount % 50 == 0 || currentJobCount >= totalJobsExpected) {
                log.info("... Current job count: {} / {}", currentJobCount, totalJobsExpected);
            }

            if (currentJobCount >= totalJobsExpected) {
                log.debug("✅ All expected jobs seem to be loaded.");
                break;
            }

            // Просто скролимо вниз
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body"
                + ".scrollHeight);");
            sleep(scrollDelay);

            // Перевірка, чи з'явилися нові вакансії
            int newJobCount = driver.findElements(By.cssSelector(JOB_CARD_SELECTOR)).size();
            if (newJobCount == currentJobCount) {
                attemptsWithNoNewJobs++;
                if (attemptsWithNoNewJobs == 1) {
                    log.debug("⚠️ No new jobs loaded. Attempt {} of {}.", attemptsWithNoNewJobs,
                        MAX_ATTEMPTS_WITH_NO_NEW_JOBS);
                }
            } else {
                attemptsWithNoNewJobs = 0; // Скидаємо лічильник, якщо контент завантажився
            }
        }

        log.info("✅ Loading finished. Final count: {}",
            driver.findElements(By.cssSelector(JOB_CARD_SELECTOR)).size());
    }


    public void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
