import json
import os
import mysql.connector
import numpy as np
from sklearn.linear_model import LinearRegression, LogisticRegression
from sklearn.impute import SimpleImputer

def time_to_seconds(time_str):
    """ '1:25.7' のようなタイム文字列を秒数（float）に変換する """
    if not time_str or time_str == "-":
        return None
    try:
        parts = time_str.split(":")
        if len(parts) == 2:
            return float(parts[0]) * 60 + float(parts[1])
        elif len(parts) == 1:
            return float(parts[0])
    except Exception:
        return None
    return None

def generate_predictions():
    # データベースに接続
    conn = mysql.connector.connect(
        host="127.0.0.1",
        port=3307,
        user="root",
        password="root",
        database="mydatabase"
    )
    
    try:
        cursor = conn.cursor(dictionary=True)
        
        # ==========================================
        # 1. 【モデル1：スピード指数用のデータ準備】
        # ==========================================
        cursor.execute("""
            SELECT 
                r.location, 
                r.distance, 
                AVG(
                    CASE 
                        WHEN rr.race_time LIKE '%:%' THEN 
                            CAST(SUBSTRING_INDEX(rr.race_time, ':', 1) AS UNSIGNED) * 60 + 
                            CAST(SUBSTRING_INDEX(rr.race_time, ':', -1) AS DECIMAL(5,2))
                        ELSE NULL 
                    END
                ) as avg_time
            FROM race_results rr
            JOIN races r ON rr.race_id = r.id
            WHERE rr.race_time IS NOT NULL AND rr.race_time != '-'
            GROUP BY r.location, r.distance
        """)
        benchmarks = {}
        for row in cursor.fetchall():
            if row["location"] and row["distance"] and row["avg_time"]:
                benchmarks[(row["location"], row["distance"])] = float(row["avg_time"])

        # ==========================================
        # 2. 【モデル2・3：機械学習用の過去データ収集】
        # ==========================================
        cursor.execute("""
            SELECT 
                weight,
                last_3f_time,
                row_order
            FROM race_results
            WHERE weight IS NOT NULL 
              AND last_3f_time IS NOT NULL 
              AND row_order IS NOT NULL
        """)
        train_rows = cursor.fetchall()
        
        reg_model = LinearRegression()
        clf_model = LogisticRegression()
        is_ml_trained = False
        
        if len(train_rows) >= 5:
            X_train = []
            y_reg_train = []
            y_clf_train = []
            
            for row in train_rows:
                try:
                    w = float(row["weight"])
                    l3f = float(row["last_3f_time"])
                    order = float(row["row_order"])
                    
                    X_train.append([w, l3f])
                    # 回帰用（着順が良いほど高得点）
                    y_reg_train.append(max(1.0, 20.0 - order))
                    # 分類用（1着なら1、それ以外は0）
                    y_clf_train.append(1 if order == 1 else 0)
                except ValueError:
                    continue
            
            if len(X_train) >= 5:
                imputer = SimpleImputer(strategy='mean')
                X_train_imputed = imputer.fit_transform(X_train)
                
                # 回帰モデル（得点化）の学習
                reg_model.fit(X_train_imputed, y_reg_train)
                
                # ロジスティック回帰（勝率予測）の学習
                # ※ 0または1のクラスが両方存在する場合のみ学習可能
                if len(set(y_clf_train)) > 1:
                    clf_model.fit(X_train_imputed, y_clf_train)
                
                is_ml_trained = True
                print("機械学習モデル（回帰・ロジスティック）の学習が完了しました。")
