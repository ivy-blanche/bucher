package com.bucher.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DepartmentCreateDTO {

    @NotBlank(message = "院系名称不能为空")
    private String name;

    @NotBlank(message = "院系编号不能为空")
    private String deptCode;

    @NotNull(message = "类型不能为空")
    private Integer type;

    private String description;
}
