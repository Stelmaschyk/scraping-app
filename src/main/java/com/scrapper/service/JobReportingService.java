package com.scrapper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервіс для формування звітів про скрапінг
 * Відповідає за логування результатів та формування статистики
 */
@Service
@Slf4j
public class JobReportingService {

    /**
     * Формує оновлений звіт про фільтрацію з новою логікою
     */
    public void printUpdatedFinalReport(int totalCards, int passedFunctionFilter,
                                       int foundUrls, int finalJobs, int savedWithCompanyPrefix, 
                                       int savedWithoutCompanyPrefix, List<String> functions) {
        log.info("📊 ОНОВЛЕНИЙ ЗВІТ ПРО ФІЛЬТРАЦІЮ (НОВА ЛОГІКА):");
        log.info("   • Всього карток: {}", totalCards);
        log.info("   • Пройшли фільтр функцій: {}", passedFunctionFilter);
        log.info("   • Знайдено URL: {}", foundUrls);
        log.info("   • Збережено з префіксом компанії (БЕЗ перевірки тегів): {}", savedWithCompanyPrefix);
        log.info("   • Збережено без префіксу компанії (тільки фільтр функцій): {}", savedWithoutCompanyPrefix);
        log.info("   • Фінальних вакансій: {}", finalJobs);
        
        if (totalCards > 0) {
            log.info("   • Ефективність фільтрації функцій: {:.1f}%", (double) passedFunctionFilter / totalCards * 100);
        }
        if (passedFunctionFilter > 0) {
            log.info("   • Конверсія в URL: {:.1f}%", (double) foundUrls / passedFunctionFilter * 100);
        }
        if (foundUrls > 0) {
            log.info("   • Конверсія в фінальні вакансії: {:.1f}%", (double) finalJobs / foundUrls * 100);
            log.info("   • Частка збережених з префіксом компанії: {:.1f}%", (double) savedWithCompanyPrefix / foundUrls * 100);
            log.info("   • Частка збережених без префіксу компанії: {:.1f}%", (double) savedWithoutCompanyPrefix / foundUrls * 100);
        }
        log.info("   • Застосовані функції: {}", functions);
        
        // Перевірка нової логіки
        if (savedWithCompanyPrefix > 0) {
            log.info("✅ Нова логіка працює: {} вакансій збережено з префіксом компанії БЕЗ перевірки тегів", savedWithCompanyPrefix);
        }
        
        if (passedFunctionFilter > 0 && foundUrls == 0) {
            log.error("❌ КРИТИЧНА ПОМИЛКА: Всі {} відфільтрованих карток не дали URL!", passedFunctionFilter);
        }
        
        if (foundUrls > 0 && finalJobs == 0) {
            log.error("❌ КРИТИЧНА ПОМИЛКА: Всі {} знайдених URL не пройшли фінальну перевірку!", foundUrls);
        }
        
        log.info("🎯 Результат: {} з {} карток успішно оброблено", finalJobs, totalCards);
        log.info("🔍 НОВА ГІБРИДНА ЛОГІКА: 1) job functions → 2) Load More (ОДИН раз) → 3) нескінченна прокрутка → 4) URL → 5) префікс компанії → 6) збір тегів (без фільтрації)");
    }



    /**
     * Формує звіт про помилки
     */
    public void printErrorReport(String operation, Exception error, String additionalInfo) {
        log.error("❌ ЗВІТ ПРО ПОМИЛКУ:");
        log.error("   • Операція: {}", operation);
        log.error("   • Помилка: {}", error.getMessage());
        log.error("   • Додаткова інформація: {}", additionalInfo);
        log.error("   • Stack trace:", error);
    }

    /**
     * Формує звіт про успішне завершення
     */
    public void printSuccessReport(String operation, int totalItems, long durationMs) {
        log.info("✅ ЗВІТ ПРО УСПІШНЕ ЗАВЕРШЕННЯ:");
        log.info("   • Операція: {}", operation);
        log.info("   • Оброблено елементів: {}", totalItems);
        log.info("   • Тривалість: {} мс", durationMs);
        
        if (durationMs > 0) {
            double itemsPerSecond = (double) totalItems / (durationMs / 1000.0);
            log.info("   • Швидкість: {:.2f} елементів/сек", itemsPerSecond);
        }
    }

    /**
     * Формує звіт про прогрес обробки
     */
    public void printProgressReport(int current, int total, String operation) {
        if (current % 10 == 0 || current == total) {
            double percentage = (double) current / total * 100;
            log.info("📊 Прогрес {}: {}/{} ({:.1f}%)", operation, current, total, percentage);
        }
    }

    /**
     * Формує звіт про статистику скрапінгу
     */
    public void printScrapingStatistics(int totalPages, int totalJobs, int totalErrors, 
                                       long totalDuration, List<String> jobFunctions) {
        log.info("📈 СТАТИСТИКА СКРАПІНГУ:");
        log.info("   • Оброблено сторінок: {}", totalPages);
        log.info("   • Знайдено вакансій: {}", totalJobs);
        log.info("   • Помилок: {}", totalErrors);
        log.info("   • Загальна тривалість: {} мс", totalDuration);
        log.info("   • Фільтр функцій: {}", jobFunctions);
        
        if (totalDuration > 0) {
            double jobsPerSecond = (double) totalJobs / (totalDuration / 1000.0);
            log.info("   • Загальна швидкість: {:.2f} вакансій/сек", jobsPerSecond);
        }
        
        if (totalPages > 0) {
            double jobsPerPage = (double) totalJobs / totalPages;
            log.info("   • Середня кількість вакансій на сторінку: {:.1f}", jobsPerPage);
        }
        
        if (totalJobs > 0) {
            double errorRate = (double) totalErrors / totalJobs * 100;
            log.info("   • Частка помилок: {:.1f}%", errorRate);
        }
    }
}

