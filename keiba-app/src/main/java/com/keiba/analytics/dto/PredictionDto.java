package com.keiba.analytics.dto;
import lombok.Data;

@Data
public class PredictionDto {
    private String netkeibaHorseId;
    private Double score;
}