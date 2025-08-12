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
 * ✅ Сервіс для витягування даних з різних джерел
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataExtractionServiceImpl implements DataExtractionService {

    private final DateParsingService dateParsingService;

    @Override
    public List<String> extractTags(WebElement source) {
        List<String> tags = new ArrayList<>();
        try {
            List<WebElement> tagElements = source.findElements(By.cssSelector("[data-testid='tag"
                + "']"));
            for (WebElement tagElement : tagElements) {
                String tagText = tagElement.getText().trim();
                if (!tagText.isEmpty()) {
                    tags.add(tagText);
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Error extracting tags: {}", e.getMessage());
        }
        return tags;
    }

    @Override
    public String extractLocation(WebElement source) {
        try {
            List<WebElement> metaElements = source.findElements(By.cssSelector("[itemprop"
                + "='address']"));
            if (!metaElements.isEmpty()) {
                String location = metaElements.get(0).getAttribute("content");
                if (location != null && !location.trim().isEmpty()) {
                    return location.trim();
                }
            }

            List<WebElement> metaTags = source.findElements(By.cssSelector("meta[itemprop"
                + "='address']"));
            if (!metaTags.isEmpty()) {
                String location = metaTags.get(0).getAttribute("content");
                if (location != null && !location.trim().isEmpty()) {
                    return location.trim();
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Error extracting location: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public LocalDateTime extractPostedDate(WebElement source) {
        try {
            List<WebElement> metaElements = source.findElements(By.cssSelector("meta[itemprop"
                + "='datePosted']"));
            if (!metaElements.isEmpty()) {
                String dateStr = metaElements.get(0).getAttribute("content");
                if (dateStr != null && !dateStr.trim().isEmpty()) {
                    return dateParsingService.parseMetaDate(dateStr);
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Error extracting posted date: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public String extractLogoUrl(WebElement source) {
        try {
            List<WebElement> imgElements = source.findElements(By.cssSelector("img"));
            for (WebElement img : imgElements) {
                String src = img.getAttribute("src");
                String alt = img.getAttribute("alt");

                if (src != null && !src.trim().isEmpty() &&
                    alt != null && (alt.toLowerCase().contains("logo") || alt.toLowerCase().contains("company"))) {
                    return src.trim();
                }
            }
            List<WebElement> logoElements = source.findElements(By.cssSelector("[data-testid"
                + "='image'], [data-testid='profile-picture'] img"));
            if (!logoElements.isEmpty()) {
                String src = logoElements.get(0).getAttribute("src");
                if (src != null && !src.trim().isEmpty()) {
                    return src.trim();
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Error extracting logo URL: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public String extractCompanyName(WebElement source) {
        try {
            List<WebElement> metaElements = source.findElements(By.cssSelector("[itemprop='name"
                + "']"));
            if (!metaElements.isEmpty()) {
                String content = metaElements.get(0).getAttribute("content");
                if (content != null && !content.trim().isEmpty()) {
                    return content.trim();
                }
            }
            List<WebElement> metaTags = source.findElements(By.cssSelector("meta[itemprop='name"
                + "']"));
            if (!metaTags.isEmpty()) {
                String content = metaTags.get(0).getAttribute("content");
                if (content != null && !content.trim().isEmpty()) {
                    return content.trim();
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Error extracting company name: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public String extractTitle(WebElement source) {
        try {
            List<WebElement> titleElements = source.findElements(By.cssSelector("[itemprop='title"
                + "']"));
            for (WebElement titleElement : titleElements) {
                String content = titleElement.getAttribute("content");
                String text = titleElement.getText();

                if (content != null && !content.trim().isEmpty()) {
                    return content.trim();
                }
                if (text != null && !text.trim().isEmpty()) {
                    return text.trim();
                }
            }
        } catch (Exception e) {
        }

        try {
            WebElement titleElement = source.findElement(By.cssSelector("[data-testid='job-title"
                + "']"));
            String text = titleElement.getText();
            if (text != null && !text.trim().isEmpty()) {
                return text.trim();
            }
        } catch (Exception e) {
        }

        try {
            List<WebElement> headings = source.findElements(By.cssSelector("h1, h2, h3"));
            for (WebElement heading : headings) {
                String text = heading.getText();
                if (text != null && !text.trim().isEmpty() && text.length() > 3) {
                    return text.trim();
                }
            }
        } catch (Exception e) {
        }

        return "Unknown Position";
    }

    @Override
    public String extractDescription(WebElement source) {
        try {
            for (String selector : ScrapingSelectors.DESCRIPTION) {
                List<WebElement> elements = source.findElements(By.cssSelector(selector));
                for (WebElement element : elements) {
                    String text = element.getText();
                    String content = element.getAttribute("content");

                    if (content != null && !content.trim().isEmpty() && content.length() < 500) {
                        if (!content.contains(" at ") && !content.contains(" - ")) {
                            return content.trim();
                        }
                    }

                    if (text != null && !text.trim().isEmpty() && text.length() < 500) {
                        if (!text.contains(" at ") && !text.contains(" - ")) {
                            return text.trim();
                        }
                    }
                }
            }

            List<WebElement> metaElements = source.findElements(By.cssSelector("meta[name"
                + "='description'], meta[property='og:description']"));
            for (WebElement meta : metaElements) {
                String content = meta.getAttribute("content");
                if (content != null && !content.trim().isEmpty() && content.length() < 500) {
                    return content.trim();
                }
            }
        } catch (Exception e) {
            log.debug("⚠️ Error extracting description: {}", e.getMessage());
        }
        return null;
    }
}
