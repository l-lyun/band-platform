package band.platform.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration(proxyBeanMethods = false)
public class JavaMailSenderConfig {

	@Bean
	public JavaMailSender javaMailSender(
		@Value("${spring.mail.host:localhost}") String host,
		@Value("${spring.mail.port:2525}") int port
	) {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost(host);
		mailSender.setPort(port);
		return mailSender;
	}
}
