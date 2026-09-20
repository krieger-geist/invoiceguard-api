package com.invoiceguard.common.util;

/**
 * Simple string similarity helpers shared by the duplicate-detection engine
 * and a couple of risk rules. Deliberately dependency-free (no external NLP
 * library) — the similarity needs here are shallow (near-identical invoice
 * numbers/PO numbers, rough description overlap), not semantic search, so a
 * classic edit-distance/token-overlap approach is appropriate and keeps the
 * risk engine's core logic auditable without a black-box library in the mix.
 */
public final class StringSimilarity {

    private StringSimilarity() {}

    /** Classic Levenshtein edit distance. */
    public static int levenshteinDistance(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    /** 1.0 = identical, 0.0 = completely different. Normalized by the longer string's length. */
    public static double normalizedSimilarity(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) {
            return 1.0;
        }
        return 1.0 - ((double) levenshteinDistance(a, b) / maxLen);
    }

    /** Jaccard similarity over lowercase word sets — used for coarse description comparison. */
    public static double wordOverlapSimilarity(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) {
            return 0.0;
        }
        var wordsA = java.util.Set.of(a.toLowerCase().split("\\W+"));
        var wordsB = java.util.Set.of(b.toLowerCase().split("\\W+"));
        var union = new java.util.HashSet<>(wordsA);
        union.addAll(wordsB);
        if (union.isEmpty()) {
            return 0.0;
        }
        var intersection = new java.util.HashSet<>(wordsA);
        intersection.retainAll(wordsB);
        return (double) intersection.size() / union.size();
    }
}
