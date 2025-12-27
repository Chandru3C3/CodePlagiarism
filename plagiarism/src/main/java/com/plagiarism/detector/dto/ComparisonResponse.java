package com.plagiarism.detector.dto;

import java.util.List;

public class ComparisonResponse {
	private String file1Name;
	private String file2Name;
	private Double overallSimilarity;
	private Double tokenSimilarity;
	private Double structuralSimilarity;
	private String status;
	private List<MatchSegment> matches;

	// Default Constructor
	public ComparisonResponse() {
	}

	// Parameterized Constructor
	public ComparisonResponse(String file1Name, String file2Name, Double overallSimilarity, Double tokenSimilarity,
			Double structuralSimilarity, String status, List<MatchSegment> matches) {
		this.file1Name = file1Name;
		this.file2Name = file2Name;
		this.overallSimilarity = overallSimilarity;
		this.tokenSimilarity = tokenSimilarity;
		this.structuralSimilarity = structuralSimilarity;
		this.status = status;
		this.matches = matches;
	}

	// Getters and Setters
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

	public List<MatchSegment> getMatches() {
		return matches;
	}

	public void setMatches(List<MatchSegment> matches) {
		this.matches = matches;
	}
}
