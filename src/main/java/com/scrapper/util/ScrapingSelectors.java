package com.scrapper.util;

public class ScrapingSelectors {
    private ScrapingSelectors() {
    }

    // Селектори для карток вакансій (використовуються в DirectJobScraperServiceImpl та ApplyUrlScraperServiceImpl)
    public static final String[] JOB_CARD = new String[]{
        // ✅ ОНОВЛЕНО: Більш точні селектори для пошуку реальних карток вакансій
        
        // Точні селектори на основі реального HTML (найвищий пріоритет)
        "div[data-testid=job-card]",
        "div[data-testid=job-item]",
        "div[data-testid=job]",
        "div[data-testid=position]",
        "div[data-testid=vacancy]",
        "div[data-testid=opportunity]",

        // Селектори для конкретних класів з HTML (на основі реального коду)
        "div.sc-dmqHEX.bpXRKw",
        "div.sc-beqWaB.jfIxNQ",
        "div[class*='sc-']:has([data-testid=job-title])",
        "div[class*='sc-']:has([data-testid=organization-name])",
        "div[class*='sc-']:has([data-testid=job-function])",

        // Загальні селектори для карток
        "[data-testid=job-card]",
        "[data-testid=job-item]",
        "[data-testid=job]",
        "[data-testid=position]",
        "[data-testid=vacancy]",
        "[data-testid=opportunity]",

        // Селектори на основі класів
        "[class*='job-card']",
        "[class*='job-item']",
        "[class*='position-card']",
        "[class*='vacancy-card']",
        "[class*='opportunity-card']",
        "div[class*='JobCard']",
        "div[class*='JobItem']",

        // Селектори на основі структури
        "div:has([data-testid=job-title])",
        "div:has([data-testid=organization-name])",
        "div:has([data-testid=job-function])",
        "div:has([data-testid=tag])",
        "div[role=article]",
        "div[role=listitem]",

        // Запасні селектори
        ".job-card",
        ".job-item",
        ".position-card",
        ".vacancy-card",
        ".opportunity-card",
        "[data-testid=content] > div",

        // ✅ ДОДАНО: Більш точні селектори для пошуку карток з посиланнями
        "div:has(a[href*='/jobs/']):not(:has([data-testid=search])):not(:has([data-testid=navigation]))",
        "div:has(a[href*='/companies/']):not(:has([data-testid=search])):not(:has([data-testid=navigation]))",
        
        // ✅ ДОДАНО: Селектори для пошуку карток за структурою
        "div:has(h1, h2, h3):has(a[href*='/jobs/'])",
        "div:has(h1, h2, h3):has(a[href*='/companies/'])",
        
        // ✅ ДОДАНО: Селектори для пошуку карток за вмістом
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

    // ✅ ДОДАНО: Селектори для кнопок та посилань (щоб їх виключати)
    public static final String[] BUTTON_ELEMENTS = new String[]{
        "[data-testid=button]",
        "[data-testid=link]",
        "button",
        "a:not([href*='/jobs/']):not([href*='/companies/'])",
        ".btn",
        ".button",
        ".link"
    };

    // ✅ ДОДАНО: Селектори для статистики та інформації (щоб їх виключати)
    public static final String[] STATS_ELEMENTS = new String[]{
        "[data-testid=stats]",
        "[data-testid=counter]",
        "[data-testid=info]",
        ".stats",
        ".counter",
        ".info",
        ".statistics"
    };

    // Селектори для заголовків вакансій (використовуються в JobIngestServiceImpl та CombinedJobIngestService)
    public static final String[] JOB_TITLE = new String[]{
        // ✅ ОНОВЛЕНО: Точні селектори на основі реального HTML
        "[itemprop='title']",
        "div[itemprop='title']",
        "div[font-size*='2,3'][color='text.dark'][font-weight='medium']",
        "div.sc-beqWaB.kToBwF",
        
        // Запасні селектори
        "[data-testid=job-title]",
        "[data-testid=position-title]",
        "[data-testid=vacancy-title]",
        "[data-testid=opportunity-title]",
        "h1[data-testid=job-title]",
        "h2[data-testid=job-title]",
        "h3[data-testid=job-title]",
        ".job-title",
        ".position-title",
        ".vacancy-title",
        ".opportunity-title",
        "h1, h2, h3"
    };

    // Селектори для назв організацій (використовуються в JobIngestServiceImpl та CombinedJobIngestService)
    public static final String[] ORG_NAME = new String[]{
        // ✅ ОНОВЛЕНО: Точні селектори на основі реального HTML
        "[itemprop='name']",
        "meta[itemprop='name']",
        
        // Запасні селектори
        "[data-testid=organization-name]",
        "[data-testid=company-name]",
        "[data-testid=org-name]",
        "[data-testid=employer-name]",
        ".organization-name",
        ".company-name",
        ".employer-name",
        ".org-name",
        "[class*='organization']",
        "[class*='company']",
        "[class*='employer']",
        "h1, h2, h3, h4, h5, h6"
    };

    // Селектори для посилань на організації (використовуються в CombinedJobIngestService)
    public static final String[] ORG_LINK = new String[]{
        "a[data-testid=organization-link]",
        ".organization-link",
        "a[href*='http']:not([data-testid=job-title])"
    };

    // Селектори для логотипів організацій (використовуються в JobIngestServiceImpl та CombinedJobIngestService)
    public static final String[] ORG_LOGO = new String[]{
        // ✅ ОНОВЛЕНО: Точні селектори на основі реального HTML
        "img[data-testid=image]",
        "[data-testid=profile-picture] img",
        "[data-testid=company-logo] img",
        "[data-testid=organization-logo] img",
        ".organization-logo img",
        "img[alt*='logo']",
        "img[alt*='company']",
        "img[alt*='organization']",
        
        // Запасні селектори
        "img[src*='logo']",
        "img[src*='company']",
        "img[src*='organization']"
    };

    // Селектори для функцій вакансій (використовуються в JobIngestServiceImpl та CombinedJobIngestService)
    public static final String[] JOB_FUNCTION = new String[]{
        // ✅ ОНОВЛЕНО: Точні селектори на основі реального HTML
        "[data-testid=job-function]",
        "[data-testid=position-function]",
        "[data-testid=role-function]",
        "[itemprop='jobFunction']",
        "meta[itemprop='jobFunction']",
        
        // Запасні селектори
        "div[class*='sc-']:has([data-testid=job-function])",
        "[data-testid=content] div",
        "[data-testid=content] span",
        "div[class*='bpXRKw']",
        "div:has([data-testid=job-function])",
        "div:has([data-testid=position-function])",
        "div:has([data-testid=role-function])",
        "div, span, p, li",
        ".job-function",
        ".position-function",
        ".role-function"
    };

    // Селектори для тегів (використовуються в ApplyUrlScraperServiceImpl, JobIngestServiceImpl та CombinedJobIngestService)
    public static final String[] TAGS = new String[]{
        // ✅ ОНОВЛЕНО: Точні селектори на основі реального HTML
        "[data-testid=tag]",
        "div[data-testid=tag]",
        "div.sc-dmqHEX.OHsAR",
        "div.sc-dmqHEX.XKhIJ",
        
        // Запасні селектори
        "[data-testid=skill]",
        "[data-testid=technology]",
        "[data-testid=requirement]",
        ".job-tags .tag",
        ".skill-tags .tag",
        ".technology-tags .tag",
        "[class*='tag']",
        "[class*='skill']",
        "[class*='technology']",
        ".tag",
        ".skill",
        ".technology",
        ".requirement"
    };

    // Селектори для локацій (використовуються в JobIngestServiceImpl та CombinedJobIngestService)
    public static final String[] LOCATION = new String[]{
        // ✅ ОНОВЛЕНО: Точні селектори на основі реального HTML
        "[itemprop='address']",
        "meta[itemprop='address']",
        "div.sc-beqWaB.sc-gueYoa.ictnPY.MYFxR",
        "div.sc-beqWaB.ewYjoF",
        "span.sc-beqWaB.vIGjl",
        
        // Запасні селектори
        "[data-testid=location]",
        ".location",
        "[class*='location']",
        "[data-testid=content] div:matchesOwn(,)",
        "div[class*='bpXRKw']:matchesOwn(,)"
    };

    // Селектори для дат публікації (використовуються в JobIngestServiceImpl)
    public static final String[] POSTED_DATE = new String[]{
        // ✅ ОНОВЛЕНО: Точні селектори на основі реального HTML
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
        
        // Запасні селектори
        "[data-testid=posted-date]",
        ".posted-date",
        "time[datetime]",
        "[class*='posted']",
        "[data-testid=content] div:matchesOwn(^\\s*on\\s+\\w+\\s+\\d{1,2},\\s+\\d{4}\\s*$)",
        "div:matchesOwn(^\\s*on\\s+\\w+\\s+\\d{1,2},\\s+\\d{4}\\s*$)",
        "div:matchesOwn(^\\s*Posted\\s*$) ~ div"
    };

    // Селектори для описів (використовуються в JobIngestServiceImpl та CombinedJobIngestService)
    public static final String[] DESCRIPTION = new String[]{
        // ✅ ОНОВЛЕНО: Точні селектори на основі реального HTML
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
        
        // ✅ ДОДАНО: Селектори для meta тегів з описом
        "meta[itemprop='description']",
        "[itemprop='description']",
        
        // Запасні селектори
        "#about-job .tiptap.ProseMirror",
        "#about-job [class*='Editor__Content']",
        "#about-job [class*='About__Job__Content']",
        ".job-description",
        "article [class*='description']",
        "div[itemprop='description']"
    };

    // Селектори для посилань на подачу заявки (використовуються в ApplyUrlScraperServiceImpl)
    public static final String[] APPLY_OR_READ_MORE = new String[]{
        // ✅ ОНОВЛЕНО: Точні селектори на основі реального HTML
        "a[data-testid=read-more][href]",
        "a[data-testid=apply][href]",
        "a[data-testid=apply-now][href]",
        "a[href][aria-label*='Read more']",
        "a[href][aria-label*='Apply']",
        "a:matchesOwn(Read more)",
        "a:matchesOwn(Apply)",
        "a:matchesOwn(Apply Now)",
        
        // Запасні селектори
        "a[href*='apply']",
        "a[href*='application']",
        "button[data-testid=apply]",
        "button:matchesOwn(Apply)",
        ".apply-button",
        ".read-more-button"
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
        
        // Запасні селектори
        "a[href*='careers']",
        "a[href*='opportunities']",
        "a[href*='positions']"
    };

    // Селектори для кнопки Load More
    public static final String[] LOAD_MORE_BUTTON = new String[]{
        // ✅ ОНОВЛЕНО: Точні селектори на основі реального HTML
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
        
        // Запасні селектори
        "[data-testid='load-more']",
        "[data-testid='show-more']",
        ".load-more-button",
        ".show-more-button",
        "button[class*='load']",
        "button[class*='more']",
        "a[class*='load']",
        "a[class*='more']"
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