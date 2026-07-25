import pandas as pd
import json

def calculate_speed_index(df):
    """
    簡単なスピード指数の計算ロジック例
    （実際はレースごとに基準タイムなどを計算して算出します）
    """
    # タイムを秒数に変換する仮定の処理
    # 例として「1:32.4」のような文字列を数値に変換する計算などをここに入れる
    df['speed_index'] = 100 - (df['row_order'] * 2)  # サンプル計算：着順が良いほど指数が高い
    return df

def run_analysis():
    # 1. データを取得 (fetch_data.pyのロジックを流用)
    # 実際は全データを取得してレースごとに計算する
    # ここでは仮のデータフレームとして処理
    data = {'raceId': ['202606280511']*5, 'horseNumber': [1, 2, 3, 4, 5], 'row_order': [1, 2, 3, 4, 5]}
    df = pd.DataFrame(data)
    
    # 2. 指数計算を実行
    df = calculate_speed_index(df)
    
    # 3. JSONに変換して保存
    result_list = df[['raceId', 'horseNumber', 'speed_index']].to_dict(orient='records')
    
    # Javaプロジェクトのresources配下に書き出し
    output_path = "../keiba-app/src/main/resources/data/analysis_results.json"
    
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(result_list, f, ensure_ascii=False, indent=4)
    print("分析結果をJSONに出力しました。")

if __name__ == "__main__":
    run_analysis()