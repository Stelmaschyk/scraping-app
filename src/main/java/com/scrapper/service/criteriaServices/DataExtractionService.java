package com.scrapper.service.criteriaServices;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.time.LocalDateTime;
import java.util.List;

public interface DataExtractionService {

    List<String> extractTags(WebElement source);
    String extractLocation(WebElement source);
    LocalDateTime extractPostedDate(WebElement source);
    String extractLogoUrl(WebElement source);
    String extractCompanyName(WebElement source);
    String extractTitle(WebElement source);
    String extractDescription(WebElement source);
}
