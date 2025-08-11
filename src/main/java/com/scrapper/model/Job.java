package com.scrapper.model;

import com.scrapper.validation.Validation;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
    name = "jobs")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    private String positionName;
    @Column(nullable = false, unique = true)
    private String jobPageUrl;
    @Column(nullable = false)
    private String organizationUrl;
    private String logoUrl;
    @Column(nullable = false)
    private String organizationTitle;
    @Column(nullable = false)
    private String laborFunction;
    @Column(nullable = false)
    private String address;
    @Column(nullable = false)
    private long postedDate; // ✅ ЗМІНЕНО: Unix Timestamp замість LocalDateTime
    @Column(nullable = false)
    private String description;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "job_locations", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "location")
    @Builder.Default
    private List<String> locations = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "job_tags", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "tag_name")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    /**
     * ✅ ДОДАНО: Конвертує LocalDateTime в Unix Timestamp
     */
    public void setPostedDateFromLocalDateTime(LocalDateTime localDateTime) {
        if (localDateTime != null) {
            this.postedDate = localDateTime.toEpochSecond(ZoneOffset.UTC);
        } else {
            this.postedDate = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        }
    }

    /**
     * ✅ ДОДАНО: Конвертує Unix Timestamp в LocalDateTime
     */
    public LocalDateTime getPostedDateAsLocalDateTime() {
        return LocalDateTime.ofEpochSecond(this.postedDate, 0, ZoneOffset.UTC);
    }

    /**
     * ✅ ДОДАНО: Встановлює поточний час як Unix Timestamp
     */
    public void setCurrentTimeAsPostedDate() {
        this.postedDate = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
    }

    public void addTag(String tag){
        if(Validation.NOT_BLANK.test(tag)){
            this.tags.add(tag.trim());
        }
    }

    public void isValid(){
        Validation.IS_VALID.test(this);
    }

    public void addLocation(String location){
        if(Validation.NOT_BLANK.test(location)){
            this.locations.add(location.trim());
        }
    }
}
