package com.novastack.sms.config;

import com.novastack.sms.service.AuthService;
import com.novastack.sms.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final AuthService authService;
    private final WalletService walletService;

    @Override
    public void run(ApplicationArguments args) {
        authService.ensurePlatformSender();
        authService.backfillMpesaAccountRefs();
        int walletsCreated = walletService.backfillMissingWallets();
        authService.ensureSuperAdmin();
        log.info(
                "Platform sender ID, M-Pesa account refs, wallets (created {}), and SUPER_ADMIN ensured",
                walletsCreated);
    }
}
