package com.bucher.user.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DepartmentVO {

    private Long id;
    private String name;
    private String deptCode;
    private Integer type;
    private String description;
    private LocalDateTime createTime;
}
