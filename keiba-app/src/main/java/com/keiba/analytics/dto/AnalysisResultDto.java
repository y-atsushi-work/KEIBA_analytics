package com.keiba.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data; // Lombokの導入

@Data // これだけでGetter, Setter, toString等が自動生成される
public class AnalysisResultDto {
	@JsonProperty("raceId") // JSONの "raceId" をこの変数にマッピングする
    private String raceId;
    private Integer horseNumber;
    private Double speedIndex;
    private Double winRate;
}