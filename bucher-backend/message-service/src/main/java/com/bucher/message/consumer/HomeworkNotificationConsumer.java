package com.bucher.message.consumer;

import com.bucher.common.result.Result;
import com.bucher.message.config.RabbitMQConfig;
import com.bucher.message.entity.Notification;
import com.bucher.message.enums.NotificationTypeEnum;
import com.bucher.message.feign.CourseFeignClient;
import com.bucher.message.feign.UserFeignClient;
import com.bucher.message.mapper.NotificationMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 作业通知消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HomeworkNotificationConsumer {

    private final UserFeignClient userFeignClient;
    private final CourseFeignClient courseFeignClient;
    private final NotificationMapper notificationMapper;

    @RabbitListener(queues = RabbitMQConfig.HOMEWORK_QUEUE)
    public void handleHomeworkPublished(HomeworkPublishedEvent event, Channel channel, Message message) {
        try {
            log.info("接收作业发布事件: homeworkId={}, title={}", event.getHomeworkId(), event.getTitle());

            String teacherName = resolveTeacherName(event.getTeacherId());
            if (teacherName == null) {
                log.warn("教师ID={} 不存在，跳过通知", event.getTeacherId());
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            String courseName = resolveCourseName(event.getCourseId());
            if (courseName == null) {
                log.warn("课程ID={} 不存在，跳过通知", event.getCourseId());
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            List<Long> studentIds = resolveStudentIds(event.getClassIds());
            if (studentIds.isEmpty()) {
                log.warn("班级 {} 中没有学生，跳过通知", event.getClassIds());
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            List<Notification> notifications = studentIds.stream().map(studentId -> {
                Notification n = new Notification();
                n.setUserId(studentId);
                n.setType(NotificationTypeEnum.HOMEWORK.getCode());
                n.setRelatedId(event.getHomeworkId());
                n.setTeacherName(teacherName);
                n.setCourseName(courseName);
                n.setBizTitle(event.getTitle());
                n.setDeadline(event.getDeadline());
                n.setIsRead(false);
                return n;
            }).collect(Collectors.toList());

            for (Notification n : notifications) {
                notificationMapper.insert(n);
            }

            log.info("作业通知创建完成: homeworkId={}, 通知学生数={}", event.getHomeworkId(), notifications.size());
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception e) {
            log.error("处理作业发布事件异常", e);
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            } catch (IOException ex) {
                log.error("发送 basicNack 失败", ex);
            }
        }
    }

    private String resolveTeacherName(Long teacherId) {
        try {
            Result<String> result = userFeignClient.getUserNameById(teacherId);
            return result != null ? result.getData() : null;
        } catch (Exception e) {
            log.error("调用 user-service 获取教师姓名失败, teacherId={}", teacherId, e);
            return null;
        }
    }

    private String resolveCourseName(Long courseId) {
        try {
            Result<String> result = courseFeignClient.getCourseNameById(courseId);
            return result != null ? result.getData() : null;
        } catch (Exception e) {
            log.error("调用 course-service 获取课程名称失败, courseId={}", courseId, e);
            return null;
        }
    }

    private List<Long> resolveStudentIds(List<Long> classIds) {
        try {
            Result<List<Long>> result = courseFeignClient.getClassMemberIds(classIds);
            return result != null && result.getData() != null ? result.getData() : List.of();
        } catch (Exception e) {
            log.error("调用 course-service 获取学生列表失败, classIds={}", classIds, e);
            return List.of();
        }
    }
}
