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
 * 
 * ПРИНЦИПИ:
 * - DRY (Don't Repeat Yourself) - немає дублювання коду
 * - Єдинорідність - одна логіка для обох джерел
 * - Легко тестувати - кожен метод тестується окремо
 * - Легко розширювати - нові стратегії додаються просто
 */
public interface DataExtractionService {
    
    /**
     * Витягує теги з джерела (картка або детальна сторінка)
     */
    List<String> extractTags(WebElement source);
    List<String> extractTags(WebDriver source);
    
    /**
     * Витягує локацію з джерела (картка або детальна сторінка)
     */
    String extractLocation(WebElement source);
    String extractLocation(WebDriver source);
    
    /**
     * Витягує дату публікації з джерела (картка або детальна сторінка)
     */
    LocalDateTime extractPostedDate(WebElement source);
    LocalDateTime extractPostedDate(WebDriver source);
    
    /**
     * Витягує URL логотипу з картки вакансії
     */
    String extractLogoUrl(WebElement source);
    
    /**
     * Витягує назву компанії з джерела (картка або детальна сторінка)
     */
    String extractCompanyName(WebElement source);
    String extractCompanyName(WebDriver source);
    
    /**
     * Витягує заголовок вакансії з джерела (картка або детальна сторінка)
     */
    String extractTitle(WebElement source);
    String extractTitle(WebDriver source);
    
    /**
     * Витягує опис вакансії з джерела (картка або детальна сторінка)
     */
    String extractDescription(WebElement source);
    String extractDescription(WebDriver source);
}
