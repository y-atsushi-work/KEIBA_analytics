package com.keiba.analytics.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "shutuba_races")
public class ShutubaRace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 出馬表用の一時的なレースID (race.netkeiba.com のID)
    @Column(name = "netkeiba_race_id", unique = true, nullable = false)
    private String netkeibaRaceId;

    private String raceName;
    private LocalDate raceDate;
    private String location; // 競馬場
    private Integer round;   // レース番号 (1〜12)

    // レースに紐づく出走予定馬のリスト
    @OneToMany(mappedBy = "shutubaRace", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShutubaEntry> entries = new ArrayList<>();

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNetkeibaRaceId() { return netkeibaRaceId; }
    public void setNetkeibaRaceId(String netkeibaRaceId) { this.netkeibaRaceId = netkeibaRaceId; }
    public String getRaceName() { return raceName; }
    public void setRaceName(String raceName) { this.raceName = raceName; }
    public LocalDate getRaceDate() { return raceDate; }
    public void setRaceDate(LocalDate raceDate) { this.raceDate = raceDate; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getRound() { return round; }
    public void setRound(Integer round) { this.round = round; }
    public List<ShutubaEntry> getEntries() { return entries; }
    
    public void addEntry(ShutubaEntry entry) {
        entries.add(entry);
        entry.setShutubaRace(this);
    }
}