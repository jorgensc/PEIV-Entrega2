package jcondo.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// o servidor do Azure roda em UTC, entao "hoje" e "agora" sempre pelo fuso do condominio
@Configuration
public class RelogioConfig {

    @Bean
    public Clock relogio(@Value("${jcondo.fuso:America/Sao_Paulo}") String fuso) {
        return Clock.system(ZoneId.of(fuso));
    }
}
