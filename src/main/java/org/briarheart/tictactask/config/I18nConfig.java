package org.briarheart.tictactask.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.MessageSourceAccessor;

import java.util.Locale;

/**
 * @author Roman Chigvintsev
 */
@Configuration
public class I18nConfig {
    @Bean
    public MessageSourceAccessor messages(MessageSource messageSource) {
        return new MessageSourceAccessor(messageSource, Locale.ENGLISH);
    }
}
