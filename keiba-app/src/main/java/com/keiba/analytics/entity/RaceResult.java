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
@Table(name = "race_results")
public class RaceResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ★リレーション: 複数の結果が、1つのレースに紐づく
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    // ★リレーション: 複数の結果が、1頭の馬に紐づく
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horse_id", nullable = false)
    private Horse horse;

    @Column(name = "row_order")
    private Integer rowOrder; // 着順（1, 2, 3... 取消などは99など）

    @Column(name = "bracket_number")
    private Integer bracketNumber; // 枠番 (1〜8)

    @Column(name = "horse_number")
    private Integer horseNumber; // 馬番

    @Column(name = "jockey_name")
    private String jockeyName; // 騎手名

    private Double weight; // 斤量 (57.0 など)
    
    @Column(name = "horse_weight")
    private Integer horseWeight;

    @Column(name = "weight_change")
    private Integer weightChange;

    @Column(name = "race_time")
    private String raceTime; // 走破タイム（例 "1:32.4"）

    // ゲッター、セッター
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Race getRace() { return race; }
    public void setRace(Race race) { this.race = race; }

    public Horse getHorse() { return horse; }
    public void setHorse(Horse horse) { this.horse = horse; }

    public Integer getRowOrder() { return rowOrder; }
    public void setRowOrder(Integer rowOrder) { this.rowOrder = rowOrder; }

    public Integer getBracketNumber() { return bracketNumber; }
    public void setBracketNumber(Integer bracketNumber) { this.bracketNumber = bracketNumber; }

    public Integer getHorseNumber() { return horseNumber; }
    public void setHorseNumber(Integer horseNumber) { this.horseNumber = horseNumber; }

    public String getJockeyName() { return jockeyName; }
    public void setJockeyName(String jockeyName) { this.jockeyName = jockeyName; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public String getRaceTime() { return raceTime; }
    public void setRaceTime(String raceTime) { this.raceTime = raceTime; }

    public Integer getHorseWeight() { return horseWeight; }
    public void setHorseWeight(Integer horseWeight) { this.horseWeight = horseWeight; }

    public Integer getWeightChange() { return weightChange; }
    public void setWeightChange(Integer weightChange) { this.weightChange = weightChange; }
    }