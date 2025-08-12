# 🚀 Scraping App - Скрапінг вакансій Techstars

## 📋 Опис
Додаток для скрапінгу вакансій з сайту Techstars з використанням Selenium WebDriver та збереженням в PostgreSQL. Проект має модульну архітектуру з розділенням відповідальності між сервісами.

## 🔧 Технології
- **Spring Boot 3.2.0**
- **Java 17**
- **PostgreSQL**
- **Selenium WebDriver 4.15.0**
- **WebDriver Manager 5.6.2**
- **Jsoup 1.17.1**
- **Liquibase**
- **MapStruct 1.5.5**
- **Lombok**

## 🏗️ Архітектура проекту

### 📁 Структура проекту
```
src/main/java/com/scrapper/
├── controller/
│   └── ScrapeController.java              # REST API контролер
├── service/
│   ├── ApplyUrlScraperService.java        # Основний сервіс скрапінгу
│   ├── ApplyUrlScraperServiceImpl.java    # Реалізація скрапінгу
│   ├── JobProcessingService.java          # Обробка карток вакансій
│   ├── JobCardProcessingService.java      # Обробка карток з покращеною логікою
│   ├── JobCreationService.java            # Створення Job об'єктів
│   ├── JobIngestService.java              # Збереження в базу даних
│   ├── JobReportingService.java           # Звітність та статистика
│   ├── PageInteractionService.java        # Взаємодія з веб-сторінкою
│   ├── criteriaServices/                  # Сервіси для витягування даних
│   │   ├── DataExtractionService.java     # Єдиний сервіс екстракції
│   │   ├── DateParsingService.java        # Парсинг дат
│   │   ├── DescriptionIngestService.java  # Збереження описів
│   │   ├── LocationIngestService.java     # Збереження локацій
│   │   ├── LogoIngestService.java         # Збереження логотипів
│   │   ├── TagIngestService.java          # Збереження тегів
│   │   └── TitleIngestService.java        # Збереження заголовків
│   └── webdriver/                         # Управління WebDriver
│       ├── WebDriverConfigService.java     # Конфігурація WebDriver
│       ├── WebDriverManagerService.java    # Управління драйверами
│       ├── WebDriverNavigationService.java # Навігація по сторінках
│       └── WebDriverService.java           # Основний WebDriver сервіс
├── model/
│   ├── Job.java                           # Модель вакансії
│   └── JobFunction.java                   # Функції вакансій
├── dto/
│   ├── ScrapeRequestDto.java              # Вхідні дані для скрапінгу
│   └── ScrapeResponseDto.java             # Вихідні дані скрапінгу
├── repository/                             # Репозиторії для роботи з БД
├── config/
│   └── HttpClientConfig.java              # Конфігурація HTTP клієнта
├── mapper/                                # Маппери для конвертації
├── util/
│   ├── ScrapingSelectors.java             # CSS селектори для скрапінгу
│   └── TechstarsFilterUtil.java           # Утиліти для фільтрації
└── validation/
    └── Validation.java                     # Валідація даних
```

## 🚀 Запуск

### 1. Налаштування бази даних
```bash
# Створіть базу даних PostgreSQL
createdb scraping_dev

# Налаштуйте application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/scraping_dev
spring.datasource.username=dbname
spring.datasource.password=11111
```

### 2. Запуск додатку
```bash
mvn spring-boot:run
```

## 📡 API

### Єдиний endpoint для скрапінгу

**POST** `http://localhost:8080/api/scrape`

**Request Body:**
```json
{
  "jobFunctions": ["it"]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Scraping and saving completed successfully",
  "totalJobsFound": 25,
  "jobsSaved": 23,
  "jobUrls": [
    "https://jobs.techstars.com/companies/company1/jobs/123",
    "https://jobs.techstars.com/companies/company2/jobs/456"
  ]
}
```

## 🎯 Доступні Job Functions
- `Software Engineering`
- `Product`
- `Marketing & Communications`
- `Design`
- `IT`
- `Legal`
- `Operations`
- `Other Engineering`
- `People & HR`
- `Quality Assurance`
- `Sales & Business Development`

## 🔍 Як це працює

### 🎯 Основна логіка скрапінгу
1. **Selenium відкриває** сайт Techstars Jobs
2. **Застосовуються фільтри** тільки по Job Functions (теги не фільтруються)
3. **Натискається кнопка "Load More"** один раз
4. **Сторінка прокручується** до самого низу з автоматичним завантаженням
5. **Скрапляться всі вакансії** що відповідають функціям (з усіма полями)
6. **Дані зберігаються** в базу даних

## ⚙️ Налаштування

### Selenium налаштування
```properties
# Таймаут для Selenium
scraping.selenium.timeout=30

# Затримка між скролами (10 секунд)
scraping.selenium.scroll.delay=10000

# Максимальна кількість спроб скролу
scraping.selenium.scroll.max-attempts=20

# Максимальна кількість спроб без нових вакансій
scraping.selenium.scroll.max-no-new-jobs=3
```

## 🔧 Основні сервіси

### 1. **ApplyUrlScraperService** - Основний сервіс скрапінгу
- Координує весь процес скрапінгу
- Управляє WebDriver та навігацією
- Викликає спеціалізовані сервіси для обробки

### 2. **JobProcessingService** - Обробка карток вакансій
- Створює Job об'єкти з карток
- Застосовує фільтри за функціями
- Обробляє вакансії з різних сторінок

### 3. **DataExtractionService** - Єдиний сервіс екстракції
- Витягує дані з карток та детальних сторінок
- Універсальні методи для обох джерел
- Підтримує WebElement та WebDriver

### 4. **PageInteractionService** - Взаємодія з веб-сторінкою
- Знаходить картки вакансій
- Керує скролінгом та завантаженням
- Натискає кнопки та навігує

### 5. **WebDriver Services** - Управління браузером
- **WebDriverConfigService**: Налаштування драйвера
- **WebDriverManagerService**: Автоматичне управління драйверами
- **WebDriverNavigationService**: Навігація та очікування

## 📊 Логування
Всі операції логуються з емодзі для кращої читабельності:
- 🚀 Початок операції
- 🔍 Скрапінг
- ✅ Успішне завершення
- ❌ Помилки
- ⚠️ Попередження

### Postman
```http
POST http://localhost:8080/api/api/scrape
Content-Type: application/json

{
  "jobFunctions": ["IT"],
}
```

### cURL
```bash
curl -X POST http://localhost:8080/api/api/scrape \
  -H "Content-Type: application/json" \
  -d '{"jobFunctions": ["IT"]}'
```

## 🗄️ База даних

### Основні таблиці
- **jobs** - основна інформація про вакансії
- **job_locations** - локації вакансій
- **job_tags** - теги вакансій

### Liquibase міграції
- Автоматичне створення схеми БД
- Версіонування структури
- Міграція даних

## ⚠️ Важливо
- Додаток розроблений для навчальних цілей
- Використовуйте відповідально
- Дотримуйтесь robots.txt та умов використання сайту
- Час виконання: 2-5 хвилин
- Очікуваний результат: 50-200+ вакансій
- **Всі операції виконуються послідовно без багатопоточності**

## 🔧 Налагодження
Логи доступні в `logs/application.log` з детальною інформацією про кожен крок скрапінгу.

### Основні логи
- Початок та завершення скрапінгу
- Кількість знайдених карток
- Статистика фільтрації
- Помилки та попередження
