package org.briarheart.tictactask;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;

@SpringBootApplication(exclude = DataSourceTransactionManagerAutoConfiguration.class)
public class TicTacTaskApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicTacTaskApplication.class, args);
    }
}
