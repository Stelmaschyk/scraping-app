package com.scrapper.util;

public class ScrapingSelectors {
    private ScrapingSelectors() {
    }

    public static final String[] JOB_CARD = new String[]{
        "[data-testid=job-list-item]",
        "section[role=region] div[id^=job-summary]",
        "div[id^=job-summary]",
        "article[data-testid=job-card]",
        "div[data-testid=job-card]",
        "div[class*='JobMainSummary_Card']"
    };

    public static final String[] JOB_TITLE = new String[]{
        "[data-testid=content] h1",
        "[data-testid=content] h2",
        "h1[data-testid=job-title]",
        "[data-testid=job-title]",
        ".job-title",
        "h1",
        "h2",
        "h3 a",
        "a[title]"
    };

    public static final String[] ORG_NAME = new String[]{
        "[data-testid=company-name]",
        "[data-testid=organization-name]",
        "[data-testid=profile-picture] ~ p",
        "[class*='Company__Name']",
        "[class*='organization']",
        "p[class*='bpXRKw']"
    };

    public static final String[] CARD_LINK = new String[]{
        "a[href*='jobs.techstars.com']",
        "a[href*='http']"
    };

    public static final String[] LOCATION = new String[]{
        "[data-testid=location]",
        ".location",
        "[class*='location']",
        "[data-testid=content] div:matchesOwn(,)",
        "div[class*='bpXRKw']:matchesOwn(,)"
    };

    public static final String[] POSTED_DATE = new String[]{
        "[data-testid=posted-date]",
        ".posted-date",
        "time[datetime]",
        "[class*='posted']",
        "[data-testid=content] div:matchesOwn(^\\s*on\\s+\\w+\\s+\\d{1,2},\\s+\\d{4}\\s*$)",
        "div:matchesOwn(^\\s*on\\s+\\w+\\s+\\d{1,2},\\s+\\d{4}\\s*$)",
        "div:matchesOwn(^\\s*Posted\\s*$) ~ div"
    };

    public static final String[] TAGS = new String[]{
        "[data-testid=tag]",
        ".job-tags .tag",
        "[class*='tag']",
        "[data-testid=tag] .sc-dmqHEX.XKhIJ",
        "[data-testid=tag]",
        "[class*='tag']"
    };

    public static final String[] JOB_FUNCTION = new String[]{
        "[data-testid=job-function]",
        "[data-testid=content] div",
        "[data-testid=content] span",
        "div[class*='bpXRKw']",
        "div, span, p, li"
    };

    public static final String[] DESCRIPTION = new String[]{
        "[data-testid=careerPage]",
        "[data-testid=careerPage] section",
        "[data-testid=careerPage] div",
        "#about-job .tiptap.ProseMirror",
        "#about-job [class*='Editor__Content']",
        "#about-job [class*='About__Job__Content']",
        ".job-description",
        "[data-testid=job-description]",
        "article [class*='description']"
    };

    public static final String[] ORG_LOGO = new String[]{
        "img[data-testid=image]",
        "[data-testid=profile-picture] img",
        "[data-testid=company-logo] img",
        ".organization-logo img",
        "img[alt*='logo']"
    };

    public static final String[] ORG_LINK = new String[]{
        "a[data-testid=organization-link]",
        ".organization-link",
        "a[href*='http']:not([data-testid=job-title])"
    };

    public static final String[] APPLY_URL = new String[]{
        "a[data-testid=read-more][href]",
        "a[data-testid=button][href*='jobs.lever.co']",
        "a[data-testid=button][href*='boards.greenhouse.io']",
        "a[data-testid=button][target=_blank][href]",
        "a[href*='apply']",
        "a[role=button][href]"
    };

    public static final String[] APPLY_OR_READ_MORE = new String[]{
        "a[data-testid=read-more][href]",
        "a[href][aria-label*='Read more']",
        "a:matchesOwn(Read more)"
    };
}