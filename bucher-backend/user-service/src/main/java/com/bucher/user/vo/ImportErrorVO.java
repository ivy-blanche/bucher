package com.bucher.user.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportErrorVO {

    private int row;
    private String userNo;
    private String reason;
}
