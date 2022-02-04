package org.briarheart.tictactask;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = MailSenderAutoConfiguration.class)
@ActiveProfiles("test")
public class TicTacTaskApplicationTests {
	@Test
	public void contextLoads() {
	}
}
