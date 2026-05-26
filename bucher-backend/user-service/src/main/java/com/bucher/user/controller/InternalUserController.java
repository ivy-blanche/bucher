package com.bucher.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bucher.common.result.Result;
import com.bucher.user.entity.User;
import com.bucher.user.enums.UserRoleEnum;
import com.bucher.user.mapper.UserMapper;
import com.bucher.common.vo.TeacherSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 内部服务间调用接口（不经过网关鉴权）
 */
@Tag(name = "内部服务调用")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserMapper userMapper;

    @Operation(summary = "根据ID获取用户真实姓名（内部Feign调用）")
    @GetMapping("/users/{id}/name")
    public Result<String> getUserNameById(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.success(null);
        }
        return Result.success(user.getRealName());
    }

    @Operation(summary = "根据工号搜索教师（内部Feign调用）")
    @GetMapping("/teachers/search")
    public Result<List<TeacherSearchVO>> searchTeachersByUserNo(String keyword) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, UserRoleEnum.TEACHER.getCode())
                .like(User::getUserNo, keyword)
                .eq(User::getIsDeleted, 0)
                .orderByAsc(User::getUserNo));

        List<TeacherSearchVO> voList = users.stream().map(u -> {
            TeacherSearchVO vo = new TeacherSearchVO();
            vo.setId(u.getId());
            vo.setUserNo(u.getUserNo());
            vo.setRealName(u.getRealName());
            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList);
    }
}
