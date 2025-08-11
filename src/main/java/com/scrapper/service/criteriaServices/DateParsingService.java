package com.scrapper.service.criteriaServices;

import java.time.LocalDateTime;

public interface DateParsingService {
    
    /**
     * Парсить дату з meta тегу itemprop="datePosted"
     * @param dateStr рядок дати з meta тегу (наприклад, "2025-08-10")
     * @return LocalDateTime або null якщо не вдалося розпарсити
     */
    LocalDateTime parseMetaDate(String dateStr);
}
