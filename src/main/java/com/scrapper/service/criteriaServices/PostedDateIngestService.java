package com.scrapper.service.criteriaServices;

import java.time.LocalDateTime;

public interface PostedDateIngestService {
    
    /**
     * Зберігає дату публікації для вакансії
     * @param jobId ID вакансії
     * @param postedDate дата публікації
     * @return true якщо збережено успішно
     */
    boolean savePostedDateForJob(Long jobId, LocalDateTime postedDate);
}

