package com.scrapper.service.criteriaServices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@Slf4j
public class DateParsingServiceImpl implements DateParsingService {
    
    @Override
    public LocalDateTime parseMetaDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        String cleanDate = dateStr.trim();
        log.debug("🔍 Parsing meta date: '{}'", cleanDate);
        
        try {
            // ✅ Парсимо дату формату YYYY-MM-DD з meta тегу
            if (cleanDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                LocalDateTime date = LocalDateTime.parse(cleanDate.trim() + "T00:00:00");
                log.debug("✅ Parsed meta date: '{}' -> {} (Unix: {})", 
                        cleanDate, date, date.toEpochSecond(java.time.ZoneOffset.UTC));
                return date;
            } else {
                log.warn("⚠️ Meta date format not supported: '{}', expected YYYY-MM-DD", cleanDate);
                return null;
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not parse meta date: '{}', error: {}", cleanDate, e.getMessage());
            return null;
        }
    }
}
