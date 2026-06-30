package com.keiba.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "horses")
public class Horse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // netkeibaの馬ID（例: 2021105154）を一意に保存
    @Column(name = "netkeiba_horse_id", unique = true, nullable = false)
    private String netkeibaHorseId;

    @Column(name = "horse_name", nullable = false)
    private String horseName;

    private String sex; // 牡、牝、セン

    private Integer age; // 年齢

    // ゲッター、セッター
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNetkeibaHorseId() { return netkeibaHorseId; }
    public void setNetkeibaHorseId(String netkeibaHorseId) { this.netkeibaHorseId = netkeibaHorseId; }

    public String getHorseName() { return horseName; }
    public void setHorseName(String horseName) { this.horseName = horseName; }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
}