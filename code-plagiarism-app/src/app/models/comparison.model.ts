export interface ComparisonResult {
  file1Name: string;
  file2Name: string;
  overallSimilarity: number;
  tokenSimilarity: number;
  structuralSimilarity: number;
  status: string;
  matches: MatchSegment[];
}

export interface MatchSegment {
  line1Range: string;
  line2Range: string;
  similarity: number;
  codeSnippet1?: string;
  codeSnippet2?: string;
}

export interface Submission {
  id: number;
  fileName: string;
  fileContent: string;
  language: string;
  uploadedAt: string;
  uploadedBy: string;
}

export interface UploadResponse {
  message: string;
  ids: number[];
}

export interface AnalysisRequest {
  submissionIds: number[];
}