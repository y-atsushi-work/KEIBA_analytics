package com.keiba.analytics.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource; // これを追加
import org.springframework.stereotype.Service;

import com.keiba.analytics.dto.AnalysisResultDto;
import com.keiba.analytics.dto.PredictionDto;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class AnalysisService {
    
    // 引数にパスを受け取る必要がなくなります（resources/data直下にある前提）
    public List<AnalysisResultDto> getAnalysisResults() throws Exception {
        // src/main/resources/data/analysis_results.json を探す
        ClassPathResource resource = new ClassPathResource("data/analysis_results.json");
        
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(resource.getInputStream(), new TypeReference<List<AnalysisResultDto>>(){});
    }
    
    //
    public Map<String, Double> getPredictionScores() throws Exception {
        // ※分析結果のJSON形式を PredictionDto のリストと想定しています
        ClassPathResource resource = new ClassPathResource("data/prediction_scores.json");
        ObjectMapper mapper = new ObjectMapper();
        List<PredictionDto> list = mapper.readValue(resource.getInputStream(), new TypeReference<List<PredictionDto>>(){});

        // netkeibaHorseIdをキー、scoreを値にしたMapに変換
        return list.stream().collect(Collectors.toMap(PredictionDto::getNetkeibaHorseId, PredictionDto::getScore));
    }
}