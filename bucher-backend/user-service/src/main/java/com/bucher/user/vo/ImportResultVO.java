package com.bucher.user.vo;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ImportResultVO {

    private int totalCount;
    private int successCount;
    private int failCount;
    private List<ImportErrorVO> errors;
}
