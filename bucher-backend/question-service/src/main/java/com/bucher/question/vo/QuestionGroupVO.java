package com.bucher.question.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 题库分组列表-轻量视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionGroupVO {

    private Long id;

    private String name;

    private Integer questionCount;

    private LocalDateTime createTime;
}
