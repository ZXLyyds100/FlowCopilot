package com.kama.jchatmind.agent.tools;

import com.kama.jchatmind.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailTools implements Tool {

    private final EmailService emailService;

    public EmailTools(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public String getName() {
        return "emailTool";
    }

    @Override
    public String getDescription() {
        return "用于发送邮件。";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    @dev.langchain4j.agent.tool.Tool(
            name = "sendEmail",
            value = "发送邮件到指定收件人，参数包含 to、subject、content。邮件采用异步方式发送。"
    )
    public String sendEmail(String to, String subject, String content) {
        if (to == null || to.trim().isEmpty()) {
            return "错误：收件人邮箱地址不能为空";
        }
        if (subject == null || subject.trim().isEmpty()) {
            return "错误：邮件主题不能为空";
        }
        if (content == null || content.trim().isEmpty()) {
            return "错误：邮件内容不能为空";
        }
        if (!to.contains("@")) {
            return "错误：收件人邮箱地址格式不正确";
        }

        emailService.sendEmailAsync(to.trim(), subject.trim(), content.trim());
        log.info("邮件已提交异步发送，收件人: {}, 主题: {}", to, subject);
        return String.format("邮件已提交发送！\n收件人: %s\n主题: %s\n邮件正在后台异步发送中...", to, subject);
    }
}
