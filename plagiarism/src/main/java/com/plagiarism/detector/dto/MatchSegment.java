package com.plagiarism.detector.dto;

public class MatchSegment {
	private String line1Range;
	private String line2Range;
	private Double similarity;
	private String codeSnippet1;
	private String codeSnippet2;

	// Default Constructor
	public MatchSegment() {
	}

	// Parameterized Constructor
	public MatchSegment(String line1Range, String line2Range, Double similarity, String codeSnippet1,
			String codeSnippet2) {
		this.line1Range = line1Range;
		this.line2Range = line2Range;
		this.similarity = similarity;
		this.codeSnippet1 = codeSnippet1;
		this.codeSnippet2 = codeSnippet2;
	}

	// Getters and Setters
	public String getLine1Range() {
		return line1Range;
	}

	public void setLine1Range(String line1Range) {
		this.line1Range = line1Range;
	}

	public String getLine2Range() {
		return line2Range;
	}

	public void setLine2Range(String line2Range) {
		this.line2Range = line2Range;
	}

	public Double getSimilarity() {
		return similarity;
	}

	public void setSimilarity(Double similarity) {
		this.similarity = similarity;
	}

	public String getCodeSnippet1() {
		return codeSnippet1;
	}

	public void setCodeSnippet1(String codeSnippet1) {
		this.codeSnippet1 = codeSnippet1;
	}

	public String getCodeSnippet2() {
		return codeSnippet2;
	}

	public void setCodeSnippet2(String codeSnippet2) {
		this.codeSnippet2 = codeSnippet2;
	}
}