# ==========================================
        # 3. 【出走予定馬のデータ取得と3モデルのスコア算出】
        # ==========================================
        # 各出走馬の「実際の斤量」等を取得する
        cursor.execute("""
            SELECT 
                netkeiba_horse_id,
                CAST(carried_weight AS DECIMAL(5,2)) as carried_weight
            FROM shutuba_entries
            WHERE netkeiba_horse_id IS NOT NULL
        """)
        entry_weights = {}
        for row in cursor.fetchall():
            if row["netkeiba_horse_id"]:
                entry_weights[row["netkeiba_horse_id"]] = float(row["carried_weight"]) if row["carried_weight"] else 55.0

        # 各馬の過去タイム情報を取得 (Horseテーブルを経由して netkeiba_horse_id で正しく紐付ける)
        cursor.execute("""
            SELECT 
                h.netkeiba_horse_id,
                r.location,
                r.distance,
                rr.race_time,
                rr.last_3f_time
            FROM race_results rr
            JOIN horses h ON rr.horse_id = h.id
            JOIN races r ON rr.race_id = r.id
            WHERE rr.race_time IS NOT NULL AND rr.race_time != '-'
        """)
        entry_history = cursor.fetchall()
        
        horse_speed_scores = {}
        horse_l3f_history = {} # 馬ごとの過去上がり3F平均用
        
        for entry in entry_history:
            h_id = entry["netkeiba_horse_id"]
            t_sec = time_to_seconds(entry["race_time"])
            if not t_sec:
                continue
            b_time = benchmarks.get((entry["location"], entry["distance"]), 80.0)
            s_idx = max(0.0, min(150.0, (b_time - t_sec) * 10.0 + 50.0))
            
            if h_id not in horse_speed_scores:
                horse_speed_scores[h_id] = []
            horse_speed_scores[h_id].append(s_idx)
            
            # 過去の上がり3Fも蓄積しておく
            if entry["last_3f_time"] is not None:
                if h_id not in horse_l3f_history:
                    horse_l3f_history[h_id] = []
                horse_l3f_history[h_id].append(float(entry["last_3f_time"]))

        # ユニークな出走馬一覧を取得
        cursor.execute("SELECT DISTINCT netkeiba_horse_id FROM shutuba_entries WHERE netkeiba_horse_id IS NOT NULL")
        unique_horses = cursor.fetchall()
        
        prediction_data = []
        
        for h in unique_horses:
            horse_id = h["netkeiba_horse_id"]
            
            # --- モデル1: スピード指数スコア ---
            if horse_id in horse_speed_scores and horse_speed_scores[horse_id]:
                raw_speed = sum(horse_speed_scores[horse_id]) / len(horse_speed_scores[horse_id])
                speed_score = round(max(0.0, min(100.0, raw_speed)), 1)
            else:
                speed_score = 50.0
            
            # --- モデル2 & 3 用の特徴量（馬ごとの実データを使用） ---
            w_val = entry_weights.get(horse_id, 55.0) # 実際の斤量（取れなければ55.0）
            
            # その馬の過去の平均上がり3F、なければ全体のデフォルト
            if horse_id in horse_l3f_history and horse_l3f_history[horse_id]:
                l3f_val = sum(horse_l3f_history[horse_id]) / len(horse_l3f_history[horse_id])
            else:
                l3f_val = 35.0 
            
            # 機械学習による予測計算
            if is_ml_trained:
                X_pred = np.array([[w_val, l3f_val]])
                
                # モデル2: 回帰分析スコア
                pred_reg = reg_model.predict(X_pred)[0]
                regression_score = round(max(0.0, min(100.0, float(pred_reg) * 5.0)), 1)
                
                # モデル3: ロジスティック回帰
                try:
                    proba = clf_model.predict_proba(X_pred)[0][1]
                    win_prob_score = round(proba * 100.0, 1)
                except Exception:
                    win_prob_score = 50.0
            else:
                regression_score = 50.0
                win_prob_score = 50.0
            
            # --- 総合スコアの合算 ---
            combined_score = round(
                (speed_score * 0.4) + 
                (regression_score * 0.3) + 
                (win_prob_score * 0.3), 
            1)
            
            prediction_data.append({
                "netkeibaHorseId": horse_id,
                "score": combined_score
            })
            print(f"馬ID: {horse_id} | 斤量: {w_val} | 上がり3F: {l3f_val:.1f} | スピード: {speed_score} | 回帰: {regression_score} | 勝率: {win_prob_score} | 総合: {combined_score}")
            
    finally:
        cursor.close()
        conn.close()

    # 保存先（絶対パス）
    output_dir = "/Users/atsushi/KEIBA_analytics/keiba-app/src/main/resources/data"
    output_file = os.path.join(output_dir, "prediction_scores.json")
    
    os.makedirs(output_dir, exist_ok=True)

    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(prediction_data, f, ensure_ascii=False, indent=4)
        
    print(f"3つの予測モデル（スピード・回帰・勝率）を統合し、{len(prediction_data)} 頭の総合スコアを算出して保存しました。")

if __name__ == "__main__":
    generate_predictions()