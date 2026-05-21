package com.bucher.user.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class TeacherImportDTO {

    @ExcelProperty("工号")
    private String userNo;

    @ExcelProperty("姓名")
    private String realName;
}
