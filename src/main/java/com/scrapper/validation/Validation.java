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
    
    /**
     * ✅ Валідація тегів
     */
    public static boolean isValidTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return false;
        }
        
        String cleanTag = tag.trim().toLowerCase();
        
        // Фільтруємо занадто короткі теги
        if (cleanTag.length() < 2) {
            return false;
        }
        
        // Фільтруємо занадто довгі теги
        if (cleanTag.length() > 50) {
            return false;
        }
        
        // Фільтруємо небажані теги
        String[] unwantedTags = {
            "search", "explore", "join", "my", "job alerts", "on-site", "remote",
            "job function", "seniority", "salary", "industry", "company stage",
            "more filters", "create job alert", "powered by", "showing", "jobs",
            "companies", "talent network", "claim your profile"
        };
        
        for (String unwanted : unwantedTags) {
            if (cleanTag.contains(unwanted)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * ✅ Валідація локації
     */
    public static boolean isValidLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            return false;
        }
        
        String cleanLocation = location.trim();
        
        // Фільтруємо занадто короткі локації
        if (cleanLocation.length() < 3) {
            return false;
        }
        
        // Фільтруємо занадто довгі локації
        if (cleanLocation.length() > 100) {
            return false;
        }
        
        // Перевіряємо, чи містить локація кому (показник того, що це реальна локація)
        if (!cleanLocation.contains(",")) {
            return false;
        }
        
        // Перевіряємо, чи містить локація країну або штат
        String[] validCountries = {"usa", "united states", "america", "canada", "uk", "united kingdom", "germany", "france", "spain", "italy", "netherlands", "sweden", "norway", "denmark", "finland", "switzerland", "austria", "belgium", "ireland", "portugal", "greece", "poland", "czech republic", "hungary", "romania", "bulgaria", "croatia", "slovenia", "slovakia", "estonia", "latvia", "lithuania", "india", "china", "japan", "south korea", "singapore", "australia", "new zealand"};
        
        String lowerLocation = cleanLocation.toLowerCase();
        boolean hasValidCountry = false;
        
        for (String country : validCountries) {
            if (lowerLocation.contains(country)) {
                hasValidCountry = true;
                break;
            }
        }
        
        if (!hasValidCountry) {
            return false;
        }
        
        return true;
    }
    
    /**
     * ✅ Валідація дати
     */
    public static boolean isValidDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return false;
        }
        
        String cleanDate = date.trim();
        
        // Перевіряємо, чи містить дата ключові слова
        String[] dateKeywords = {"posted", "today", "yesterday", "ago", "2024", "2025"};
        String lowerDate = cleanDate.toLowerCase();
        
        boolean hasDateKeyword = false;
        for (String keyword : dateKeywords) {
            if (lowerDate.contains(keyword)) {
                hasDateKeyword = true;
                break;
            }
        }
        
        if (!hasDateKeyword) {
            return false;
        }
        
        return true;
    }
    
    /**
     * ✅ Валідація заголовка
     */
    public static boolean isValidTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return false;
        }
        
        String cleanTitle = title.trim();
        
        // Фільтруємо занадто короткі заголовки
        if (cleanTitle.length() < 5) {
            return false;
        }
        
        // Фільтруємо занадто довгі заголовки
        if (cleanTitle.length() > 200) {
            return false;
        }
        
        // Перевіряємо, чи містить заголовок ключові слова про вакансію
        String[] jobKeywords = {
            "engineer", "designer", "manager", "developer", "analyst", "specialist",
            "coordinator", "director", "lead", "senior", "junior", "full-time",
            "part-time", "remote", "onsite", "hybrid", "salary", "experience"
        };
        
        String lowerTitle = cleanTitle.toLowerCase();
        boolean hasJobKeyword = false;
        
        for (String keyword : jobKeywords) {
            if (lowerTitle.contains(keyword)) {
                hasJobKeyword = true;
                break;
            }
        }
        
        if (!hasJobKeyword) {
            return false;
        }
        
        return true;
    }
    
    /**
     * ✅ Валідація назви компанії
     */
    public static boolean isValidCompanyName(String companyName) {
        if (companyName == null || companyName.trim().isEmpty()) {
            return false;
        }
        
        String cleanName = companyName.trim();
        
        // Фільтруємо занадто короткі назви
        if (cleanName.length() < 2) {
            return false;
        }
        
        // Фільтруємо занадто довгі назви
        if (cleanName.length() > 100) {
            return false;
        }
        
        // Фільтруємо небажані назви
        String[] unwantedNames = {
            "search", "explore", "join", "my", "job alerts", "on-site", "remote",
            "job function", "seniority", "salary", "industry", "company stage",
            "more filters", "create job alert", "powered by", "showing", "jobs",
            "companies", "talent network", "claim your profile"
        };
        
        String lowerName = cleanName.toLowerCase();
        for (String unwanted : unwantedNames) {
            if (lowerName.contains(unwanted)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * ✅ Валідація опису
     */
    public static boolean isValidDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return false;
        }
        
        String cleanDescription = description.trim();
        
        // Фільтруємо занадто короткі описи
        if (cleanDescription.length() < 20) {
            return false;
        }
        
        // Фільтруємо занадто довгі описи
        if (cleanDescription.length() > 5000) {
            return false;
        }
        
        // Перевіряємо, чи містить опис ключові слова про вакансію
        String[] jobKeywords = {
            "responsibilities", "requirements", "qualifications", "experience", "skills",
            "duties", "tasks", "goals", "objectives", "team", "project", "development",
            "design", "analysis", "management", "coordination", "planning", "strategy"
        };
        
        String lowerDescription = cleanDescription.toLowerCase();
        boolean hasJobKeyword = false;
        
        for (String keyword : jobKeywords) {
            if (lowerDescription.contains(keyword)) {
                hasJobKeyword = true;
                break;
            }
        }
        
        if (!hasJobKeyword) {
            return false;
        }
        
        return true;
    }
}
