package org.briarheart.orchestra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Roman Chigvintsev
 */
@Configuration
@ConfigurationProperties("info.application")
@Getter
@Setter
public class ApplicationInfoProperties {
    private String name;
    private String version;
}
