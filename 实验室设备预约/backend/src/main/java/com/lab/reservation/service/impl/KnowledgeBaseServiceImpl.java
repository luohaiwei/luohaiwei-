package com.lab.reservation.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.reservation.entity.AiChatLog;
import com.lab.reservation.entity.KnowledgeBase;
import com.lab.reservation.mapper.AiChatLogMapper;
import com.lab.reservation.mapper.KnowledgeBaseMapper;
import com.lab.reservation.service.KnowledgeBaseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.reservation.utils.HanLPUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 知识库Service实现类
 * 集成HanLP自然语言处理，实现意图识别与语义匹配
 * 优化：同义词处理、问题归一化、自适应阈值
 */
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** 从答案文本中解析「1. xxx」「1、xxx」等编号行，生成前端 OperationGuide 所需的 JSON */
    private static final Pattern NUMBERED_STEP = Pattern.compile("^\\s*(\\d+)[\\.．、）)]\\s*(.+)$");

    /** 语义匹配基础阈值 */
    private static final double SIMILARITY_THRESHOLD = 0.3;

    /** 高置信度阈值 */
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.7;

    /** 同义词映射 */
    private static final Map<String, List<String>> SYNONYMS = new HashMap<>();

    static {
        // 设备操作相关
        SYNONYMS.put("开机", Arrays.asList("启动", "开启", "打开电源", "power on"));
        SYNONYMS.put("关机", Arrays.asList("关闭", "停止", "关闭电源", "power off", "关机"));
        SYNONYMS.put("校准", Arrays.asList("校正", "标定", "调校", "calibration"));
        SYNONYMS.put("操作", Arrays.asList("使用", "运用", "操控", "操作步骤"));
        SYNONYMS.put("预约", Arrays.asList("预定", "预订", "申请使用"));
        SYNONYMS.put("取消", Arrays.asList("撤销", "删除", "作废"));
        SYNONYMS.put("故障", Arrays.asList("问题", "损坏", "出错", "异常", "报错", "error"));
        SYNONYMS.put("维修", Arrays.asList("修理", "检修", "维护"));

        // 安全相关
        SYNONYMS.put("安全", Arrays.asList("危险", "警告", "注意", "防护", "安全操作"));
        SYNONYMS.put("高压", Arrays.asList("高电压", "高压电"));
        SYNONYMS.put("高温", Arrays.asList("高热", "高温状态"));
        SYNONYMS.put("有毒", Arrays.asList("毒性", "有毒物质", "化学品"));
    }

    @Autowired
    private AiChatLogMapper aiChatLogMapper;

    @Override
    public String smartQuery(String question, Long userId) {
        long startTime = System.currentTimeMillis();

        QueryWrapper<KnowledgeBase> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        List<KnowledgeBase> allList = list(wrapper);

        // 问题归一化处理
        String normalizedQuestion = normalizeQuestion(question);

        // 使用HanLP提取用户问题关键词
        List<String> userKeywords = HanLPUtil.extractKeywords(normalizedQuestion);

        // 添加同义词
        Set<String> expandedKeywords = expandWithSynonyms(userKeywords);

        KnowledgeBase matched = null;
        double maxSimilarity = 0;

        for (KnowledgeBase kb : allList) {
            double score = 0;

            // 1. 关键词匹配（传统方式，兼容无HanLP场景）
            if (StrUtil.isNotBlank(kb.getKeywords())) {
                List<String> kbKeywords = Arrays.asList(kb.getKeywords().split(","));
                Set<String> kbExpanded = expandWithSynonyms(kbKeywords);

                for (String keyword : kbKeywords) {
                    String kw = keyword.trim();
                    if (normalizedQuestion.contains(kw)) {
                        score += 0.5;
                        break;
                    }
                }

                // 同义词匹配
                for (String ekw : expandedKeywords) {
                    if (kbExpanded.contains(ekw)) {
                        score += 0.4;
                        break;
                    }
                }

                // HanLP关键词匹配
                for (String ukw : userKeywords) {
                    if (kbExpanded.contains(ukw) || kbKeywords.stream().anyMatch(k -> k.contains(ukw) || ukw.contains(k))) {
                        score += 0.35;
                        break;
                    }
                }
            }

            // 2. 问题匹配
            if (StrUtil.isNotBlank(kb.getQuestion())) {
                String kbNormQuestion = normalizeQuestion(kb.getQuestion());
                if (normalizedQuestion.equals(kbNormQuestion)) {
                    score += 0.8; // 完全匹配
                } else if (normalizedQuestion.contains(kbNormQuestion) || kbNormQuestion.contains(normalizedQuestion)) {
                    score += 0.5; // 部分包含
                }

                // 语义相似度
                double sim = HanLPUtil.calculateSimilarity(normalizedQuestion, kbNormQuestion);
                score += sim * 0.6;
            }

            // 3. 关键词语义相似度
            if (StrUtil.isNotBlank(kb.getKeywords())) {
                double sim = HanLPUtil.calculateSimilarity(normalizedQuestion, kb.getKeywords());
                score += sim * 0.4;
            }

            // 4. 危险关键词加权（安全类问题优先级大幅提升）
            boolean hasDangerKeyword = containsDangerKeywords(normalizedQuestion);
            if (hasDangerKeyword) {
                if (kb.getIsDanger() != null && kb.getIsDanger() == 1) {
                    // 危险类问题强力加权，确保危险查询优先返回危险答案
                    score *= 3.0;
                    // 如果问题中包含"不戴"、"直接接触"、"无防护"等否定词，额外加分
                    if (normalizedQuestion.contains("不戴") || normalizedQuestion.contains("无") 
                        || normalizedQuestion.contains("直接") || normalizedQuestion.contains("没有")) {
                        score += 0.5;
                    }
                } else {
                    // 非危险类记录在检测到危险关键词时降权
                    score *= 0.5;
                }
            }

            if (score > maxSimilarity && score >= SIMILARITY_THRESHOLD) {
                maxSimilarity = score;
                matched = kb;
            }
        }

        String answer;
        long responseTime = System.currentTimeMillis() - startTime;

        if (matched != null) {
            answer = matched.getAnswer();
            saveChatLog(userId, question, answer, matched.getId(), matched.getDeviceId(), maxSimilarity, responseTime);
            if (matched.getIsDanger() != null && matched.getIsDanger() == 1) {
                answer = "⚠️ 安全警告：" + answer;
            }
            // 添加置信度提示
            if (maxSimilarity < HIGH_CONFIDENCE_THRESHOLD) {
                answer += "\n\n💡 提示：以上回复根据相似问题推断，如需更准确信息请联系管理员。";
            }
        } else {
            answer = "抱歉，我无法理解您的问题。请尝试以下方式：\n" +
                    "1. 使用更简洁的关键词描述（如：设备名称+操作类型）\n" +
                    "2. 查看设备操作手册\n" +
                    "3. 联系实验室管理员\n" +
                    "4. 拨打设备维护热线";
            saveChatLog(userId, question, answer, null, null, maxSimilarity, responseTime);
        }
        return answer;
    }

    @Override
    public Map<String, Object> smartQueryWithDangerLevel(String question, Long userId, String category) {
        long startTime = System.currentTimeMillis();

        QueryWrapper<KnowledgeBase> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        List<KnowledgeBase> allList = list(wrapper);

        // 问题归一化处理
        String normalizedQuestion = normalizeQuestion(question);

        // 使用HanLP提取用户问题关键词
        List<String> userKeywords = HanLPUtil.extractKeywords(normalizedQuestion);

        // 添加同义词
        Set<String> expandedKeywords = expandWithSynonyms(userKeywords);

        KnowledgeBase matched = null;
        double maxSimilarity = 0;

        for (KnowledgeBase kb : allList) {
            double score = 0;

            // 分类匹配加权（用户点了左侧分类时，提升同类知识优先级）
            boolean categoryMatch = false;
            if (category != null && !category.isEmpty() && !"常见问题".equals(category)) {
                if (kb.getCategory() != null && kb.getCategory().equals(category)) {
                    score += 0.5; // 分类匹配额外加权
                    categoryMatch = true;
                }
            }

            // 1. 关键词匹配（传统方式，兼容无HanLP场景）
            if (StrUtil.isNotBlank(kb.getKeywords())) {
                List<String> kbKeywords = Arrays.asList(kb.getKeywords().split(","));
                Set<String> kbExpanded = expandWithSynonyms(kbKeywords);

                for (String keyword : kbKeywords) {
                    String kw = keyword.trim();
                    if (normalizedQuestion.contains(kw)) {
                        score += 0.5;
                        break;
                    }
                }

                // 同义词匹配
                for (String ekw : expandedKeywords) {
                    if (kbExpanded.contains(ekw)) {
                        score += 0.4;
                        break;
                    }
                }

                // HanLP关键词匹配
                for (String ukw : userKeywords) {
                    if (kbExpanded.contains(ukw) || kbKeywords.stream().anyMatch(k -> k.contains(ukw) || ukw.contains(k))) {
                        score += 0.35;
                        break;
                    }
                }
            }

            // 2. 问题匹配
            if (StrUtil.isNotBlank(kb.getQuestion())) {
                String kbNormQuestion = normalizeQuestion(kb.getQuestion());
                if (normalizedQuestion.equals(kbNormQuestion)) {
                    score += 0.8; // 完全匹配
                } else if (normalizedQuestion.contains(kbNormQuestion) || kbNormQuestion.contains(normalizedQuestion)) {
                    score += 0.5; // 部分包含
                }

                // 语义相似度
                double sim = HanLPUtil.calculateSimilarity(normalizedQuestion, kbNormQuestion);
                score += sim * 0.6;
            }

            // 3. 关键词语义相似度
            if (StrUtil.isNotBlank(kb.getKeywords())) {
                double sim = HanLPUtil.calculateSimilarity(normalizedQuestion, kb.getKeywords());
                score += sim * 0.4;
            }

            // 4. 危险关键词加权（安全类问题优先级提升）
            if (containsDangerKeywords(normalizedQuestion)) {
                if (kb.getIsDanger() != null && kb.getIsDanger() == 1) {
                    score *= 1.2; // 危险类问题加权
                }
            }

            if (score > maxSimilarity && score >= SIMILARITY_THRESHOLD) {
                maxSimilarity = score;
                matched = kb;
            }
        }

        String answer;
        String dangerLevel = "info";
        long responseTime = System.currentTimeMillis() - startTime;

        if (matched != null) {
            answer = matched.getAnswer();

            // 判断危险等级
            if (matched.getIsDanger() != null && matched.getIsDanger() == 1) {
                if (normalizedQuestion.contains("高压") || normalizedQuestion.contains("有毒") || normalizedQuestion.contains("着火") || normalizedQuestion.contains("爆炸")) {
                    dangerLevel = "danger"; // 紧急危险
                    answer = "⚠️ 安全警告：" + answer;
                } else {
                    dangerLevel = "warning"; // 一般警告
                    answer = "⚠️ 警告：" + answer;
                }
            }

            saveChatLog(userId, question, answer, matched.getId(), matched.getDeviceId(), maxSimilarity, responseTime);

            // 添加置信度提示
            if (maxSimilarity < HIGH_CONFIDENCE_THRESHOLD) {
                answer += "\n\n💡 提示：以上回复根据相似问题推断，如需更准确信息请联系管理员。";
            }
        } else {
            // 未匹配到知识时，也要检查问题是否包含危险关键词
            if (containsDangerKeywordsForWarning(question)) {
                dangerLevel = "danger";
                answer = "⚠️ 安全警告：检测到您的问题可能涉及危险操作！\n\n" +
                        "建议您：\n" +
                        "1. 立即停止当前操作\n" +
                        "2. 远离危险区域\n" +
                        "3. 立即联系实验室管理人员\n" +
                        "4. 如有紧急情况请拨打校园报警电话\n\n" +
                        "如需了解安全操作规范，请联系管理员添加相关知识。";
                saveChatLog(userId, question, answer, null, null, maxSimilarity, responseTime);
            } else {
                answer = "抱歉，我无法理解您的问题。请尝试以下方式：\n" +
                        "1. 使用更简洁的关键词描述（如：设备名称+操作类型）\n" +
                        "2. 查看设备操作手册\n" +
                        "3. 联系实验室管理员\n" +
                        "4. 拨打设备维护热线";
                saveChatLog(userId, question, answer, null, null, maxSimilarity, responseTime);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        result.put("dangerLevel", dangerLevel);

        // 操作引导：优先库中 guide_steps；历史数据多为 NULL，则从答案中的编号步骤自动生成 JSON
        // 同时检查 guideType 是否为 IMAGE 或 guideSteps 不为空
        if (matched != null) {
            boolean hasGuideContent = StrUtil.isNotBlank(matched.getGuideSteps());
            // guideType 为 IMAGE 或 guideSteps 不为空都视为有引导
            boolean isImageGuide = "IMAGE".equals(matched.getGuideType());
            if (hasGuideContent || isImageGuide) {
                String effectiveGuide = hasGuideContent
                        ? matched.getGuideSteps()
                        : buildGuideStepsJsonFromAnswer(matched.getAnswer());
                if (StrUtil.isNotBlank(effectiveGuide)) {
                    result.put("hasGuide", true);
                    result.put("guideSteps", effectiveGuide);
                    result.put("guideType", matched.getGuideType());
                    int duration = matched.getGuideDuration() != null && matched.getGuideDuration() > 0
                            ? matched.getGuideDuration()
                            : estimateGuideDurationMinutes(effectiveGuide);
                    result.put("guideDuration", duration);
                } else {
                    result.put("hasGuide", false);
                }
            } else {
                result.put("hasGuide", false);
            }
        } else {
            result.put("hasGuide", false);
        }

        return result;
    }

    /**
     * 从知识库答案中的编号行生成引导步骤 JSON（与前端 OperationGuide 解析格式一致）
     */
    private String buildGuideStepsJsonFromAnswer(String rawAnswer) {
        if (StrUtil.isBlank(rawAnswer)) {
            return null;
        }
        List<Map<String, Object>> steps = new ArrayList<>();
        for (String line : rawAnswer.split("\\r?\\n")) {
            Matcher m = NUMBERED_STEP.matcher(line.trim());
            if (m.find()) {
                int n = Integer.parseInt(m.group(1));
                String body = m.group(2).trim();
                Map<String, Object> step = new LinkedHashMap<>();
                step.put("step", n);
                step.put("title", "第" + n + "步");
                step.put("content", body);
                steps.add(step);
            }
        }
        if (steps.isEmpty()) {
            return null;
        }
        try {
            return JSON.writeValueAsString(steps);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private int estimateGuideDurationMinutes(String guideStepsJson) {
        try {
            List<?> arr = JSON.readValue(guideStepsJson, List.class);
            int n = arr != null ? arr.size() : 0;
            return Math.max(1, Math.min(30, (n + 1) / 2));
        } catch (Exception e) {
            return 5;
        }
    }

    /**
     * 问题归一化处理
     */
    private String normalizeQuestion(String question) {
        if (question == null) return "";
        String normalized = question.toLowerCase()
                .replace("？", "?")
                .replace("！", "!")
                .replace("。", ".")
                .replace("，", ",")
                .replace("：", ":")
                .replace(";", ",")
                .replace("、", ",")
                .replace("  ", " ")
                .trim();
        return normalized;
    }

    /**
     * 同义词扩展
     */
    private Set<String> expandWithSynonyms(Collection<String> keywords) {
        Set<String> expanded = new HashSet<>(keywords);
        for (String keyword : keywords) {
            // 检查是否是某个同义词组的主词
            for (Map.Entry<String, List<String>> entry : SYNONYMS.entrySet()) {
                if (entry.getValue().contains(keyword) || entry.getKey().equals(keyword)) {
                    expanded.add(entry.getKey());
                    expanded.addAll(entry.getValue());
                }
            }
        }
        return expanded;
    }

    /**
     * 检查是否包含危险关键词（用于未匹配知识时的预警）
     */
    private boolean containsDangerKeywordsForWarning(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        // 紧急危险关键词
        String[] dangerKeywords = {
            "高压", "高压电", "触电", "电击",
            "有毒", "毒气", "中毒", "化学品",
            "着火", "火灾", "燃烧", "起火",
            "爆炸", "爆破", "炸药", "易爆",
            "辐射", "放射性",
            "腐蚀", "腐蚀性"
        };
        for (String kw : dangerKeywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    /**
     * 检查是否包含危险关键词（原有方法）
     */
    private boolean containsDangerKeywords(String text) {
        String[] dangerKeywords = {"高压", "高温", "有毒", "危险", "安全", "注意", "警告", "易燃", "易爆", "腐蚀"};
        for (String kw : dangerKeywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    @Override
    public List<KnowledgeBase> searchByKeyword(String keyword) {
        // 先用原关键词搜索
        List<KnowledgeBase> results = baseMapper.searchByKeywords(keyword);

        // 再用同义词搜索
        Set<Long> resultIds = results.stream().map(KnowledgeBase::getId).collect(Collectors.toSet());
        List<KnowledgeBase> synonymResults = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : SYNONYMS.entrySet()) {
            if (entry.getValue().contains(keyword) || entry.getKey().equals(keyword)) {
                List<KnowledgeBase> synResults = baseMapper.searchByKeywords(entry.getKey());
                for (KnowledgeBase kb : synResults) {
                    if (!resultIds.contains(kb.getId())) {
                        synonymResults.add(kb);
                        resultIds.add(kb.getId());
                    }
                }
            }
        }

        results.addAll(synonymResults);
        return results;
    }

    @Override
    public List<KnowledgeBase> listByCategory(String category) {
        return baseMapper.selectByCategory(category);
    }

    @Override
    public List<KnowledgeBase> getDangerAlerts() {
        return baseMapper.selectDangerAlerts();
    }

    @Override
    public List<KnowledgeBase> listByDeviceId(Long deviceId) {
        return baseMapper.selectByDeviceId(deviceId);
    }

    @Override
    public IPage<KnowledgeBase> pageManage(int pageNum, int pageSize, String category, String keyword, Integer status) {
        Page<KnowledgeBase> page = new Page<>(pageNum, pageSize);
        QueryWrapper<KnowledgeBase> wrapper = new QueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq("category", category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("question", keyword).or().like("answer", keyword).or().like("keywords", keyword));
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");
        return baseMapper.selectPage(page, wrapper);
    }

    /**
     * 保存聊天记录
     */
    private void saveChatLog(Long userId, String question, String answer, Long knowledgeId, Long deviceId, double confidence, long responseTime) {
        AiChatLog chatLog = new AiChatLog();
        chatLog.setUserId(userId);
        chatLog.setQuestion(question);
        chatLog.setAnswer(answer);
        chatLog.setKnowledgeId(knowledgeId);
        chatLog.setDeviceId(deviceId);
        chatLog.setConfidence(confidence);
        chatLog.setResponseTime(responseTime);
        aiChatLogMapper.insert(chatLog);
    }
}
