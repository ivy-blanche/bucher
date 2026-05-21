package com.bucher.user.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class StudentImportDTO {

    @ExcelProperty("学号")
    private String userNo;

    @ExcelProperty("姓名")
    private String realName;

    @ExcelProperty("手机号")
    private String phone;
}
