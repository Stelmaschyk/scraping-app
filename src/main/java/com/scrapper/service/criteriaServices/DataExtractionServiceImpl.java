package com.scrapper.service.criteriaServices;

import com.scrapper.util.ScrapingSelectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ✅ РЕАЛІЗАЦІЯ: Єдиний сервіс для витягування даних з різних джерел
 * 
 * Цей сервіс усуває дублювання логіки між методами extract*FromCard() та extract*FromDetailPage()
 * і надає універсальні методи для обох джерел даних.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataExtractionServiceImpl implements DataExtractionService {

    private final DateParsingService dateParsingService;

    // ✅ УТИЛІТНІ МЕТОДИ ДЛЯ РОБОТИ З ЕЛЕМЕНТАМИ
    
    /**
     * Отримує текст елемента за селектором
     */
    private String getElementText(WebElement root, String selector) {
        try {
            log.debug("🔍 Searching for element with selector: '{}'", selector);
            WebElement element = root.findElement(By.cssSelector(selector));
            String text = element.getText();
            log.debug("🔍 Found element with selector '{}', text: '{}'", selector, text);
            return text;
        } catch (Exception e) {
            log.debug("⚠️ Element not found with selector '{}': {}", selector, e.getMessage());
            return null;
        }
    }

    /**
     * Отримує атрибут елемента за селектором
     */
    private String getElementAttribute(WebElement root, String selector, String attribute) {
        try {
            log.debug("🔍 Searching for element with selector: '{}' and attribute: '{}'", selector, attribute);
            WebElement element = root.findElement(By.cssSelector(selector));
            String value = element.getAttribute(attribute);
            log.debug("🔍 Found element with selector '{}', attribute '{}': '{}'", selector, attribute, value);
            return value;
        } catch (Exception e) {
            log.debug("⚠️ Element not found with selector '{}': {}", selector, e.getMessage());
            return null;
        }
    }

    /**
     * Отримує текст елемента за селектором (для WebDriver)
     */
    private String getElementText(WebDriver driver, String selector) {
        try {
            log.debug("🔍 Searching for element with selector: '{}'", selector);
            WebElement element = driver.findElement(By.cssSelector(selector));
            String text = element.getText();
            log.debug("🔍 Found element with selector '{}', text: '{}'", selector, text);
            return text;
        } catch (Exception e) {
            log.debug("⚠️ Element not found with selector '{}': {}", selector, e.getMessage());
            return null;
        }
    }

    /**
     * Отримує атрибут елемента за селектором (для WebDriver)
     */
    private String getElementAttribute(WebDriver driver, String selector, String attribute) {
        try {
            log.debug("🔍 Searching for element with selector: '{}' and attribute: '{}'", selector, attribute);
            WebElement element = driver.findElement(By.cssSelector(selector));
            String value = element.getAttribute(attribute);
            log.debug("🔍 Found element with selector '{}', attribute '{}': '{}'", selector, attribute, value);
            return value;
        } catch (Exception e) {
            log.debug("⚠️ Element not found with selector '{}': {}", selector, e.getMessage());
            return null;
        }
    }

    // ✅ РЕАЛІЗАЦІЯ МЕТОДІВ ІНТЕРФЕЙСУ

    @Override
    public List<String> extractTags(WebElement source) {
        List<String> tags = new ArrayList<>();
        try {
            // Шукаємо всі елементи з data-testid="tag"
            List<WebElement> tagElements = source.findElements(By.cssSelector("[data-testid='tag']"));
            
            for (WebElement tagElement : tagElements) {
                try {
                    String tagText = tagElement.getText().trim();
                    if (!tagText.isEmpty()) {
                        tags.add(tagText);
                        log.debug("🏷️ Found tag: '{}'", tagText);
                    }
                } catch (Exception e) {
                    log.debug("⚠️ Error extracting tag text: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Error searching for tags: {}", e.getMessage());
        }
        
        log.debug("🏷️ Extracted {} tags from element", tags.size());
        return tags;
    }

    @Override
    public List<String> extractTags(WebDriver source) {
        List<String> tags = new ArrayList<>();
        try {
            for (String selector : ScrapingSelectors.JOB_DETAIL_PAGE) {
                if (selector.contains("data-testid=tag")) {
                    List<WebElement> elements = source.findElements(By.cssSelector(selector));
                    for (WebElement element : elements) {
                        String tag = element.getText().trim();
                        if (!tag.isEmpty()) {
                            tags.add(tag);
                        }
                    }
                    if (!tags.isEmpty()) {
                        log.info("✅ Extracted {} tags from detail page: {}", tags.size(), tags);
                        return tags;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Error extracting tags from detail page: {}", e.getMessage());
        }
        return tags;
    }

    @Override
    public String extractLocation(WebElement source) {
        try {
            // Спочатку шукаємо в meta тегах
            List<WebElement> metaElements = source.findElements(By.cssSelector("meta[itemprop='address']"));
            if (!metaElements.isEmpty()) {
                String location = metaElements.get(0).getAttribute("content");
                if (location != null && !location.trim().isEmpty()) {
                    log.debug("📍 Found location in meta: '{}'", location);
                    return location.trim();
                }
            }
            
            // Якщо meta не знайдено, шукаємо в звичайних елементах
            String location = getElementText(source, ScrapingSelectors.LOCATION[0]);
            if (location != null && !location.trim().isEmpty()) {
                log.debug("📍 Found location in element: '{}'", location);
                return location.trim();
            }
            
        } catch (Exception e) {
            log.debug("⚠️ Error extracting location: {}", e.getMessage());
        }
        
        log.debug("📍 No location found in element");
        return null;
    }

    @Override
    public String extractLocation(WebDriver source) {
        try {
            // Спочатку шукаємо в meta тегах
            List<WebElement> metaElements = source.findElements(By.cssSelector("meta[itemprop='address']"));
            if (!metaElements.isEmpty()) {
                String location = metaElements.get(0).getAttribute("content");
                if (location != null && !location.trim().isEmpty()) {
                    log.info("✅ Extracted location from meta tag: {}", location);
                    return location.trim();
                }
            }
            
            // Потім шукаємо в div елементах
            for (String selector : ScrapingSelectors.JOB_DETAIL_PAGE) {
                if (selector.contains("sc-beqWaB")) {
                    List<WebElement> elements = source.findElements(By.cssSelector(selector));
                    for (WebElement element : elements) {
                        String text = element.getText().trim();
                        if (text.contains(",") && (text.contains("USA") || text.contains("Remote") || text.contains("India"))) {
                            log.info("✅ Extracted location from div: {}", text);
                            return text;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Error extracting location from detail page: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public LocalDateTime extractPostedDate(WebElement source) {
        try {
            // Шукаємо тільки в meta тегах з itemprop="datePosted"
            List<WebElement> metaElements = source.findElements(By.cssSelector("meta[itemprop='datePosted']"));
            if (!metaElements.isEmpty()) {
                String dateStr = metaElements.get(0).getAttribute("content");
                if (dateStr != null && !dateStr.trim().isEmpty()) {
                    // ✅ ВИПРАВЛЕНО: Використовуємо DateParsingService для парсингу дати
                    LocalDateTime date = dateParsingService.parseMetaDate(dateStr);
                    if (date != null) {
                        log.debug("✅ Extracted posted date from meta tag: '{}' -> {} (Unix: {})", 
                                dateStr, date, date.toEpochSecond(java.time.ZoneOffset.UTC));
                        return date;
                    }
                }
            }
            
        } catch (Exception e) {
            log.debug("⚠️ Error extracting posted date: {}", e.getMessage());
        }
        
        log.debug("📅 No posted date found in meta[itemprop='datePosted']");
        return null;
    }

    @Override
    public LocalDateTime extractPostedDate(WebDriver source) {
        try {
            // Шукаємо тільки в meta тегах з itemprop="datePosted"
            List<WebElement> metaElements = source.findElements(By.cssSelector("meta[itemprop='datePosted']"));
            if (!metaElements.isEmpty()) {
                String dateStr = metaElements.get(0).getAttribute("content");
                if (dateStr != null && !dateStr.trim().isEmpty()) {
                    // ✅ ВИПРАВЛЕНО: Використовуємо DateParsingService для парсингу дати
                    LocalDateTime date = dateParsingService.parseMetaDate(dateStr);
                    if (date != null) {
                        log.debug("✅ Extracted posted date from detail page meta tag: '{}' -> {} (Unix: {})", 
                                dateStr, date, date.toEpochSecond(java.time.ZoneOffset.UTC));
                        return date;
                    }
                }
            }
            
        } catch (Exception e) {
            log.debug("⚠️ Error extracting posted date from detail page: {}", e.getMessage());
        }
        
        log.debug("📅 No posted date found in detail page meta[itemprop='datePosted']");
        return null;
    }

    @Override
    public String extractLogoUrl(WebElement source) {
        try {
            // Шукаємо в img елементах
            List<WebElement> imgElements = source.findElements(By.cssSelector("img"));
            for (WebElement img : imgElements) {
                String src = img.getAttribute("src");
                String alt = img.getAttribute("alt");
                
                if (src != null && !src.trim().isEmpty() && 
                    (alt != null && (alt.toLowerCase().contains("logo") || alt.toLowerCase().contains("company")))) {
                    log.debug("🖼️ Found logo URL: '{}'", src);
                    return src.trim();
                }
            }
            
            // Шукаємо за селекторами
            String logoUrl = getElementAttribute(source, ScrapingSelectors.ORG_LOGO[0], "src");
            if (logoUrl != null && !logoUrl.trim().isEmpty()) {
                log.debug("🖼️ Found logo URL using selector: '{}'", logoUrl);
                return logoUrl.trim();
            }
            
        } catch (Exception e) {
            log.debug("⚠️ Error extracting logo URL: {}", e.getMessage());
        }
        
        log.debug("🖼️ No logo URL found");
        return null;
    }

    @Override
    public String extractCompanyName(WebElement source) {
        try {
            // Шукаємо за селекторами
            String companyName = getElementText(source, ScrapingSelectors.ORG_NAME[0]);
            if (companyName != null && !companyName.trim().isEmpty()) {
                log.debug("🏢 Found company name using selector: '{}'", companyName);
                return companyName.trim();
            }
            
            // Шукаємо в meta тегах
            List<WebElement> metaElements = source.findElements(By.cssSelector("meta[itemprop='name']"));
            if (!metaElements.isEmpty()) {
                String content = metaElements.get(0).getAttribute("content");
                if (content != null && !content.trim().isEmpty()) {
                    log.debug("🏢 Found company name in meta: '{}'", content);
                    return content.trim();
                }
            }
            
        } catch (Exception e) {
            log.debug("⚠️ Error extracting company name: {}", e.getMessage());
        }
        
        log.debug("🏢 No company name found");
        return null;
    }

    @Override
    public String extractCompanyName(WebDriver source) {
        try {
            for (String selector : ScrapingSelectors.JOB_DETAIL_PAGE) {
                if (selector.contains("itemprop='name'")) {
                    List<WebElement> elements = source.findElements(By.cssSelector(selector));
                    if (!elements.isEmpty()) {
                        String companyName = elements.get(0).getText().trim();
                        log.info("✅ Extracted company name from detail page: {}", companyName);
                        return companyName;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Error extracting company name from detail page: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public String extractTitle(WebElement source) {
        log.debug("🔍 Starting title extraction...");
        
        // Стратегія 1: Шукаємо за звичайними селекторами
        String title = getElementText(source, ScrapingSelectors.JOB_TITLE[0]);
        log.debug("🔍 Strategy 1 - JOB_TITLE selector result: '{}'", title);
        
        if (title != null && !title.trim().isEmpty()) {
            log.debug("💼 Found title using JOB_TITLE selector: '{}'", title.trim());
            return title.trim();
        }
        
        // Стратегія 2: Шукаємо за data-testid="job-title"
        try {
            log.debug("🔍 Strategy 2 - Searching for data-testid='job-title'...");
            WebElement titleElement = source.findElement(By.cssSelector("[data-testid='job-title']"));
            String text = titleElement.getText();
            log.debug("🔍 data-testid='job-title' text: '{}'", text);
            
            if (text != null && !text.trim().isEmpty()) {
                log.debug("💼 Found title using data-testid='job-title': '{}'", text.trim());
                return text.trim();
            }
        } catch (Exception e) {
            log.debug("⚠️ Strategy 2 failed: {}", e.getMessage());
        }
        
        // Стратегія 3: Шукаємо за itemprop="title"
        try {
            log.debug("🔍 Strategy 3 - Searching for [itemprop='title']...");
            List<WebElement> titleElements = source.findElements(By.cssSelector("[itemprop='title']"));
            log.debug("🔍 Found {} [itemprop='title'] elements", titleElements.size());
            
            for (WebElement titleElement : titleElements) {
                String content = titleElement.getAttribute("content");
                String text = titleElement.getText();
                log.debug("🔍 Title element - content: '{}', text: '{}'", content, text);
                
                if (content != null && !content.trim().isEmpty()) {
                    log.debug("💼 Found title using [itemprop='title'] content: '{}'", content.trim());
                    return content.trim();
                }
                if (text != null && !text.trim().isEmpty()) {
                    log.debug("💼 Found title using [itemprop='title'] text: '{}'", text.trim());
                    return text.trim();
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Strategy 3 failed: {}", e.getMessage());
        }
        
        // Стратегія 4: Шукаємо в заголовках (h1, h2, h3)
        try {
            log.debug("🔍 Strategy 4 - Searching for headings...");
            List<WebElement> headings = source.findElements(By.cssSelector("h1, h2, h3, h4, h5, h6"));
            log.debug("🔍 Found {} heading elements", headings.size());
            
            for (WebElement heading : headings) {
                String text = heading.getText();
                log.debug("🔍 Heading text: '{}'", text);
                
                if (text != null && !text.trim().isEmpty() && text.length() > 3) {
                    log.debug("💼 Found title in heading: '{}'", text.trim());
                    return text.trim();
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Strategy 4 failed: {}", e.getMessage());
        }
        
        // Стратегія 5: Шукаємо в посиланнях з текстом що може бути назвою позиції
        try {
            log.debug("🔍 Strategy 5 - Searching for links that might contain title...");
            List<WebElement> links = source.findElements(By.cssSelector("a[href]"));
            log.debug("🔍 Found {} links", links.size());
            
            for (WebElement link : links) {
                String text = link.getText();
                String href = link.getAttribute("href");
                log.debug("🔍 Link - text: '{}', href: '{}'", text, href);
                
                // Перевіряємо чи посилання містить /jobs/ (може бути назва позиції)
                if (text != null && !text.trim().isEmpty() && 
                    href != null && href.contains("/jobs/") && 
                    text.length() > 3 && text.length() < 100) {
                    log.debug("💼 Found title in job link: '{}'", text.trim());
                    return text.trim();
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Strategy 5 failed: {}", e.getMessage());
        }
        
        log.warn("⚠️ All strategies failed to find title");
        return "Unknown Position";
    }

    @Override
    public String extractTitle(WebDriver source) {
        try {
            for (String selector : ScrapingSelectors.JOB_DETAIL_PAGE) {
                if (selector.contains("itemprop='title'")) {
                    List<WebElement> elements = source.findElements(By.cssSelector(selector));
                    if (!elements.isEmpty()) {
                        String title = elements.get(0).getText().trim();
                        log.info("✅ Extracted title from detail page: {}", title);
                        return title;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Error extracting title from detail page: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public String extractDescription(WebElement source) {
        try {
            // ✅ Шукаємо опис за селекторами з ScrapingSelectors (найточніші)
            for (String selector : ScrapingSelectors.DESCRIPTION) {
                try {
                    List<WebElement> elements = source.findElements(By.cssSelector(selector));
                    for (WebElement element : elements) {
                        String text = element.getText();
                        String content = element.getAttribute("content");
                        
                        // Перевіряємо content атрибут
                        if (content != null && !content.trim().isEmpty() && content.length() < 500) {
                            // ✅ Перевіряємо, чи це не назва вакансії
                            if (!content.contains(" at ") && !content.contains(" - ") && 
                                !content.contains("UX Designer") && !content.contains("Software Engineer")) {
                                log.debug("📝 Found description using selector '{}' content: '{}'", selector, content);
                                return content.trim();
                            }
                        }
                        
                        // Перевіряємо текст елемента
                        if (text != null && !text.trim().isEmpty() && text.length() < 500) {
                            // ✅ Перевіряємо, чи це не назва вакансії
                            if (!text.contains(" at ") && !text.contains(" - ") && 
                                !text.contains("UX Designer") && !text.contains("Software Engineer")) {
                                log.debug("📝 Found description using selector '{}' text: '{}'", selector, text);
                                return text.trim();
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("⚠️ Selector '{}' failed: {}", selector, e.getMessage());
                }
            }
            
            // ✅ Шукаємо короткий опис в meta тегах
            List<WebElement> metaElements = source.findElements(By.cssSelector("meta[name='description'], meta[property='og:description']"));
            for (WebElement meta : metaElements) {
                String content = meta.getAttribute("content");
                if (content != null && !content.trim().isEmpty() && content.length() < 500) {
                    log.debug("📝 Found description in meta tag: '{}'", content);
                    return content.trim();
                }
            }
            
        } catch (Exception e) {
            log.debug("⚠️ Error extracting description: {}", e.getMessage());
        }
        
        log.debug("📝 No description found");
        return null;
    }

    @Override
    public String extractDescription(WebDriver source) {
        try {
            // ✅ Шукаємо опис за селекторами з ScrapingSelectors
            for (String selector : ScrapingSelectors.JOB_DETAIL_PAGE) {
                if (selector.contains("description") || selector.contains("content")) {
                    List<WebElement> elements = source.findElements(By.cssSelector(selector));
                    for (WebElement element : elements) {
                        String text = element.getText();
                        String content = element.getAttribute("content");
                        
                        // Перевіряємо content атрибут
                        if (content != null && !content.trim().isEmpty() && content.length() < 1000) {
                            if (!content.contains(" at ") && !content.contains(" - ")) {
                                log.info("✅ Extracted description from detail page using selector '{}': '{}'", selector, content);
                                return content.trim();
                            }
                        }
                        
                        // Перевіряємо текст елемента
                        if (text != null && !text.trim().isEmpty() && text.length() < 1000) {
                            if (!text.contains(" at ") && !text.contains(" - ")) {
                                log.info("✅ Extracted description from detail page using selector '{}': '{}'", selector, text);
                                return text.trim();
                            }
                        }
                    }
                }
            }
            
            // ✅ Шукаємо опис в meta тегах
            List<WebElement> metaElements = source.findElements(By.cssSelector("meta[name='description'], meta[property='og:description']"));
            for (WebElement meta : metaElements) {
                String content = meta.getAttribute("content");
                if (content != null && !content.trim().isEmpty() && content.length() < 1000) {
                    log.info("✅ Extracted description from detail page meta tag: '{}'", content);
                    return content.trim();
                }
            }
            
        } catch (Exception e) {
            log.warn("⚠️ Error extracting description from detail page: {}", e.getMessage());
        }
        
        log.debug("📝 No description found on detail page");
        return null;
    }
}
