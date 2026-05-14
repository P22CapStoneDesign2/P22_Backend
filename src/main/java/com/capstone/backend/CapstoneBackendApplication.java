package com.capstone.backend;

import domain.auth.entity.User;
import org.springframework.boot.SpringApplication;
import global.config.EmailVerificationProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring Boot 진입점.
 * JPA 엔티티·리포지토리는 최상위 패키지(domain)에 두었으므로 스캔 범위를 명시한다.
 */
@SpringBootApplication(scanBasePackages = {"com.capstone.backend", "domain", "global"})
@ConfigurationPropertiesScan(basePackageClasses = EmailVerificationProperties.class)
@EntityScan(basePackageClasses = User.class)
@EnableJpaRepositories(basePackages = "domain.auth.repository")
public class CapstoneBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CapstoneBackendApplication.class, args);
    }
}
