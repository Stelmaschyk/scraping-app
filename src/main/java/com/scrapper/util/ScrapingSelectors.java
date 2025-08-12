package com.scrapper.util;

public class ScrapingSelectors {
    private ScrapingSelectors() {
    }

    // Селектори для карток вакансій (використовується ApplyUrlScraperServiceImpl)
    public static final String[] JOB_CARD = new String[]{

        // Селектор, який дійсно працює (знаходить 220 елементів)
        "[class*='job-card']",

        // Більш точні селектори для пошуку карток з посиланнями
        "div:has(a[href*='/jobs/']):not(:has([data-testid=search])):not(:has([data-testid=navigation]))",
        "div:has(a[href*='/companies/']):not(:has([data-testid=search])):not(:has([data-testid=navigation]))",
        
        // Селектори для пошуку карток за структурою
        "div:has(h1, h2, h3):has(a[href*='/jobs/'])",
        "div:has(h1, h2, h3):has(a[href*='/companies/'])",
        
        // Селектори для пошуку карток за вмістом
        "div:has([class*='job']):has(a[href])",
        "div:has([class*='position']):has(a[href])",
        "div:has([class*='vacancy']):has(a[href])",
        
        // Прості селектори для Selenium (найнижчий пріоритет, але найнадійніші)
        "div[class*='sc-']:has(a[href])",
        "div[class*='job']:has(a[href])",
        "div[class*='position']:has(a[href])",
        "div[class*='vacancy']:has(a[href])",
        "div[class*='opportunity']:has(a[href])",
        "div[class*='card']:has(a[href])",
        "div[class*='item']:has(a[href])",
        "div[class*='listing']:has(a[href])",
        "div[class*='posting']:has(a[href])"
    };

    // ✅ ДОДАНО: Селектори для навігаційних елементів (щоб їх виключати)
    public static final String[] NAVIGATION_ELEMENTS = new String[]{
        "[data-testid=search]",
        "[data-testid=navigation]",
        "[data-testid=header]",
        "[data-testid=footer]",
        "[data-testid=sidebar]",
        "[data-testid=filter]",
        "[data-testid=pagination]",
        ".navigation",
        ".header",
        ".footer",
        ".sidebar",
        ".filter",
        ".pagination",
        "nav",
        "header",
        "footer",
        "aside"
    };

    // ✅ ДОДАНО: Селектори для фільтрів та пошуку (щоб їх виключати)
    public static final String[] FILTER_ELEMENTS = new String[]{
        "[data-testid=filter]",
        "[data-testid=search]",
        "[data-testid=sort]",
        "[data-testid=dropdown]",
        ".filter",
        ".search",
        ".sort",
        ".dropdown",
        "select",
        "input[type='search']",
        "input[type='text']"
    };

    // Селектори для кнопок та посилань (щоб їх виключати)
    public static final String[] BUTTON_ELEMENTS = new String[]{
        "[data-testid=button]",
        "[data-testid=link]",
        "button",
        "a:not([href*='/jobs/']):not([href*='/companies/'])",
        ".btn",
        ".button",
        ".link"
    };

    // Селектори для заголовків вакансій (використовуються в JobIngestServiceImpl та CombinedJobIngestService)
    public static final String[] JOB_TITLE = new String[]{
        "[itemprop='title']",
        "div[itemprop='title']",
        "div[font-size*='2,3'][color='text.dark'][font-weight='medium']",
        "div.sc-beqWaB.kToBwF"
    };

    // Селектори для назв організацій (використовуються в JobIngestServiceImpl та CombinedJobIngestService)
    public static final String[] ORG_NAME = new String[]{
        // ✅ ОНОВЛЕНО: Точні селектори на основі реального HTML
        "[itemprop='name']",
        "meta[itemprop='name']"
    };


    // Селектори для логотипів організацій (використовуються в JobIngestServiceImpl та CombinedJobIngestService)
    public static final String[] ORG_LOGO = new String[]{
         "img[data-testid=image]",
        "[data-testid=profile-picture] img",
        "[data-testid=company-logo] img",
        "[data-testid=organization-logo] img",
        ".organization-logo img",
        "img[alt*='logo']",
        "img[alt*='company']",
        "img[alt*='organization']"
    };

