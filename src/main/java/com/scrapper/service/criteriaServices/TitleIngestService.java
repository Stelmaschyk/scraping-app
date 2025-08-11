package com.scrapper.service.criteriaServices;

public interface TitleIngestService {
    
    /**
     * Зберігає назву позиції для вакансії
     * @param jobId ID вакансії
     * @param title назва позиції
     * @return true якщо збережено успішно
     */
    boolean saveTitleForJob(Long jobId, String title);
    
    /**
     * Очищає назву позиції для вакансії
     * @param jobId ID вакансії
     */
    void clearTitleForJob(Long jobId);
}

