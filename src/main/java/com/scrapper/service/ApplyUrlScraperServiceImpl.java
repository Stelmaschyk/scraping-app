package com.scrapper.service;

import com.scrapper.model.JobFunction;
import com.scrapper.util.ScrapingSelectors;
import com.scrapper.util.TechstarsFilterUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplyUrlScraperServiceImpl implements ApplyUrlScraperService {

    private final OkHttpClient httpClient;

    @Value("${scraping.base-url:https://jobs.techstars.com/jobs}")
    private String baseUrl;

    @Value("${scraping.delay-between-requests:500}")
    private long delayBetweenRequests;

    private static final String REQUIRED_PREFIX = "https://jobs.techstars.com/companies/";

    @Override
    public List<String> fetchApplyUrls(List<JobFunction> jobFunctions, List<String> requiredTags) {
        Objects.requireNonNull(jobFunctions, "jobFunctions");
        requiredTags = requiredTags == null ? List.of() : requiredTags.stream().map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.toList());

        String listUrl = TechstarsFilterUtil.buildFilterUrl(baseUrl, jobFunctions);
        Set<String> results = new LinkedHashSet<>();
        int page = 1;
        int stableRounds = 0;
        while (true) {
            String pageUrl = listUrl + (listUrl.contains("?") ? "&" : "?") + "page=" + page;
            Document doc = fetch(pageUrl);
            if (doc == null) break;
            int before = results.size();
            extractFromPage(doc, requiredTags, results);
            if (results.size() == before) {
                stableRounds++;
            } else {
                stableRounds = 0;
            }
            // Heuristic stop if no growth 2 consecutive pages
            if (stableRounds >= 2) break;
            page++;
            sleep(delayBetweenRequests);
        }
        // also try base page without page param (page=1 may differ)
        Document baseDoc = fetch(listUrl);
        if (baseDoc != null) extractFromPage(baseDoc, requiredTags, results);
        return new ArrayList<>(results);
    }

    private void extractFromPage(Document doc, List<String> requiredTags, Set<String> out) {
        Elements cards = selectAny(doc, ScrapingSelectors.JOB_CARD);
        for (Element card : cards) {
            List<String> cardTags = selectAny(card, ScrapingSelectors.TAGS).eachText().stream()
                .map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.toList());
            if (!requiredTags.isEmpty() && !cardTags.containsAll(requiredTags)) continue;
            String href = firstAbsHref(card, ScrapingSelectors.APPLY_OR_READ_MORE);
            if (href != null && href.startsWith(REQUIRED_PREFIX)) out.add(href);
        }
    }

    private Document fetch(String url) {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            String body = response.body() != null ? response.body().string() : null;
            if (body == null) return null;
            return Jsoup.parse(body, url);
        } catch (IOException e) {
            log.warn("Failed to fetch {}: {}", url, e.toString());
            return null;
        }
    }

    private Elements selectAny(Element root, String[] selectors) {
        Elements sum = new Elements();
        for (String css : selectors) {
            Elements found = root.select(css);
            if (!found.isEmpty()) sum.addAll(found);
        }
        return sum;
    }

    private String firstAbsHref(Element root, String[] selectors) {
        for (String css : selectors) {
            Element a = root.selectFirst(css);
            if (a != null) {
                String abs = a.absUrl("href");
                if (abs != null && !abs.isBlank()) return abs;
                String raw = a.attr("href");
                if (raw != null && !raw.isBlank()) return raw;
            }
        }
        return null;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
