package kr.co.pennyway.socket.config;

import kr.co.pennyway.domain.common.importer.EnablePennywayDomainConfig;
import kr.co.pennyway.domain.common.importer.PennywayDomainConfigGroup;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnablePennywayDomainConfig(value = {
        PennywayDomainConfigGroup.REDIS,
        PennywayDomainConfigGroup.REDISSON
})
public class DomainConfig {
}