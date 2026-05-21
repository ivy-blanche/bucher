package com.bucher.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCodeEnum {

    SUCCESS(200, "成功"),
    FAIL(500, "失败"),

    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),

    // 服务端错误 5xx
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // 业务错误 1xxx - 用户模块
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "密码错误"),
    USER_DISABLED(1003, "用户已被禁用"),
    USER_ALREADY_EXISTS(1004, "用户已存在"),
    EMAIL_ALREADY_EXISTS(1005, "邮箱已被注册"),
    EMAIL_NOT_REGISTERED(1006, "该邮箱未注册"),
    VERIFICATION_CODE_ERROR(1007, "验证码错误或已过期"),
    VERIFICATION_CODE_TOO_FREQUENT(1008, "验证码发送过于频繁"),
    DEPARTMENT_NOT_FOUND(1010, "院系不存在"),
    ADMIN_CLASS_NOT_FOUND(1011, "行政班级不存在"),
    ADMIN_CLASS_NOT_IN_DEPT(1012, "行政班级不属于所选院系"),
    ADMIN_CLASS_HAS_STUDENTS(1013, "行政班级下存在学生，无法删除"),
    EXCEL_IMPORT_ERROR(1014, "Excel导入失败"),
    USER_NO_ALREADY_EXISTS(1015, "工号/学号已存在"),
    UPLOAD_FILE_EMPTY(1016, "上传文件不能为空"),
    FORBIDDEN_OPERATION(1017, "无权限执行此操作"),

    // 课程相关 2xxx
    COURSE_NOT_FOUND(2001, "课程不存在"),
    COURSE_CLASS_NOT_FOUND(2002, "课程班级不存在"),
    COURSE_CLASS_FORBIDDEN(2003, "无权操作该班级"),
    INVITE_CODE_INVALID(2004, "邀请码无效"),
    INVITE_CODE_EXPIRED(2005, "邀请码已过期"),
    ALREADY_IN_CLASS(2006, "已加入该班级"),

    COURSE_CODE_INVALID(2007, "课程码无效"),
    ALREADY_ENROLLED(2008, "已选该课程"),
    COURSE_CLOSED(2009, "课程已结课"),

    // 考试相关 3xxx
    EXAM_NOT_FOUND(3001, "考试不存在"),
    EXAM_NOT_STARTED(3002, "考试未开始"),
    EXAM_FINISHED(3003, "考试已结束"),

    // 课程资料 21xx
    MATERIAL_NOT_FOUND(2101, "资料不存在"),
    MATERIAL_FORBIDDEN(2102, "无权操作该资料"),
    MATERIAL_FILE_TOO_LARGE(2103, "文件大小超出限制"),
    MATERIAL_FILE_TYPE_INVALID(2104, "不支持的文件类型"),
    MATERIAL_UPLOAD_FAILED(2105, "文件上传失败"),

    // 作业相关 4xxx
    HOMEWORK_NOT_FOUND(4001, "作业不存在"),
    HOMEWORK_EXPIRED(4002, "作业已截止");

    private final Integer code;
    private final String message;
}
