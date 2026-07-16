package com.novastack.sms.security;

import com.novastack.sms.domain.repository.UserRepository;
import com.novastack.sms.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserPrincipal loadUserByUsername(String username) throws UsernameNotFoundException {
        String raw = username == null ? "" : username.trim();
        if (raw.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }

        return userRepository.findByEmailWithOrganization(raw)
                .or(() -> {
                    if (PhoneNormalizer.looksLikePhone(raw)) {
                        String normalized = PhoneNormalizer.normalize(raw);
                        return userRepository.findOrgAdminByOrganizationPhone(normalized)
                                .or(() -> userRepository.findOrgAdminByOrganizationPhone(raw));
                    }
                    return java.util.Optional.empty();
                })
                .map(UserPrincipal::fromUser)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
