package aliang.flowcopilot.service.impl;

import aliang.flowcopilot.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 邮件服务实现。
 * <p>
 * 基于 Spring Mail 发送普通文本邮件，主要给 Agent 的邮件工具提供支撑。
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    /**
     * 构造邮件服务实现。
     *
     * @param mailSender Spring Mail 发件器
     */
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    @Async
    /**
     * 异步发送邮件。
     *
     * @param to 收件人
     * @param subject 主题
     * @param content 内容
     */
    public void sendEmailAsync(String to, String subject, String content) {
        try {
            // 创建邮件消息
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            message.setFrom(from);

            // 发送邮件
            mailSender.send(message);

            log.info("异步发送邮件成功，收件人: {}, 主题: {}", to, subject);
        } catch (Exception e) {
            log.error("异步发送邮件失败，收件人: {}, 主题: {}, 错误: {}", to, subject, e.getMessage(), e);
        }
    }
}