    // Селектори для функцій вакансій (використовуються в JobIngestServiceImpl)
    public static final String[] JOB_FUNCTION = new String[]{
        "[data-testid=job-function]",
        "[data-testid=position-function]",
        "[data-testid=role-function]",
        "[itemprop='jobFunction']",
        "meta[itemprop='jobFunction']"
    };

    // Селектори для тегів (використовуються в ApplyUrlScraperServiceImpl, JobIngestServiceImpl)
    public static final String[] TAGS = new String[]{
        "[data-testid=tag]",
        "div[data-testid=tag]",
        "div.sc-dmqHEX.OHsAR",
        "div.sc-dmqHEX.XKhIJ"
    };

    // Селектори для локацій (використовуються в JobIngestServiceImpl)
    public static final String[] LOCATION = new String[]{
        "[itemprop='address']",
        "meta[itemprop='address']",
        "div.sc-beqWaB.sc-gueYoa.ictnPY.MYFxR",
        "div.sc-beqWaB.ewYjoF",
        "span.sc-beqWaB.vIGjl",
    };

    // Селектори для дат публікації (використовуються в JobIngestServiceImpl)
    public static final String[] POSTED_DATE = new String[]{

        "meta[itemprop='datePosted']",
        "[itemprop='datePosted']",
        
        // Селектори для div елементів з датою (на основі зображення)
        "div.sc-beqWaB.enQFes",
        "div[class*='enQFes']",
        "div[font-size='1'][color='text.main']",
        
        // Селектори для тексту дати
        "div:contains('days ago')",
        "div:contains('Today')",
        "div:contains('Yesterday')",
        "div:contains('Posted')",
    };

    // Селектори для описів (використовуються в JobIngestServiceImpl)
    public static final String[] DESCRIPTION = new String[]{
        "[data-testid=careerPage]",
        "[data-testid=careerPage] section",
        "[data-testid=careerPage] div",
        "[data-testid=job-description]",
        "[data-testid=position-description]",
        "[data-testid=vacancy-description]",
        
        // ✅ ДОДАНО: Селектори для карток вакансій
        "[data-testid=job-card] [data-testid=description]",
        "[data-testid=job-card] [data-testid=about]",
        "[data-testid=job-card] [data-testid=summary]",
        "[data-testid=job-card] .job-description",
        "[data-testid=job-card] .position-description",
        "[data-testid=job-card] .vacancy-description",
        
        // ✅ ДОДАНО: Селектори для внутрішнього контенту карток
        "div[class*='job-info'] [data-testid=description]",
        "div[class*='job-info'] [data-testid=about]",
        "div[class*='job-info'] [data-testid=summary]",
        "div[class*='job-info'] .description",
        "div[class*='job-info'] .about",
        "div[class*='job-info'] .summary",
        
        // ✅ ДОДАНО: Селектори на основі наданої HTML структури
        "div.sc-beqWaB.sc-gueYoa.lpllVF.MYFxR [data-testid=description]",
        "div.sc-beqWaB.sc-gueYoa.lpllVF.MYFxR [data-testid=about]",
        "div.sc-beqWaB.sc-gueYoa.lpllVF.MYFxR [data-testid=summary]",
        "div.sc-beqWaB.sc-gueYoa.lpllVF.MYFxR .description",
        "div.sc-beqWaB.sc-gueYoa.lpllVF.MYFxR .about",
        "div.sc-beqWaB.sc-gueYoa.lpllVF.MYFxR .summary",
        
        // ✅ ДОДАНО: Селектори для пошуку в div з класом job-info
        "div.job-info [data-testid=description]",
        "div.job-info [data-testid=about]",
        "div.job-info [data-testid=summary]",
        "div.job-info .description",
        "div.job-info .about",
        "div.job-info .summary",
        "meta[itemprop='description']",
        "[itemprop='description']",

    };

    // Селектори для посилань на подачу заявки (використовуються в ApplyUrlScraperServiceImpl)
    public static final String[] APPLY_OR_READ_MORE = new String[]{
        "a[data-testid=read-more][href]",
        "a[data-testid=apply][href]",
        "a[data-testid=apply-now][href]",
        "a[href][aria-label*='Read more']",
        "a[href][aria-label*='Apply']",
        "a:matchesOwn(Read more)",
        "a:matchesOwn(Apply)",
        "a:matchesOwn(Apply Now)",
    };

