package com.bucher.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bucher.user.entity.Department;
import org.apache.ibatis.annotations.Mapper;

/**
 * 院系/部门 Mapper
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {

}
