package com.ominal.app;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Lightweight ranking for the chat drawer search surface. */
final class OminalChatSearch {
    private OminalChatSearch() {
    }

    static int score(String query, String title, List<String> messages) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) return 1;

        String normalizedTitle = normalize(title);
        String[] terms = normalizedQuery.split(" ");
        int score = 0;
        if (normalizedTitle.equals(normalizedQuery)) score = 1200;
        else if (normalizedTitle.startsWith(normalizedQuery)) score = 1000;
        else if (normalizedTitle.contains(normalizedQuery)) score = 850;

        int titleTerms = matchingTerms(normalizedTitle, terms);
        if (titleTerms == terms.length) score = Math.max(score, 720 + terms.length * 20);
        else score = Math.max(score, titleTerms * 130);
        if (isSubsequence(normalizedQuery, normalizedTitle)) score = Math.max(score, 180);

        List<String> safeMessages = messages == null ? Collections.emptyList() : messages;
        for (int index = safeMessages.size() - 1; index >= 0; index--) {
            String message = normalize(safeMessages.get(index));
            if (message.isEmpty()) continue;
            int messageScore = 0;
            if (message.contains(normalizedQuery)) messageScore = 560;
            int messageTerms = matchingTerms(message, terms);
            if (messageTerms == terms.length)
                messageScore = Math.max(messageScore, 420 + terms.length * 15);
            else messageScore = Math.max(messageScore, messageTerms * 70);
            if (isSubsequence(normalizedQuery, message)) messageScore = Math.max(messageScore, 90);
            if (messageScore > 0) {
                int recencyBoost = Math.max(0, 28 - (safeMessages.size() - 1 - index));
                score = Math.max(score, messageScore + recencyBoost);
            }
        }
        return score;
    }

    static String normalize(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return decomposed.toLowerCase(Locale.US)
            .replaceAll("[^a-z0-9]+", " ")
            .trim()
            .replaceAll("\\s+", " ");
    }

    private static int matchingTerms(String text, String[] terms) {
        if (text.isEmpty()) return 0;
        int matches = 0;
        for (String term : terms) {
            if (!term.isEmpty() && text.contains(term)) matches++;
        }
        return matches;
    }

    private static boolean isSubsequence(String query, String candidate) {
        if (query.length() < 3 || candidate.isEmpty()) return false;
        int queryIndex = 0;
        for (int index = 0; index < candidate.length() && queryIndex < query.length(); index++) {
            if (candidate.charAt(index) == query.charAt(queryIndex)) queryIndex++;
        }
        return queryIndex == query.length();
    }
}