    // Селектори для посилань на картки вакансій (використовуються в DirectJobScraperServiceImpl)
    public static final String[] JOB_CARD_LINK = new String[]{
        // ✅ ОНОВЛЕНО: Точні селектори на основі реального HTML
        "a[data-testid='job-card-link']",
        "a[data-testid='job-link']",
        "a[data-testid='position-link']",
        "a[data-testid='vacancy-link']",
        "a[href*='/jobs/']",
        "a[href*='/companies/']",
        "a[href*='techstars.com']",
    };

    // Селектори для кнопки Load More
    public static final String[] LOAD_MORE_BUTTON = new String[]{
        "button[data-testid='load-more']",
        "button[data-testid='show-more']",
        "button:contains('Load more')",
        "button:contains('Load More')",
        "button:contains('Show more')",
        "button:contains('Show More')",
        "a[data-testid='load-more']",
        "a[data-testid='show-more']",
        "a:contains('Load more')",
        "a:contains('Show more')",
    };

    // ✅ ДОДАНО: Селектори для пошуку вакансій на різних сторінках
    public static final String[] JOB_LISTING_PAGE = new String[]{
        // Головна сторінка зі списком вакансій
        "https://jobs.techstars.com/jobs",
        
        // Сторінки компаній з вакансіями
        "https://jobs.techstars.com/companies/*/jobs",
        
        // Детальні сторінки вакансій
        "https://jobs.techstars.com/companies/*/jobs/*"
    };

    // ✅ ДОДАНО: Селектори для пошуку карток вакансій на головній сторінці
    public static final String[] MAIN_PAGE_JOB_CARDS = new String[]{
        // Картки вакансій на головній сторінці /jobs
        "div[data-testid=job-card]",
        "div[data-testid=job-item]",
        "div[data-testid=job]",
        "div[data-testid=position]",
        "div[data-testid=vacancy]",
        "div[data-testid=opportunity]",
        
        // Картки з посиланнями на вакансії
        "div:has(a[href*='/jobs/'])",
        "div:has(a[href*='/companies/'])",
        
        // Картки з класами
        "div.sc-dmqHEX.bpXRKw",
        "div.sc-beqWaB.jfIxNQ",
        "div[class*='sc-']:has(a[href])"
    };

    // ✅ ДОДАНО: Селектори для пошуку вакансій на сторінці компанії
    public static final String[] COMPANY_PAGE_JOBS = new String[]{
        // Список вакансій на сторінці компанії
        "div[data-testid=job-listing]",
        "div[data-testid=job-item]",
        "div[data-testid=position-item]",
        "div[data-testid=vacancy-item]",
        
        // Картки вакансій з посиланнями
        "div:has(a[href*='/jobs/'])",
        "div:has(a[href*='/companies/'])",
        
        // Загальні селектори
        "div[class*='job']:has(a[href])",
        "div[class*='position']:has(a[href])",
        "div[class*='vacancy']:has(a[href])"
    };

    // ✅ ДОДАНО: Селектори для детальної сторінки вакансії
    public static final String[] JOB_DETAIL_PAGE = new String[]{
        // Заголовок вакансії
        "[itemprop='title']",
        "div[itemprop='title']",
        "h1[itemprop='title']",
        "h2[itemprop='title']",
        
        // Назва компанії
        "[itemprop='name']",
        "meta[itemprop='name']",
        
        // Теги
        "[data-testid=tag]",
        "div[data-testid=tag]",
        "div.sc-dmqHEX.OHsAR",
        "div.sc-dmqHEX.XKhIJ",
        
        // Локація
        "[itemprop='address']",
        "meta[itemprop='address']",
        "div.sc-beqWaB.sc-gueYoa.ictnPY.MYFxR",
        "span.sc-beqWaB.vIGjl",
        
        // Дата публікації
        "[itemprop='datePosted']",
        "meta[itemprop='datePosted']",
        "div.sc-beqWaB.enQFes"
    };
}