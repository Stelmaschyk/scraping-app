package com.scrapper.service.criteriaServices;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ НОВИЙ СЕРВІС: Єдиний сервіс для витягування даних з різних джерел
 * 
 * Цей сервіс усуває дублювання логіки між методами extract*FromCard() та extract*FromDetailPage()
 * і надає універсальні методи для обох джерел даних.
 */
public interface DataExtractionService {
    
    /**
     * Витягує теги з картки вакансії
     */
    List<String> extractTags(WebElement source);
    
    /**
     * Витягує локацію з картки вакансії
     */
    String extractLocation(WebElement source);
    
    /**
     * Витягує дату публікації з картки вакансії
     */
    LocalDateTime extractPostedDate(WebElement source);
    
    /**
     * Витягує URL логотипу з картки вакансії
     */
    String extractLogoUrl(WebElement source);

    /**
     * Витягує назву компанії з картки вакансії
     */
    String extractCompanyName(WebElement source);

    /**
     * Витягує заголовок вакансії з картки
     */
    String extractTitle(WebElement source);

    /**
     * Витягує опис вакансії з картки
     */
    String extractDescription(WebElement source);
}
