package com.plagiarism.detector.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.text.similarity.CosineSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.plagiarism.detector.dto.MatchSegment;

@Service
public class PlagiarismDetectionService {

	private final JavaParser javaParser;

	public PlagiarismDetectionService() {
		this.javaParser = new JavaParser();
	}

	/**
	 * Tokenize code - remove comments, normalize, extract tokens
	 */
	public List<String> tokenizeCode(String code) {
		// Remove comments
		code = removeComments(code);

		// Normalize whitespace
		code = normalizeWhitespace(code);

		// Split into tokens
		String[] tokens = code.split("[\\s\\{\\}\\(\\)\\[\\];,\\.]+");
		List<String> result = new ArrayList<>();

		for (String token : tokens) {
			if (!token.isEmpty() && !isKeyword(token)) {
				result.add(token.toLowerCase());
			}
		}

		return result;
	}

	/**
	 * Remove comments from code
	 */
	private String removeComments(String code) {
		// Remove single line comments
		code = code.replaceAll("//.*?\\n", "\n");

		// Remove multi-line comments
		code = code.replaceAll("/\\*.*?\\*/", "");

		return code;
	}

	/**
	 * Normalize whitespace
	 */
	private String normalizeWhitespace(String code) {
		return code.replaceAll("\\s+", " ").trim();
	}

	/**
	 * Check if token is a programming keyword
	 */
	private boolean isKeyword(String token) {
		Set<String> keywords = new HashSet<>(Arrays.asList("abstract", "assert", "boolean", "break", "byte", "case",
				"catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends",
				"final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
				"interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short",
				"static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try",
				"void", "volatile", "while", "true", "false", "null"));
		return keywords.contains(token.toLowerCase());
	}

	/**
	 * Calculate token-based similarity (Jaccard Index)
	 */
	public double calculateTokenSimilarity(String code1, String code2) {
		List<String> tokens1 = tokenizeCode(code1);
		List<String> tokens2 = tokenizeCode(code2);

		if (tokens1.isEmpty() && tokens2.isEmpty()) {
			return 100.0;
		}

		if (tokens1.isEmpty() || tokens2.isEmpty()) {
			return 0.0;
		}

		Set<String> set1 = new HashSet<>(tokens1);
		Set<String> set2 = new HashSet<>(tokens2);

		// Calculate intersection
		Set<String> intersection = new HashSet<>(set1);
		intersection.retainAll(set2);

		// Calculate union
		Set<String> union = new HashSet<>(set1);
		union.addAll(set2);

		if (union.isEmpty()) {
			return 0.0;
		}

		// Jaccard similarity
		return (double) intersection.size() / union.size() * 100.0;
	}

	/**
	 * Calculate cosine similarity
	 */
	public double calculateCosineSimilarity(String code1, String code2) {
		Map<CharSequence, Integer> vec1 = tokenToVector(code1);
		Map<CharSequence, Integer> vec2 = tokenToVector(code2);

		if (vec1.isEmpty() && vec2.isEmpty()) {
			return 100.0;
		}

		if (vec1.isEmpty() || vec2.isEmpty()) {
			return 0.0;
		}

		CosineSimilarity cosine = new CosineSimilarity();
		Double similarity = cosine.cosineSimilarity(vec1, vec2);

		return similarity * 100.0;
	}

	/**
	 * Convert text to vector representation
	 */
	private Map<CharSequence, Integer> tokenToVector(String text) {
		Map<CharSequence, Integer> vector = new HashMap<>();
		String[] words = text.split("\\s+");

		for (String word : words) {
			if (!word.isEmpty()) {
				vector.put(word, vector.getOrDefault(word, 0) + 1);
			}
		}

		return vector;
	}

	/**
	 * Calculate Levenshtein distance similarity
	 */
	public double calculateLevenshteinSimilarity(String code1, String code2) {
		if (code1.isEmpty() && code2.isEmpty()) {
			return 100.0;
		}

		LevenshteinDistance lev = new LevenshteinDistance();
		Integer distance = lev.apply(code1, code2);

		int maxLen = Math.max(code1.length(), code2.length());
		if (maxLen == 0) {
			return 100.0;
		}

		return (1.0 - (double) distance / maxLen) * 100.0;
	}

	/**
	 * Calculate structural similarity using AST (for Java)
	 */
	public double calculateStructuralSimilarity(String javaCode1, String javaCode2) {
		try {
			Optional<CompilationUnit> cu1Optional = javaParser.parse(javaCode1).getResult();
			Optional<CompilationUnit> cu2Optional = javaParser.parse(javaCode2).getResult();

			if (!cu1Optional.isPresent() || !cu2Optional.isPresent()) {
				return 0.0;
			}

			CompilationUnit cu1 = cu1Optional.get();
			CompilationUnit cu2 = cu2Optional.get();

			String ast1 = cu1.toString();
			String ast2 = cu2.toString();

			return calculateCosineSimilarity(ast1, ast2);

		} catch (Exception e) {
			System.err.println("Error in AST parsing: " + e.getMessage());
			return 0.0;
		}
	}

	/**
	 * Calculate overall similarity (weighted average)
	 */
	public double calculateOverallSimilarity(String code1, String code2, String language) {
		double tokenSim = calculateTokenSimilarity(code1, code2);
		double cosineSim = calculateCosineSimilarity(code1, code2);
		double structuralSim = 0.0;

		// Calculate structural similarity for Java
		if ("java".equalsIgnoreCase(language)) {
			structuralSim = calculateStructuralSimilarity(code1, code2);
		}

		// Weighted average
		if (structuralSim > 0) {
			return (tokenSim * 0.4 + cosineSim * 0.3 + structuralSim * 0.3);
		} else {
			return (tokenSim * 0.6 + cosineSim * 0.4);
		}
	}

	/**
	 * Find matching code segments
	 */
	public List<MatchSegment> findMatchingSegments(String code1, String code2) {
		List<MatchSegment> matches = new ArrayList<>();
		String[] lines1 = code1.split("\n");
		String[] lines2 = code2.split("\n");

		int segmentSize = 10;
		double threshold = 70.0;

		for (int i = 0; i < lines1.length - segmentSize; i++) {
			StringBuilder segment1Builder = new StringBuilder();
			for (int k = i; k < Math.min(i + segmentSize, lines1.length); k++) {
				segment1Builder.append(lines1[k]).append("\n");
			}
			String segment1 = segment1Builder.toString();

			for (int j = 0; j < lines2.length - segmentSize; j++) {
				StringBuilder segment2Builder = new StringBuilder();
				for (int k = j; k < Math.min(j + segmentSize, lines2.length); k++) {
					segment2Builder.append(lines2[k]).append("\n");
				}
				String segment2 = segment2Builder.toString();

				double similarity = calculateCosineSimilarity(segment1, segment2);

				if (similarity > threshold) {
					String line1Range = (i + 1) + "-" + Math.min(i + segmentSize, lines1.length);
					String line2Range = (j + 1) + "-" + Math.min(j + segmentSize, lines2.length);

					String snippet1 = segment1.substring(0, Math.min(100, segment1.length()));
					String snippet2 = segment2.substring(0, Math.min(100, segment2.length()));

					MatchSegment match = new MatchSegment(line1Range, line2Range, similarity, snippet1, snippet2);

					matches.add(match);
				}
			}
		}

		return matches;
	}

	/**
	 * Determine status based on similarity percentage
	 */
	public String determineStatus(double similarity) {
		if (similarity >= 80.0) {
			return "HIGH";
		} else if (similarity >= 60.0) {
			return "MEDIUM";
		} else {
			return "LOW";
		}
	}
}