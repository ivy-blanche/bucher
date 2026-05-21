package com.bucher.homework.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 代码运行结果 VO（运行仅针对样例测试用例，不保存）
 */
@Data
@Builder
@Schema(description = "代码运行结果")
public class CodeRunResultVO {

    @Schema(description = "是否编译成功")
    private Boolean compileSuccess;

    @Schema(description = "编译输出（编译失败时有值）")
    private String compileOutput;

    @Schema(description = "通过的样例数")
    private Integer passedCount;

    @Schema(description = "总样例数")
    private Integer totalCount;

    @Schema(description = "各测试用例结果")
    private List<RunTestResult> testCaseResults;

    @Data
    @Builder
    @Schema(description = "单测运行结果")
    public static class RunTestResult {

        @Schema(description = "是否通过")
        private Boolean passed;

        @Schema(description = "程序输出")
        private String stdout;

        @Schema(description = "期望输出")
        private String expectedOutput;

        @Schema(description = "运行时间（秒）")
        private String time;

        @Schema(description = "运行内存（KB）")
        private Long memory;
    }
}
