package com.scrapper.service.criteriaServices;

public interface LocationIngestService {
    
    /**
     * Зберігає локацію для вакансії
     * @param jobId ID вакансії
     * @param location локація (місто, штат, країна)
     * @return true якщо збережено успішно
     */
    boolean saveLocationForJob(Long jobId, String location);
    
    /**
     * Очищає локацію для вакансії
     * @param jobId ID вакансії
     */
    void clearLocationForJob(Long jobId);
}

