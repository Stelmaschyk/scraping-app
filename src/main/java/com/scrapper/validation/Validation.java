package com.scrapper.validation;

import com.scrapper.model.Job;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Predicate;

public final class Validation {
    public static final Predicate<String> NOT_BLANK = value -> value != null && !value.trim().isEmpty();
    public static final Predicate<Job> IS_VALID = job ->
        NOT_BLANK.test(job.getPositionName()) &&
            NOT_BLANK.test(job.getJobPageUrl()) &&
            NOT_BLANK.test(job.getOrganizationTitle()) &&
            NOT_BLANK.test(job.getLaborFunction()) &&
            job.getPostedDate() > 0; // ✅ ВИПРАВЛЕНО: Перевіряємо Unix Timestamp > 0 замість != null
    
    // ✅ ДОДАНО: Методи валідації даних з ApplyUrlScraperServiceImpl
    
    /**
     * ✅ Фільтрує елементи, щоб знайти тільки реальні картки вакансій
     */
    public static List<WebElement> filterValidJobCards(List<WebElement> elements) {
        List<WebElement> validCards = new ArrayList<>();
        
        for (WebElement element : elements) {
            try {
                // Перевіряємо, чи це реальна картка вакансії
                if (isValidJobCard(element)) {
                    validCards.add(element);
                }
            } catch (Exception e) {
                // Логування помилок валідації
            }
        }
        
        return validCards;
    }
    
    /**
     * ✅ Перевіряє, чи є елемент реальною карткою вакансії
     */
    public static boolean isValidJobCard(WebElement element) {
        try {
            String text = element.getText().toLowerCase();
            
            // ✅ Фільтруємо навігаційні елементи та фільтри
            String[] navigationKeywords = {
                "search", "explore", "join", "my", "job alerts", "on-site", "remote", 
                "job function", "seniority", "salary", "industry", "company stage", 
                "more filters", "create job alert", "powered by", "showing", "jobs",
                "companies", "talent network", "claim your profile"
            };
            
            for (String keyword : navigationKeywords) {
                if (text.contains(keyword.toLowerCase())) {
                    return false; // Це навігаційний елемент
                }
            }
            
            // ✅ Перевіряємо, чи містить елемент посилання на вакансію
            try {
                List<WebElement> links = element.findElements(By.cssSelector("a[href*='/jobs/'], a[href*='/companies/']"));
                if (links.isEmpty()) {
                    return false; // Немає посилань на вакансії
                }
            } catch (Exception e) {
                return false; // Помилка при пошуку посилань
            }
            
            // ✅ Перевіряємо, чи містить елемент інформацію про вакансію
            String[] jobKeywords = {
                "engineer", "designer", "manager", "developer", "analyst", "specialist",
                "coordinator", "director", "lead", "senior", "junior", "full-time",
                "part-time", "remote", "onsite", "hybrid", "salary", "experience"
            };
            
            boolean hasJobInfo = false;
            for (String keyword : jobKeywords) {
                if (text.contains(keyword.toLowerCase())) {
                    hasJobInfo = true;
                    break;
                }
            }
            
            if (!hasJobInfo) {
                return false; // Немає інформації про вакансію
            }
            
            // ✅ Перевіряємо розмір тексту (картка вакансії має бути достатньо великою)
            if (text.length() < 50) {
                return false; // Занадто короткий текст
            }
            
            return true; // Це схоже на реальну картку вакансії
            
        } catch (Exception e) {
            return false;
        }
    }
    

}
