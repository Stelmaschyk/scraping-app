# 🚀 Scraping App - Скрапінг вакансій Techstars

## 📋 Опис
Додаток для скрапінгу вакансій з сайту Techstars з використанням Selenium та збереженням в PostgreSQL.

## 🔧 Технології
- **Spring Boot 3.2.0**
- **Java 17**
- **PostgreSQL**
- **Selenium WebDriver**
- **Jsoup**
- **Liquibase**

## 🚀 Запуск

### 1. Налаштування бази даних
```bash
# Створіть базу даних PostgreSQL
createdb scraping_dev

# Налаштуйте application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/scraping_dev
spring.datasource.username=postgres
spring.datasource.password=1234
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
  "jobFunctions": ["Software Engineer", "UX Designer", "Product Manager"]
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

1. **Selenium відкриває** сайт Techstars Jobs
2. **Застосовуються фільтри** тільки по Job Functions (теги не фільтруються)
3. **Натискається кнопка "Load More"** один раз
4. **Сторінка прокручується** до самого низу з автоматичним завантаженням
5. **Скрапляться всі вакансії** що відповідають функціям (з усіма полями)
6. **Дані зберігаються** в базу даних

## 🔄 Нова логіка фільтрації (2025)

**Зміни в API:**
- **Видалено поле `tags`** з request body
- **Залишено тільки `jobFunctions`** для фільтрації
- **Теги збираються для всіх вакансій** без фільтрації
- **Вакансії зберігаються** якщо відповідають функціям

**Нова гібридна логіка завантаження:**
1. **Job Functions** → фільтрація за функціями
2. **Load More** → натискання кнопки ОДИН раз
3. **Нескінченна прокрутка** → автоматичне завантаження
4. **URL перевірка** → пошук посилань на вакансії
5. **Префікс компанії** → збереження вакансій Techstars
6. **Збір тегів** → для всіх збережених вакансій

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

## 📊 Логування
Всі операції логуються з емодзі для кращої читабельності:
- 🚀 Початок операції
- 🔍 Скрапінг
- ✅ Успішне завершення
- ❌ Помилки
- ⚠️ Попередження

## 🧪 Тестування

### Postman
```http
POST http://localhost:8080/api/scrape
Content-Type: application/json

{
  "jobFunctions": ["IT"],
  "tags": []
}
```

### cURL
```bash
curl -X POST http://localhost:8080/api/scrape \
  -H "Content-Type: application/json" \
  -d '{"jobFunctions": ["IT"], "tags": []}'
```

## 📁 Структура проекту
```
src/main/java/com/scrapper/
├── controller/
│   └── ScrapeController.java          # Єдиний контролер
├── service/
│   ├── ApplyUrlScraperService.java    # Скрапінг з Selenium
│   └── JobIngestService.java          # Збереження в БД
├── model/
│   ├── Job.java                       # Модель вакансії
│   └── JobFunction.java               # Функції вакансій
├── dto/
│   ├── ScrapeRequestDto.java          # Вхідні дані
│   └── ScrapeResponseDto.java         # Вихідні дані
└── util/
    └── ScrapingSelectors.java         # Всі CSS селектори
```

## ⚠️ Важливо
- Додаток розроблений для навчальних цілей
- Використовуйте відповідально
- Дотримуйтесь robots.txt та умов використання сайту
- Час виконання: 2-5 хвилин
- Очікуваний результат: 50-200+ вакансій

## 🔧 Налагодження
Логи доступні в `logs/application.log` з детальною інформацією про кожен крок скрапінгу.
