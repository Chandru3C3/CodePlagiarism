package com.plagiarism.detector.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "comparison_results")
@NamedQuery(name = "ComparisonResult.findAll", query = "SELECT d FROM ComparisonResult d")
public class ComparisonResult {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "file1_id", nullable = false)
	private Long file1Id;

	@Column(name = "file2_id", nullable = false)
	private Long file2Id;

	@Column(name = "file1_name", nullable = false)
	private String file1Name;

	@Column(name = "file2_name", nullable = false)
	private String file2Name;

	@Column(name = "overall_similarity", nullable = false)
	private Double overallSimilarity;

	@Column(name = "token_similarity", nullable = false)
	private Double tokenSimilarity;

	@Column(name = "structural_similarity", nullable = false)
	private Double structuralSimilarity;

	@Column(name = "status", nullable = false)
	private String status;

	@Column(name = "matching_segments", columnDefinition = "TEXT")
	private String matchingSegments;

	@Column(name = "compared_at", nullable = false)
	private LocalDateTime comparedAt;

	// Default Constructor
	public ComparisonResult() {
	}

	// Parameterized Constructor
	public ComparisonResult(Long file1Id, Long file2Id, String file1Name, String file2Name, Double overallSimilarity,
			Double tokenSimilarity, Double structuralSimilarity, String status, String matchingSegments,
			LocalDateTime comparedAt) {
		this.file1Id = file1Id;
		this.file2Id = file2Id;
		this.file1Name = file1Name;
		this.file2Name = file2Name;
		this.overallSimilarity = overallSimilarity;
		this.tokenSimilarity = tokenSimilarity;
		this.structuralSimilarity = structuralSimilarity;
		this.status = status;
		this.matchingSegments = matchingSegments;
		this.comparedAt = comparedAt;
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getFile1Id() {
		return file1Id;
	}

	public void setFile1Id(Long file1Id) {
		this.file1Id = file1Id;
	}

	public Long getFile2Id() {
		return file2Id;
	}

	public void setFile2Id(Long file2Id) {
		this.file2Id = file2Id;
	}

	public String getFile1Name() {
		return file1Name;
	}

	public void setFile1Name(String file1Name) {
		this.file1Name = file1Name;
	}

	public String getFile2Name() {
		return file2Name;
	}

	public void setFile2Name(String file2Name) {
		this.file2Name = file2Name;
	}

	public Double getOverallSimilarity() {
		return overallSimilarity;
	}

	public void setOverallSimilarity(Double overallSimilarity) {
		this.overallSimilarity = overallSimilarity;
	}

	public Double getTokenSimilarity() {
		return tokenSimilarity;
	}

	public void setTokenSimilarity(Double tokenSimilarity) {
		this.tokenSimilarity = tokenSimilarity;
	}

	public Double getStructuralSimilarity() {
		return structuralSimilarity;
	}

	public void setStructuralSimilarity(Double structuralSimilarity) {
		this.structuralSimilarity = structuralSimilarity;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMatchingSegments() {
		return matchingSegments;
	}

	public void setMatchingSegments(String matchingSegments) {
		this.matchingSegments = matchingSegments;
	}

	public LocalDateTime getComparedAt() {
		return comparedAt;
	}

	public void setComparedAt(LocalDateTime comparedAt) {
		this.comparedAt = comparedAt;
	}

	@Override
	public String toString() {
		return "ComparisonResult{" + "id=" + id + ", file1Name='" + file1Name + '\'' + ", file2Name='" + file2Name
				+ '\'' + ", overallSimilarity=" + overallSimilarity + ", status='" + status + '\'' + ", comparedAt="
				+ comparedAt + '}';
	}
}