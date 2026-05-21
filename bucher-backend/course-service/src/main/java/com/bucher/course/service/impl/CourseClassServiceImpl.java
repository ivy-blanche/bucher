package com.bucher.course.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bucher.common.enums.ResultCodeEnum;
import com.bucher.common.exception.BusinessException;
import com.bucher.course.dto.CourseClassCreateDTO;
import com.bucher.course.dto.JoinByInviteCodeDTO;
import com.bucher.course.entity.Course;
import com.bucher.course.entity.CourseClass;
import com.bucher.course.entity.CourseClassMember;
import com.bucher.course.entity.CourseEnrollment;
import com.bucher.course.enums.JoinTypeEnum;
import com.bucher.course.mapper.CourseClassMapper;
import com.bucher.course.mapper.CourseClassMemberMapper;
import com.bucher.course.mapper.CourseEnrollmentMapper;
import com.bucher.course.mapper.CourseMapper;
import com.bucher.course.service.CourseClassService;
import com.bucher.course.vo.CourseClassMemberVO;
import com.bucher.course.vo.CourseClassVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 课程班级服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseClassServiceImpl implements CourseClassService {

    private final CourseMapper courseMapper;
    private final CourseClassMapper courseClassMapper;
    private final CourseClassMemberMapper memberMapper;
    private final CourseEnrollmentMapper enrollmentMapper;
    private final RedissonClient redissonClient;

    private static final String INVITE_CODE_KEY_PREFIX = "course:invite:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseClassVO createClass(CourseClassCreateDTO dto, Long teacherId) {
        Course course = courseMapper.selectById(dto.getCourseId());
        if (course == null) {
            throw new BusinessException(ResultCodeEnum.COURSE_NOT_FOUND);
        }
        if (!course.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCodeEnum.COURSE_CLASS_FORBIDDEN);
        }

        CourseClass courseClass = new CourseClass();
        courseClass.setCourseId(dto.getCourseId());
        courseClass.setName(dto.getName());
        courseClass.setTeacherId(teacherId);
        courseClass.setStatus(1);

        String inviteCode = generateUniqueInviteCode();
        courseClass.setInviteCode(inviteCode);
        courseClassMapper.insert(courseClass);

        cacheInviteCode(inviteCode, courseClass.getId());

        log.info("教师创建课程班级: teacherId={}, classId={}, name={}", teacherId, courseClass.getId(), courseClass.getName());
        return toVO(courseClass, course.getName(), 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateClassName(Long classId, String name, Long teacherId) {
        CourseClass courseClass = getClassAndCheckOwner(classId, teacherId);
        courseClass.setName(name);
        courseClassMapper.updateById(courseClass);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClass(Long classId, Long teacherId) {
        CourseClass courseClass = getClassAndCheckOwner(classId, teacherId);

        clearInviteCodeCache(courseClass.getInviteCode());
        courseClassMapper.deleteById(classId);

        memberMapper.delete(new LambdaQueryWrapper<CourseClassMember>()
                .eq(CourseClassMember::getCourseClassId, classId));

        log.info("教师删除课程班级: teacherId={}, classId={}", teacherId, classId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String regenerateInviteCode(Long classId, Long teacherId) {
        CourseClass courseClass = getClassAndCheckOwner(classId, teacherId);

        clearInviteCodeCache(courseClass.getInviteCode());

        String newCode = generateUniqueInviteCode();
        courseClass.setInviteCode(newCode);
        courseClass.setInviteExpireTime(null);
        courseClassMapper.updateById(courseClass);

        cacheInviteCode(newCode, classId);

        log.info("教师刷新邀请码: teacherId={}, classId={}, newCode={}", teacherId, classId, newCode);
        return newCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinByInviteCode(JoinByInviteCodeDTO dto, Long studentId) {
        String inviteCode = dto.getInviteCode().trim().toUpperCase();
        Long classId = getClassIdByInviteCode(inviteCode);

        if (classId == null) {
            throw new BusinessException(ResultCodeEnum.INVITE_CODE_INVALID);
        }

        CourseClass courseClass = courseClassMapper.selectById(classId);
        if (courseClass == null) {
            throw new BusinessException(ResultCodeEnum.INVITE_CODE_INVALID);
        }

        if (courseClass.getInviteExpireTime() != null
                && courseClass.getInviteExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCodeEnum.INVITE_CODE_EXPIRED);
        }

        // 检查是否已加入
        Long count = memberMapper.selectCount(new LambdaQueryWrapper<CourseClassMember>()
                .eq(CourseClassMember::getCourseClassId, classId)
                .eq(CourseClassMember::getStudentId, studentId)
                .eq(CourseClassMember::getStatus, 1));
        if (count > 0) {
            throw new BusinessException(ResultCodeEnum.ALREADY_IN_CLASS);
        }

        CourseClassMember member = new CourseClassMember();
        member.setCourseClassId(classId);
        member.setStudentId(studentId);
        member.setJoinType(JoinTypeEnum.INVITE_CODE.getCode());
        member.setJoinTime(LocalDateTime.now());
        member.setStatus(1);
        memberMapper.insert(member);

        // 通过班级码加入班级时，自动选课到该课程
        Long courseId = courseClass.getCourseId();
        Long enrolled = enrollmentMapper.selectCount(new LambdaQueryWrapper<CourseEnrollment>()
                .eq(CourseEnrollment::getCourseId, courseId)
                .eq(CourseEnrollment::getStudentId, studentId)
                .eq(CourseEnrollment::getStatus, 1));
        if (enrolled == 0) {
            CourseEnrollment enrollment = new CourseEnrollment();
            enrollment.setCourseId(courseId);
            enrollment.setStudentId(studentId);
            enrollment.setStatus(1);
            enrollmentMapper.insert(enrollment);
            log.info("学生通过班级码自动选课: studentId={}, courseId={}", studentId, courseId);
        }

        log.info("学生通过邀请码加入班级: studentId={}, classId={}, inviteCode={}", studentId, classId, inviteCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addStudent(Long classId, Long studentId, Long teacherId) {
        CourseClass courseClass = getClassAndCheckOwner(classId, teacherId);

        Long count = memberMapper.selectCount(new LambdaQueryWrapper<CourseClassMember>()
                .eq(CourseClassMember::getCourseClassId, classId)
                .eq(CourseClassMember::getStudentId, studentId)
                .eq(CourseClassMember::getStatus, 1));
        if (count > 0) {
            throw new BusinessException(ResultCodeEnum.ALREADY_IN_CLASS);
        }

        CourseClassMember member = new CourseClassMember();
        member.setCourseClassId(classId);
        member.setStudentId(studentId);
        member.setJoinType(JoinTypeEnum.TEACHER_ADD.getCode());
        member.setJoinTime(LocalDateTime.now());
        member.setStatus(1);
        memberMapper.insert(member);

        log.info("教师添加学生到班级: teacherId={}, classId={}, studentId={}", teacherId, classId, studentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeStudent(Long memberId, Long teacherId) {
        CourseClassMember member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException(ResultCodeEnum.COURSE_CLASS_NOT_FOUND);
        }

        CourseClass courseClass = courseClassMapper.selectById(member.getCourseClassId());
        if (courseClass == null || !courseClass.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCodeEnum.COURSE_CLASS_FORBIDDEN);
        }

        member.setStatus(0);
        memberMapper.updateById(member);

        log.info("教师移除班级学生: teacherId={}, classId={}, studentId={}", teacherId, member.getCourseClassId(), member.getStudentId());
    }

    @Override
    public List<CourseClassMemberVO> listMembers(Long classId) {
        List<CourseClassMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<CourseClassMember>()
                        .eq(CourseClassMember::getCourseClassId, classId)
                        .eq(CourseClassMember::getStatus, 1)
                        .orderByAsc(CourseClassMember::getJoinTime)
        );

        return members.stream()
                .map(m -> CourseClassMemberVO.builder()
                        .id(m.getId())
                        .studentId(m.getStudentId())
                        .joinType(m.getJoinType())
                        .joinTime(m.getJoinTime())
                        .status(m.getStatus())
                        .build())
                .toList();
    }

    @Override
    public List<CourseClassVO> listCourseClasses(Long courseId) {
        List<CourseClass> classes = courseClassMapper.selectList(
                new LambdaQueryWrapper<CourseClass>()
                        .eq(CourseClass::getCourseId, courseId)
                        .orderByAsc(CourseClass::getCreateTime)
        );

        if (classes.isEmpty()) {
            return List.of();
        }

        Course course = courseMapper.selectById(courseId);

        List<Long> classIds = classes.stream().map(CourseClass::getId).toList();
        List<CourseClassMember> allMembers = memberMapper.selectList(
                new LambdaQueryWrapper<CourseClassMember>()
                        .in(CourseClassMember::getCourseClassId, classIds)
                        .eq(CourseClassMember::getStatus, 1)
        );
        Map<Long, Long> countMap = allMembers.stream()
                .collect(Collectors.groupingBy(CourseClassMember::getCourseClassId, Collectors.counting()));

        return classes.stream()
                .map(c -> toVO(c, course != null ? course.getName() : null,
                        countMap.getOrDefault(c.getId(), 0L).intValue()))
                .toList();
    }

    private String generateUniqueInviteCode() {
        for (int i = 0; i < 10; i++) {
            String code = RandomUtil.randomString(8).toUpperCase();
            if (!isInviteCodeUsed(code)) {
                return code;
            }
        }
        throw new BusinessException("邀请码生成失败，请重试");
    }

    private boolean isInviteCodeUsed(String code) {
        return courseClassMapper.selectCount(
                new LambdaQueryWrapper<CourseClass>().eq(CourseClass::getInviteCode, code)
        ) > 0;
    }

    private void cacheInviteCode(String code, Long classId) {
        RBucket<Long> bucket = redissonClient.getBucket(INVITE_CODE_KEY_PREFIX + code);
        bucket.set(classId, 24, TimeUnit.HOURS);
    }

    private Long getClassIdByInviteCode(String code) {
        RBucket<Long> bucket = redissonClient.getBucket(INVITE_CODE_KEY_PREFIX + code);
        Long classId = bucket.get();
        if (classId != null) {
            return classId;
        }

        CourseClass courseClass = courseClassMapper.selectOne(
                new LambdaQueryWrapper<CourseClass>().eq(CourseClass::getInviteCode, code)
        );
        if (courseClass != null) {
            cacheInviteCode(code, courseClass.getId());
            return courseClass.getId();
        }
        return null;
    }

    private void clearInviteCodeCache(String code) {
        if (code != null) {
            redissonClient.getBucket(INVITE_CODE_KEY_PREFIX + code).delete();
        }
    }

    private CourseClass getClassAndCheckOwner(Long classId, Long teacherId) {
        CourseClass courseClass = courseClassMapper.selectById(classId);
        if (courseClass == null) {
            throw new BusinessException(ResultCodeEnum.COURSE_CLASS_NOT_FOUND);
        }
        if (!courseClass.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCodeEnum.COURSE_CLASS_FORBIDDEN);
        }
        return courseClass;
    }

    private CourseClassVO toVO(CourseClass courseClass, String courseName, int studentCount) {
        return CourseClassVO.builder()
                .id(courseClass.getId())
                .courseId(courseClass.getCourseId())
                .courseName(courseName)
                .name(courseClass.getName())
                .inviteCode(courseClass.getInviteCode())
                .inviteExpireTime(courseClass.getInviteExpireTime())
                .studentCount(studentCount)
                .status(courseClass.getStatus())
                .createTime(courseClass.getCreateTime())
                .build();
    }
}
