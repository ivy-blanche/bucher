package com.bucher.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminClassCreateDTO {

    @NotBlank(message = "班级名称不能为空")
    private String name;

    @NotNull(message = "所属院系ID不能为空")
    private Long deptId;

    @NotNull(message = "入学年份不能为空")
    private Integer year;
}
