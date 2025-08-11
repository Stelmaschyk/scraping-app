package com.scrapper.service.criteriaServices;

public interface PositionNameIngestService {
    
    /**
     * Зберегти назву позиції для вакансії
     * @param jobId ID вакансії
     * @param positionName назва позиції
     * @return true якщо збережено успішно
     */
    boolean savePositionNameForJob(Long jobId, String positionName);
}
