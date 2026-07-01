package io.synub.sso.config;

import io.synub.sso.domain.Account;
import io.synub.sso.repo.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 데모 체험 계정 시더. external_id 를 빌링 시드 고객(demo-user)에 맞춰 생성하므로,
 * 이 계정으로 로그인하면 빌링의 리치 시드 데이터가 그대로 보인다. 운영은 sso.demo.enabled=false.
 */
@Component
public class DemoSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoSeeder.class);

    private final SsoProperties props;
    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public DemoSeeder(SsoProperties props, AccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.props = props;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        SsoProperties.Demo demo = props.demo();
        if (demo == null || !demo.enabled()) return;
        if (accounts.existsByEmail(demo.email())) return;

        accounts.save(new Account(
                demo.externalId(),
                demo.email(),
                passwordEncoder.encode(demo.password()),
                demo.name()));
        log.info("데모 계정 생성: {} (external_id={})", demo.email(), demo.externalId());
    }
}
