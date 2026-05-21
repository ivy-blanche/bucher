package com.bucher.question.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bucher.common.exception.BusinessException;
import com.bucher.question.dto.QuestionBatchSaveDTO;
import com.bucher.question.dto.QuestionExcelImportDTO;
import com.bucher.question.dto.QuestionOptionSaveItem;
import com.bucher.question.dto.QuestionSaveItem;
import com.bucher.question.dto.TestCaseDTO;
import com.bucher.question.entity.Question;
import com.bucher.question.entity.QuestionGroup;
import com.bucher.question.entity.QuestionOption;
import com.bucher.question.entity.QuestionProgramming;
import com.bucher.question.entity.QuestionTestCase;
import com.bucher.question.mapper.QuestionGroupMapper;
import com.bucher.question.mapper.QuestionMapper;
import com.bucher.question.mapper.QuestionOptionMapper;
import com.bucher.question.mapper.QuestionProgrammingMapper;
import com.bucher.question.mapper.QuestionTestCaseMapper;
import com.bucher.question.service.QuestionService;
import com.bucher.question.vo.QuestionDetailVO;
import com.bucher.question.vo.QuestionImportErrorVO;
import com.bucher.question.vo.QuestionImportResultVO;
import com.bucher.question.vo.QuestionImportUploadVO;
import com.bucher.question.vo.QuestionListVO;
import com.bucher.question.vo.QuestionOptionVO;
import com.bucher.question.vo.QuestionProgrammingVO;
import com.bucher.question.vo.QuestionTestCaseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 题目 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private static final int PREVIEW_LENGTH = 100;
    private static final String[] TYPE_NAMES = {"", "单选", "多选", "填空", "简答", "编程"};

    private static final Map<String, Integer> TYPE_MAP = Map.of(
            "单选", 1, "多选", 2, "填空", 3, "简答", 4, "编程", 5
    );

    private static final Map<String, Integer> DIFFICULTY_MAP = Map.of(
            "简单", 1, "中等", 2, "困难", 3
    );

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/question-import/";

    private static final List<String> OPTION_LABELS = List.of("A", "B", "C", "D", "E", "F");

    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final QuestionGroupMapper questionGroupMapper;
    private final QuestionProgrammingMapper questionProgrammingMapper;
    private final QuestionTestCaseMapper questionTestCaseMapper;

    @Override
    public List<QuestionListVO> listByGroupId(Long groupId) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getGroupId, groupId)
                .orderByDesc(Question::getCreateTime);

        List<Question> questions = questionMapper.selectList(wrapper);

        return questions.stream()
                .map(q -> QuestionListVO.builder()
                        .id(q.getId())
                        .contentPreview(getPreview(q.getContent()))
                        .type(q.getType())
                        .typeName(getTypeName(q.getType()))
                        .difficulty(q.getDifficulty())
                        .createTime(q.getCreateTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public QuestionDetailVO getDetail(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            return null;
        }

        List<QuestionOption> options = questionOptionMapper.selectList(
                new LambdaQueryWrapper<QuestionOption>()
                        .eq(QuestionOption::getQuestionId, id)
                        .orderByAsc(QuestionOption::getSortOrder));

        QuestionProgramming programming = null;
        List<QuestionTestCase> testCases = Collections.emptyList();
        if (isProgrammingType(question.getType())) {
            programming = questionProgrammingMapper.selectOne(
                    new LambdaQueryWrapper<QuestionProgramming>()
                            .eq(QuestionProgramming::getQuestionId, id));
            testCases = questionTestCaseMapper.selectList(
                    new LambdaQueryWrapper<QuestionTestCase>()
                            .eq(QuestionTestCase::getQuestionId, id)
                            .orderByAsc(QuestionTestCase::getSortOrder));
        }

        return buildDetailVO(question, options, programming, testCases);
    }

    @Override
    public List<QuestionDetailVO> getBatchDetail(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Question> questions = questionMapper.selectBatchIds(ids);
        if (questions.isEmpty()) {
            return Collections.emptyList();
        }

        List<QuestionOption> options = questionOptionMapper.selectList(
                new LambdaQueryWrapper<QuestionOption>()
                        .in(QuestionOption::getQuestionId, ids)
                        .orderByAsc(QuestionOption::getSortOrder));

        Map<Long, List<QuestionOption>> optionMap = options.stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));

        // 批量加载编程题扩展数据
        Map<Long, QuestionProgramming> programmingMap = questionProgrammingMapper.selectList(
                        new LambdaQueryWrapper<QuestionProgramming>()
                                .in(QuestionProgramming::getQuestionId, ids))
                .stream()
                .collect(Collectors.toMap(QuestionProgramming::getQuestionId, p -> p, (a, b) -> a));

        Map<Long, List<QuestionTestCase>> testCaseMap = questionTestCaseMapper.selectList(
                        new LambdaQueryWrapper<QuestionTestCase>()
                                .in(QuestionTestCase::getQuestionId, ids)
                                .orderByAsc(QuestionTestCase::getSortOrder))
                .stream()
                .collect(Collectors.groupingBy(QuestionTestCase::getQuestionId));

        return questions.stream()
                .map(q -> buildDetailVO(q,
                        optionMap.getOrDefault(q.getId(), Collections.emptyList()),
                        programmingMap.get(q.getId()),
                        testCaseMap.getOrDefault(q.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSave(QuestionBatchSaveDTO dto, Long teacherId) {
        Long groupId = dto.getGroupId();

        for (QuestionSaveItem item : dto.getQuestions()) {
            if (Boolean.TRUE.equals(item.getDeleted())) {
                handleDelete(item.getId(), groupId);
                continue;
            }

            if (item.getId() == null) {
                handleCreate(item, groupId, teacherId);
            } else {
                handleUpdate(item);
            }
        }
    }

    @Override
    public void downloadTemplate(OutputStream outputStream) {
        QuestionExcelImportDTO example = new QuestionExcelImportDTO();
        example.setTypeName("单选");
        example.setContent("以下哪个是 Java 关键字？");
        example.setOptionA("int");
        example.setOptionB("integer");
        example.setOptionC("float");
        example.setOptionD("double");
        example.setCorrectAnswer("A");
        example.setAnalysis("int 是 Java 基本数据类型关键字");
        example.setDifficultyName("简单");
        example.setScore(2);

        EasyExcel.write(outputStream, QuestionExcelImportDTO.class)
                .sheet("题目导入模板")
                .doWrite(Collections.singletonList(example));
    }

    @Override
    public QuestionImportUploadVO uploadFile(String originalName, InputStream inputStream) {
        if (!originalName.endsWith(".xlsx")) {
            throw new BusinessException("仅支持 .xlsx 格式的 Excel 文件");
        }

        // 确保临时目录存在
        File dir = new File(TEMP_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileKey = UUID.randomUUID() + ".xlsx";
        File tempFile = new File(TEMP_DIR + fileKey);

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        } catch (IOException e) {
            log.error("保存上传文件失败", e);
            throw new BusinessException("文件上传失败");
        }

        // 读取行数
        List<QuestionExcelImportDTO> rows;
        try {
            rows = EasyExcel.read(tempFile).head(QuestionExcelImportDTO.class)
                    .sheet().doReadSync();
        } catch (Exception e) {
            tempFile.delete();
            log.error("读取 Excel 失败", e);
            throw new BusinessException("Excel 文件格式错误，请使用模板文件");
        }

        int rowCount = rows != null ? rows.size() : 0;

        return QuestionImportUploadVO.builder()
                .fileKey(fileKey)
                .originalName(originalName)
                .rowCount(rowCount)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionImportResultVO executeImport(Long groupId, String fileKey, Long teacherId) {
        // 校验分组
        QuestionGroup group = questionGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("题库分组不存在");
        }

        // 读取临时文件
        File tempFile = new File(TEMP_DIR + fileKey);
        if (!tempFile.exists()) {
            throw new BusinessException("导入文件不存在或已过期，请重新上传");
        }

        List<QuestionExcelImportDTO> rows;
        try {
            rows = EasyExcel.read(tempFile).head(QuestionExcelImportDTO.class)
                    .sheet().doReadSync();
        } catch (Exception e) {
            log.error("读取导入文件失败", e);
            throw new BusinessException("读取导入文件失败");
        }

        if (rows == null || rows.isEmpty()) {
            deleteTempFile(tempFile);
            throw new BusinessException("导入文件中没有数据");
        }

        List<QuestionImportErrorVO> errors = new ArrayList<>();
        List<QuestionSaveItem> items = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            QuestionExcelImportDTO row = rows.get(i);
            int rowNum = i + 2; // 表头占第1行，数据从第2行开始

            try {
                QuestionSaveItem item = convertRowToItem(row);
                items.add(item);
            } catch (IllegalArgumentException e) {
                errors.add(QuestionImportErrorVO.builder()
                        .row(rowNum)
                        .reason(e.getMessage())
                        .build());
            }
        }

        // 删除临时文件
        deleteTempFile(tempFile);

        if (items.isEmpty()) {
            return QuestionImportResultVO.builder()
                    .totalCount(rows.size())
                    .successCount(0)
                    .failCount(errors.size())
                    .errors(errors)
                    .build();
        }

        // 复用 batchSave
        QuestionBatchSaveDTO batchDto = new QuestionBatchSaveDTO();
        batchDto.setGroupId(groupId);
        batchDto.setQuestions(items);
        batchSave(batchDto, teacherId);

        return QuestionImportResultVO.builder()
                .totalCount(rows.size())
                .successCount(items.size())
                .failCount(errors.size())
                .errors(errors)
                .build();
    }

    /**
     * 将 Excel 行数据转换为 QuestionSaveItem
     */
    private QuestionSaveItem convertRowToItem(QuestionExcelImportDTO row) {
        // 校验题型
        Integer type = TYPE_MAP.get(row.getTypeName());
        if (type == null) {
            throw new IllegalArgumentException("题型不存在：" + row.getTypeName());
        }

        // 校验题干
        if (StrUtil.isBlank(row.getContent())) {
            throw new IllegalArgumentException("题干不能为空");
        }

        QuestionSaveItem item = new QuestionSaveItem();
        item.setType(type);
        item.setContent(row.getContent());
        item.setAnalysis(row.getAnalysis());

        // 难度
        if (StrUtil.isNotBlank(row.getDifficultyName())) {
            Integer difficulty = DIFFICULTY_MAP.get(row.getDifficultyName());
            if (difficulty == null) {
                throw new IllegalArgumentException("难度值无效：" + row.getDifficultyName());
            }
            item.setDifficulty(difficulty);
        } else {
            item.setDifficulty(2); // 默认中等
        }

        // 分值
        item.setScore(row.getScore() != null ? row.getScore() : 0);

        // 选择题：构建选项
        if (isChoiceType(type)) {
            List<QuestionOptionSaveItem> options = new ArrayList<>();
            String correctAnswer = row.getCorrectAnswer();
            boolean hasAnyOption = false;

            for (int i = 0; i < OPTION_LABELS.size(); i++) {
                String label = OPTION_LABELS.get(i);
                String optionContent = getOptionContent(row, i);
                if (StrUtil.isBlank(optionContent)) {
                    continue;
                }
                hasAnyOption = true;

                QuestionOptionSaveItem opt = new QuestionOptionSaveItem();
                opt.setLabel(label);
                opt.setContent(optionContent);
                opt.setIsCorrect(correctAnswer != null && correctAnswer.contains(label));
                opt.setSortOrder(i + 1);
                options.add(opt);
            }

            if (!hasAnyOption) {
                throw new IllegalArgumentException("选择题至少需要填写选项A和选项B");
            }
            if (options.size() < 2) {
                throw new IllegalArgumentException("选择题至少需要填写选项A和选项B");
            }

            item.setOptions(options);
        } else {
            // 填空/简答
            item.setAnswer(row.getCorrectAnswer());
        }

        return item;
    }

    private String getOptionContent(QuestionExcelImportDTO row, int index) {
        return switch (index) {
            case 0 -> row.getOptionA();
            case 1 -> row.getOptionB();
            case 2 -> row.getOptionC();
            case 3 -> row.getOptionD();
            case 4 -> row.getOptionE();
            case 5 -> row.getOptionF();
            default -> null;
        };
    }

    private void deleteTempFile(File file) {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            log.warn("删除临时文件失败: {}", file.getPath());
        }
    }

    private void handleDelete(Long questionId, Long groupId) {
        if (questionId == null) {
            return;
        }
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            return;
        }

        questionMapper.deleteById(questionId);
        questionOptionMapper.delete(
                new LambdaQueryWrapper<QuestionOption>()
                        .eq(QuestionOption::getQuestionId, questionId));

        // 级联删除编程题扩展数据
        if (isProgrammingType(question.getType())) {
            questionProgrammingMapper.delete(
                    new LambdaQueryWrapper<QuestionProgramming>()
                            .eq(QuestionProgramming::getQuestionId, questionId));
            questionTestCaseMapper.delete(
                    new LambdaQueryWrapper<QuestionTestCase>()
                            .eq(QuestionTestCase::getQuestionId, questionId));
        }

        QuestionGroup group = questionGroupMapper.selectById(groupId);
        if (group != null && group.getQuestionCount() != null && group.getQuestionCount() > 0) {
            group.setQuestionCount(group.getQuestionCount() - 1);
            questionGroupMapper.updateById(group);
        }
    }

    private void handleCreate(QuestionSaveItem item, Long groupId, Long teacherId) {
        Question question = new Question();
        question.setTeacherId(teacherId);
        question.setGroupId(groupId);
        question.setType(item.getType());
        question.setContent(item.getContent());
        question.setAnalysis(item.getAnalysis());
        question.setDifficulty(item.getDifficulty());
        question.setScore(item.getScore());

        if (isChoiceType(item.getType()) && item.getOptions() != null && !item.getOptions().isEmpty()) {
            question.setAnswer(generateAnswer(item.getOptions()));
        } else if (!isProgrammingType(item.getType())) {
            question.setAnswer(item.getAnswer());
        }

        questionMapper.insert(question);

        if (isChoiceType(item.getType()) && item.getOptions() != null && !item.getOptions().isEmpty()) {
            insertOptions(question.getId(), item.getOptions());
        }

        if (isProgrammingType(item.getType())) {
            insertProgrammingConfig(question.getId(), item);
            insertTestCases(question.getId(), item.getTestCases());
        }

        QuestionGroup group = questionGroupMapper.selectById(groupId);
        if (group != null) {
            group.setQuestionCount(group.getQuestionCount() == null ? 1 : group.getQuestionCount() + 1);
            questionGroupMapper.updateById(group);
        }
    }

    private void handleUpdate(QuestionSaveItem item) {
        Question question = questionMapper.selectById(item.getId());
        if (question == null) {
            return;
        }

        question.setType(item.getType());
        question.setContent(item.getContent());
        question.setAnalysis(item.getAnalysis());
        question.setDifficulty(item.getDifficulty());
        question.setScore(item.getScore());

        if (isChoiceType(item.getType()) && item.getOptions() != null && !item.getOptions().isEmpty()) {
            question.setAnswer(generateAnswer(item.getOptions()));
        } else if (!isProgrammingType(item.getType())) {
            question.setAnswer(item.getAnswer());
        }

        questionMapper.updateById(question);

        // 全量替换选项：删除旧选项，插入新选项
        questionOptionMapper.delete(
                new LambdaQueryWrapper<QuestionOption>()
                        .eq(QuestionOption::getQuestionId, question.getId()));

        if (isChoiceType(item.getType()) && item.getOptions() != null && !item.getOptions().isEmpty()) {
            insertOptions(question.getId(), item.getOptions());
        }

        // 全量替换编程题扩展数据
        if (isProgrammingType(item.getType())) {
            questionProgrammingMapper.delete(
                    new LambdaQueryWrapper<QuestionProgramming>()
                            .eq(QuestionProgramming::getQuestionId, question.getId()));
            questionTestCaseMapper.delete(
                    new LambdaQueryWrapper<QuestionTestCase>()
                            .eq(QuestionTestCase::getQuestionId, question.getId()));
            insertProgrammingConfig(question.getId(), item);
            insertTestCases(question.getId(), item.getTestCases());
        } else {
            // 从编程题改为其他类型，清理扩展数据
            questionProgrammingMapper.delete(
                    new LambdaQueryWrapper<QuestionProgramming>()
                            .eq(QuestionProgramming::getQuestionId, question.getId()));
            questionTestCaseMapper.delete(
                    new LambdaQueryWrapper<QuestionTestCase>()
                            .eq(QuestionTestCase::getQuestionId, question.getId()));
        }
    }

    private void insertOptions(Long questionId, List<QuestionOptionSaveItem> optionItems) {
        for (QuestionOptionSaveItem optItem : optionItems) {
            QuestionOption option = new QuestionOption();
            option.setQuestionId(questionId);
            option.setLabel(optItem.getLabel());
            option.setContent(optItem.getContent());
            option.setIsCorrect(Boolean.TRUE.equals(optItem.getIsCorrect()) ? 1 : 0);
            option.setSortOrder(optItem.getSortOrder());

            questionOptionMapper.insert(option);
        }
    }

    /**
     * 根据选项自动生成答案文本
     */
    private String generateAnswer(List<QuestionOptionSaveItem> options) {
        return options.stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                .map(QuestionOptionSaveItem::getLabel)
                .collect(Collectors.joining(","));
    }

    private boolean isChoiceType(Integer type) {
        return type != null && (type == 1 || type == 2);
    }

    private boolean isProgrammingType(Integer type) {
        return type != null && type == 5;
    }

    /**
     * 构建题目详情 VO（含编程题扩展数据）
     */
    private QuestionDetailVO buildDetailVO(Question question, List<QuestionOption> options,
                                            QuestionProgramming programming, List<QuestionTestCase> testCases) {
        List<QuestionOptionVO> optionVOs = options.stream()
                .map(o -> QuestionOptionVO.builder()
                        .id(o.getId())
                        .label(o.getLabel())
                        .content(o.getContent())
                        .isCorrect(Objects.equals(o.getIsCorrect(), 1))
                        .sortOrder(o.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        QuestionProgrammingVO programmingVO = null;
        List<QuestionTestCaseVO> testCaseVOs = null;
        if (isProgrammingType(question.getType())) {
            if (programming != null) {
                programmingVO = QuestionProgrammingVO.builder()
                        .templateCode(programming.getTemplateCode())
                        .judge0LanguageId(programming.getJudge0LanguageId())
                        .timeLimit(programming.getTimeLimit())
                        .memoryLimit(programming.getMemoryLimit())
                        .build();
            }

            testCaseVOs = testCases.stream()
                    .map(tc -> QuestionTestCaseVO.builder()
                            .id(tc.getId())
                            .input(tc.getInput())
                            .expectedOutput(tc.getExpectedOutput())
                            .isSample(Objects.equals(tc.getIsSample(), 1))
                            .sortOrder(tc.getSortOrder())
                            .build())
                    .collect(Collectors.toList());
        }

        return QuestionDetailVO.builder()
                .id(question.getId())
                .type(question.getType())
                .content(question.getContent())
                .answer(question.getAnswer())
                .analysis(question.getAnalysis())
                .options(optionVOs)
                .programmingConfig(programmingVO)
                .testCases(testCaseVOs)
                .createTime(question.getCreateTime())
                .updateTime(question.getUpdateTime())
                .build();
    }

    private void insertProgrammingConfig(Long questionId, QuestionSaveItem item) {
        if (item.getProgrammingConfig() == null) {
            return;
        }
        QuestionProgramming programming = new QuestionProgramming();
        programming.setQuestionId(questionId);
        programming.setTemplateCode(item.getProgrammingConfig().getTemplateCode());
        programming.setJudge0LanguageId(item.getProgrammingConfig().getJudge0LanguageId());
        programming.setTimeLimit(item.getProgrammingConfig().getTimeLimit() != null
                ? item.getProgrammingConfig().getTimeLimit() : 3000);
        programming.setMemoryLimit(item.getProgrammingConfig().getMemoryLimit() != null
                ? item.getProgrammingConfig().getMemoryLimit() : 256000);
        questionProgrammingMapper.insert(programming);
    }

    private void insertTestCases(Long questionId, List<TestCaseDTO> testCases) {
        if (testCases == null || testCases.isEmpty()) {
            return;
        }
        for (int i = 0; i < testCases.size(); i++) {
            TestCaseDTO dto = testCases.get(i);
            QuestionTestCase tc = new QuestionTestCase();
            tc.setQuestionId(questionId);
            tc.setInput(dto.getInput());
            tc.setExpectedOutput(dto.getExpectedOutput());
            tc.setIsSample(Boolean.TRUE.equals(dto.getIsSample()) ? 1 : 0);
            tc.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : i + 1);
            questionTestCaseMapper.insert(tc);
        }
    }

    /**
     * 截取题干预览：去 HTML 标签后取前 N 字
     */
    private String getPreview(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        String plainText = content.replaceAll("<[^>]+>", "").trim();
        if (plainText.length() <= PREVIEW_LENGTH) {
            return plainText;
        }
        return plainText.substring(0, PREVIEW_LENGTH) + "...";
    }

    /**
     * 获取题目类型名称
     */
    private String getTypeName(Integer type) {
        if (type == null || type < 1 || type > TYPE_NAMES.length - 1) {
            return "未知";
        }
        return TYPE_NAMES[type];
    }
}
