package com.scrapper.service.webdriver;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Сервіс для налаштування Chrome WebDriver
 * Відповідає за конфігурацію ChromeOptions та обхід блокування
 */
@Service
@Slf4j
public class WebDriverConfigService {

    @Value("${scraping.selenium.window.width:1920}")
    private int windowWidth;

    @Value("${scraping.selenium.window.height:1080}")
    private int windowHeight;

    @Value("${scraping.selenium.headless:false}")
    private boolean headless;

    @Value("${scraping.selenium.disable.images:true}")
    private boolean disableImages;

    @Value("${scraping.selenium.disable.javascript:false}")
    private boolean disableJavaScript;

    /**
     * Створює та налаштовує ChromeOptions для скрапінгу
     * @return налаштовані ChromeOptions
     */
    public ChromeOptions createChromeOptions() {
        log.info("🔧 Creating Chrome options for scraping...");
        
        ChromeOptions options = new ChromeOptions();
        
        // ✅ Базові налаштування безпеки
        configureBasicSecurity(options);
        
        // ✅ Налаштування для обходу блокування ботів
        configureAntiBotProtection(options);
        
        // ✅ Налаштування продуктивності
        configurePerformance(options);
        
        // ✅ Налаштування вікна
        configureWindow(options);
        
        // ✅ Додаткові налаштування
        configureAdditionalSettings(options);
        
        log.info("✅ Chrome options configured successfully");
        return options;
    }

    /**
     * Налаштовує базову безпеку Chrome
     */
    private void configureBasicSecurity(ChromeOptions options) {
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        
        log.debug("🔒 Basic security options configured");
    }

    /**
     * Налаштовує обхід детекції автоматизації
     */
    private void configureAntiBotProtection(ChromeOptions options) {
        // Обхід детекції автоматизації
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins");
        
        // User-Agent для обходу блокування
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        
        // Додаткові налаштування для обходу блокування
        options.addArguments("--disable-blink-features");
        options.addArguments("--disable-features=VizDisplayCompositor");
        options.addArguments("--disable-ipc-flooding-protection");
        
        log.debug("🤖 Anti-bot protection options configured");
    }

    /**
     * Налаштовує продуктивність Chrome
     */
    private void configurePerformance(ChromeOptions options) {
        // Вимкнення зображень для швидшого завантаження
        if (disableImages) {
            options.addArguments("--disable-images");
            log.debug("🚫 Images disabled for performance");
        }
        
        // Вимкнення JavaScript (за потреби)
        if (disableJavaScript) {
            options.addArguments("--disable-javascript");
            log.debug("🚫 JavaScript disabled for performance");
        }
        
        // Додаткові налаштування продуктивності
        options.addArguments("--disable-background-timer-throttling");
        options.addArguments("--disable-backgrounding-occluded-windows");
        options.addArguments("--disable-renderer-backgrounding");
        
        log.debug("⚡ Performance options configured");
    }

    /**
     * Налаштовує вікно браузера
     */
    private void configureWindow(ChromeOptions options) {
        // Розмір вікна
        options.addArguments(String.format("--window-size=%d,%d", windowWidth, windowHeight));
        
        // Позиція вікна
        options.addArguments("--start-maximized");
        
        // Додаткові налаштування вікна
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-notifications");
        
        log.debug("🖥️ Window options configured: {}x{}", windowWidth, windowHeight);
    }

    /**
     * Налаштовує додаткові параметри
     */
    private void configureAdditionalSettings(ChromeOptions options) {
        // Режим headless (за налаштуваннями)
        if (headless) {
            options.addArguments("--headless");
            log.debug("👻 Headless mode enabled");
        }
        
        // Віддалений дебаг
        options.addArguments("--remote-debugging-port=9222");
        
        // Додаткові експериментальні налаштування
        options.addArguments("--disable-features=TranslateUI");
        options.addArguments("--disable-features=BlinkGenPropertyTrees");
        
        log.debug("🔧 Additional settings configured");
    }

    /**
     * Створює ChromeOptions для тестування (без headless режиму)
     * @return ChromeOptions для тестування
     */
    public ChromeOptions createTestChromeOptions() {
        log.info("🧪 Creating Chrome options for testing...");
        
        ChromeOptions options = new ChromeOptions();
        
        // Для тестування завжди вимикаємо headless режим
        configureBasicSecurity(options);
        configureAntiBotProtection(options);
        configurePerformance(options);
        configureWindow(options);
        
        // Додаткові налаштування для тестування
        options.addArguments("--start-maximized");
        options.addArguments("--remote-debugging-port=9222");
        
        log.info("✅ Test Chrome options configured successfully");
        return options;
    }

    /**
     * Створює ChromeOptions для продакшену (з headless режимом)
     * @return ChromeOptions для продакшену
     */
    public ChromeOptions createProductionChromeOptions() {
        log.info("🏭 Creating Chrome options for production...");
        
        ChromeOptions options = createChromeOptions();
        
        // Для продакшену завжди включаємо headless режим
        options.addArguments("--headless");
        
        log.info("✅ Production Chrome options configured successfully");
        return options;
    }

    /**
     * Оновлює налаштування вікна
     * @param width нова ширина
     * @param height нова висота
     */
    public void updateWindowSize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        log.info("📐 Window size updated to {}x{}", width, height);
    }

    /**
     * Перемикає headless режим
     * @param headless true для включення headless режиму
     */
    public void setHeadlessMode(boolean headless) {
        this.headless = headless;
        log.info("👻 Headless mode set to: {}", headless);
    }

    /**
     * Перемикає режим зображень
     * @param disableImages true для вимкнення зображень
     */
    public void setImageMode(boolean disableImages) {
        this.disableImages = disableImages;
        log.info("🖼️ Image mode set to: {}", disableImages ? "disabled" : "enabled");
    }
}
