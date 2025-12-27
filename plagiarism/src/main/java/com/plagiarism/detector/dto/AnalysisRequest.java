package com.plagiarism.detector.dto;

import java.util.List;

public class AnalysisRequest {
	private List<Long> submissionIds;

	// Default Constructor
	public AnalysisRequest() {
	}

	// Parameterized Constructor
	public AnalysisRequest(List<Long> submissionIds) {
		this.submissionIds = submissionIds;
	}

	// Getters and Setters
	public List<Long> getSubmissionIds() {
		return submissionIds;
	}

	public void setSubmissionIds(List<Long> submissionIds) {
		this.submissionIds = submissionIds;
	}
}

