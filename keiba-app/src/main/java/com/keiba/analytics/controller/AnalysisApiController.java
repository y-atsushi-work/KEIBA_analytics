package com.keiba.analytics.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalysisApiController {

    @PostMapping("/api/generate-scores")
    public ResponseEntity<?> runGenerateScores() {
        try {
            String pythonPath = "/Users/atsushi/KEIBA_analytics/.venv/bin/python";
            String scriptPath = "/Users/atsushi/KEIBA_analytics/analysis/generate_scores.py";

            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath);
            pb.directory(new File("/Users/atsushi/KEIBA_analytics"));
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // エラー発生時のログ確認用としてメモリ上だけに保持（System.outへの毎行出力は削除）
            StringBuilder logOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logOutput.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return ResponseEntity.ok(Map.of(
                    "status", "success", 
                    "message", "予想スコアの再計算が完了しました。"
                ));
            } else {
                String details = logOutput.length() > 0 ? logOutput.toString() : "詳細ログが出力されませんでした。";
                System.err.println("❌ Python実行エラー詳細:\n" + details);

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                            "status", "error", 
                            "message", "Python実行エラー (exitCode: " + exitCode + ")\n\n【詳細ログ】\n" + details
                        ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Java例外: " + e.getMessage()));
        }
    }
}