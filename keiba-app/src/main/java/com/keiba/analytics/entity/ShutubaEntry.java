package com.keiba.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "shutuba_entries")
public class ShutubaEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shutuba_race_id", nullable = false)
    private ShutubaRace shutubaRace;

    // 出走馬のID
    @Column(name = "netkeiba_horse_id")
    private String netkeibaHorseId;
    private String horseName;
    private Integer bracketNumber; // 枠番
    private Integer horseNumber;   // 馬番
    private String jockey;         // 騎手（予定）
    private String carriedWeight;  // 斤量
    private String sexAge;         // 性齢
    private String horseWeight;    // 馬体重(増減)
    private Double odds;           // 予想オッズ
    private Integer popularity;    // 人気

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ShutubaRace getShutubaRace() { return shutubaRace; }
    public void setShutubaRace(ShutubaRace shutubaRace) { this.shutubaRace = shutubaRace; }
    public String getNetkeibaHorseId() { return netkeibaHorseId; }
    public void setNetkeibaHorseId(String netkeibaHorseId) { this.netkeibaHorseId = netkeibaHorseId; }
    public String getHorseName() { return horseName; }
    public void setHorseName(String horseName) { this.horseName = horseName; }
    public Integer getBracketNumber() { return bracketNumber; }
    public void setBracketNumber(Integer bracketNumber) { this.bracketNumber = bracketNumber; }
    public Integer getHorseNumber() { return horseNumber; }
    public void setHorseNumber(Integer horseNumber) { this.horseNumber = horseNumber; }
    public String getJockey() { return jockey; }
    public void setJockey(String jockey) { this.jockey = jockey; }
    public String getCarriedWeight() { return carriedWeight; }
    public void setCarriedWeight(String weight) { this.carriedWeight = weight; }
    public String getSexAge() { return sexAge; }
    public void setSexAge(String sexAge) { this.sexAge = sexAge; }
    public String getHorseWeight() { return horseWeight; }
    public void setHorseWeight(String horseWeight) { this.horseWeight = horseWeight; }
    public Double getOdds() { return odds; }
    public void setOdds(Double odds) { this.odds = odds; }
    public Integer getPopularity() { return popularity; }
    public void setPopularity(Integer popularity) { this.popularity = popularity; }
}