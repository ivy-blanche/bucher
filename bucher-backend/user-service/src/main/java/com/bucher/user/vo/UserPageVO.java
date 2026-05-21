package com.bucher.user.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserPageVO {

    private Long id;
    private String userNo;
    private String realName;
    private String email;
    private Integer role;
    private String roleName;
    private String deptName;
    private String adminClassName;
    private Integer status;
    private Integer pwdReset;
    private LocalDateTime createTime;
}
