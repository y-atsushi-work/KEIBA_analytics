import mysql.connector
import pandas as pd
import os
from dotenv import load_dotenv

# .envファイルを読み込む
load_dotenv()

# MySQLへの接続
conn = mysql.connector.connect(
    host=os.getenv("DB_HOST"),
    user=os.getenv("DB_USER"),
    password=os.getenv("DB_PASSWORD"),
    database=os.getenv("DB_NAME")
)

# データを取得してPandasのDataFrameにする（CSVのように表形式で扱えます）
query = "SELECT * FROM race_results LIMIT 10"
df = pd.read_sql(query, conn)

# 取得したデータの確認
print(df.head())

# 接続を閉じる
conn.close()