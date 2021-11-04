package io.camunda.example.worker;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Map;

@Slf4j
@Component
public class SendEmailWorker implements JobHandler {

  public static final String EMAIL_BODY_VAR = "mailBody";
  public static final String EMAIL_SUBJECT_VAR = "mailSubject";
  public static final String EMAIL_FROM = "my.worker@camunda.cloud";
  public static final String EMAIL_TO = "robert.emsbach@camunda.com";

  public JavaMailSender mailSender;

  public SendEmailWorker(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @Override
  @ZeebeWorker(type = "emailService")
  public void handle(JobClient client, ActivatedJob job) {

    try {
      // read process data
      final Map<String, Object> variables = job.getVariablesAsMap();
      String emailBody = (String) variables.get(EMAIL_BODY_VAR);
      if (null==emailBody) throw new RuntimeException("Missing process data: " + EMAIL_BODY_VAR);
      String emailSubject = (String) variables.get(EMAIL_SUBJECT_VAR);
      if (null==emailSubject) throw new RuntimeException("Missing process data: " + EMAIL_SUBJECT_VAR);

      // send email
      MimeMessage message =mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "utf-8");
      helper.setTo(EMAIL_TO);
      helper.setSubject(emailSubject + " (" + job.getKey()+")");
      helper.setFrom(EMAIL_FROM);
      message.setContent(emailBody, "text/html");
      mailSender.send(message);

      // complete the external task
      client.newCompleteCommand(job.getKey())
          .variables(Map.of("result", "done"))
          .send();
        //.whenComplete((result, exception) -> {log.info("Job completion result: {} : {}", result, exception);});
      log.info("Task {} with instance id {} for process instance {} completed.",
          job.getElementInstanceKey(), job.getKey(), job.getProcessInstanceKey());

    } catch (MailException | MessagingException mailEx) {
      log.error(mailEx.getMessage(), mailEx);
      // create incident, no retries
      client.newFailCommand(job.getKey())
          .retries(0)
          .errorMessage(mailEx.getMessage())
          .send();
    }
  }
}