package com.scrapper.service.criteriaServices;

public interface LogoIngestService {
    
    /**
     * Зберігає URL логотипу для вакансії
     * @param jobId ID вакансії
     * @param logoUrl URL логотипу компанії
     * @return true якщо збережено успішно
     */
    boolean saveLogoForJob(Long jobId, String logoUrl);
}

