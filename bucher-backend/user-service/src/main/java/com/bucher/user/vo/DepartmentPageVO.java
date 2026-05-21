package com.bucher.user.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DepartmentPageVO {

    private Long id;
    private String name;
    private String deptCode;
    private Integer type;
    private String description;
    private LocalDateTime createTime;
    private Long classCount;
    private Long studentCount;
    private Long teacherCount;
}
