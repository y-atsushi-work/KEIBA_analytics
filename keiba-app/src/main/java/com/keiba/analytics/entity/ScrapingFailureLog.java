package com.keiba.analytics.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * スクレイピング失敗時のログを管理するエンティティ（テーブル）
 */
@Entity
@Table(name = "scraping_failure_log")
public class ScrapingFailureLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 取得に失敗した対象のID（レースIDや日付文字列など）
	@Column(nullable = false)
	private String targetId;

	// 発生したエラーの内容・メッセージ
	@Column(length = 1000)
	private String errorMessage;

	// 失敗した日時
	private LocalDateTime failedAt;

	// 復旧（再取得成功）したかどうか
	private boolean resolved = false;

	// ★追加: リトライを何回試みたかを記録するフィールド（デフォルトは 0）
	@Column(name = "retry_count", nullable = false)
	private int retryCount = 0;

	// コンストラクタ
	public ScrapingFailureLog() {
	}

	public ScrapingFailureLog(String targetId, String errorMessage) {
		this.targetId = targetId;
		this.errorMessage = errorMessage;
		this.failedAt = LocalDateTime.now();
		this.resolved = false;
		this.retryCount = 0;
	}

	// ゲッター・セッター
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTargetId() {
		return targetId;
	}

	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public LocalDateTime getFailedAt() {
		return failedAt;
	}

	public void setFailedAt(LocalDateTime failedAt) {
		this.failedAt = failedAt;
	}

	public boolean isResolved() {
		return resolved;
	}

	public void setResolved(boolean resolved) {
		this.resolved = resolved;
	}

	// ★追加: retryCount のゲッター・セッター
	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}
}