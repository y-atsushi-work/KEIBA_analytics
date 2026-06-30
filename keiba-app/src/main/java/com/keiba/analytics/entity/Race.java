package com.keiba.analytics.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "races")
public class Race {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String netkeibaRaceId; // netkeibaの12桁のレースID (例: "202504010601")
    private LocalDate raceDate;    // 開催日
    private String raceName;       // レース名
    private String location;       // 開催場 (例: 中山, 東京)
    private Integer raceNumber;    // 何レース目か (例: 11)
    private String trackType;      // トラック (芝, ダート, 障害)
    private Integer distance;      // 距離 (m)
    private String weather;        // 天候 (晴, 曇, 雨)
    private String trackCondition; // 馬場状態 (良, 稍重, 重, 不良)
    @OneToMany(mappedBy = "race", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RaceResult> raceResults = new ArrayList<>();

    // ==========================================
    // ゲッター / セッター
    // ==========================================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNetkeibaRaceId() { return netkeibaRaceId; }
    public void setNetkeibaRaceId(String netkeibaRaceId) { this.netkeibaRaceId = netkeibaRaceId; }

    public LocalDate getRaceDate() { return raceDate; }
    public void setRaceDate(LocalDate raceDate) { this.raceDate = raceDate; }

    public String getRaceName() { return raceName; }
    public void setRaceName(String raceName) { this.raceName = raceName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Integer getRaceNumber() { return raceNumber; }
    public void setRaceNumber(Integer raceNumber) { this.raceNumber = raceNumber; }

    public String getTrackType() { return trackType; }
    public void setTrackType(String trackType) { this.trackType = trackType; }

    public Integer getDistance() { return distance; }
    public void setDistance(Integer distance) { this.distance = distance; }

    public String getWeather() { return weather; }
    public void setWeather(String weather) { this.weather = weather; }

    public String getTrackCondition() { return trackCondition; }
    public void setTrackCondition(String trackCondition) { this.trackCondition = trackCondition; }
    
    public List<RaceResult> getRaceResults() { return raceResults; }
    public void setRaceResults(List<RaceResult> raceResults) { this.raceResults = raceResults; }
}