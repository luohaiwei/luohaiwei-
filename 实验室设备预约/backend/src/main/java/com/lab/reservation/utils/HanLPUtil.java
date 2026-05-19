package com.lab.reservation.utils;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;

import java.util.ArrayList;
import java.util.List;

/**
 * HanLP 自然语言处理工具类
 * 用于AI问答中的意图识别与关键词提取
 */
public class HanLPUtil {

    /**
     * 使用HanLP提取用户问题中的关键词
     * 用于与知识库进行语义匹配
     */
    public static List<String> extractKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<String> keywords = new ArrayList<>();
        try {
            // 使用HanLP进行分词
            Segment segment = HanLP.newSegment();
            for (Term term : segment.seg(text)) {
                String word = term.word.trim();
                if (word.length() >= 2 && !isPunctuation(word)) {
                    keywords.add(word);
                }
            }
            // 关键词提取（取前5个）
            List<String> extracted = HanLP.extractKeyword(text, 5);
            if (extracted != null) {
                for (String kw : extracted) {
                    if (!keywords.contains(kw)) {
                        keywords.add(kw);
                    }
                }
            }
        } catch (Exception e) {
            String[] parts = text.split("[\\s,，。？！、；：]+");
            for (String p : parts) {
                if (p.length() >= 2) {
                    keywords.add(p);
                }
            }
        }
        return keywords;
    }

    /**
     * 计算两个文本的相似度（基于关键词重叠）
     */
    public static double calculateSimilarity(String text1, String text2) {
        List<String> kw1 = extractKeywords(text1);
        List<String> kw2 = extractKeywords(text2);
        if (kw1.isEmpty() || kw2.isEmpty()) return 0;
        int matchCount = 0;
        for (String k : kw1) {
            if (kw2.contains(k)) matchCount++;
        }
        return (double) matchCount / Math.max(kw1.size(), kw2.size());
    }

    private static boolean isPunctuation(String s) {
        if (s.length() != 1) return false;
        char c = s.charAt(0);
        return c == '，' || c == '。' || c == '！' || c == '？' || c == '、' || c == '；' || c == '：'
                || c == ',' || c == '.' || c == '!' || c == '?';
    }
}
