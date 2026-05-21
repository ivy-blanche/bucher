package com.bucher.user.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminClassVO {

    private Long id;
    private String name;
    private Long deptId;
    private String deptName;
    private Integer year;
    private LocalDateTime createTime;
}
