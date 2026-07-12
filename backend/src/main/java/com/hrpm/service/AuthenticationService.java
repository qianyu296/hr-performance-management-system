package com.hrpm.service;


import com.hrpm.common.exception.AuthenticationFailedException;
import com.hrpm.dto.LoginDTO;
import com.hrpm.entity.UserAccount;
import com.hrpm.mapper.UserAccountMapper;
import com.hrpm.vo.LoginVO;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {
    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthenticationService(
            UserAccountMapper userAccountMapper, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public LoginVO login(LoginDTO request) {
        UserAccount account = userAccountMapper.findByUsername(request.username());
        if (account == null || !"ACTIVE".equals(account.status()) || !passwordEncoder.matches(request.password(), account.passwordHash())) {
            throw new AuthenticationFailedException();
        }
        userAccountMapper.updateLastLoginTime(account.id());
        return new LoginVO(tokenService.issue(account.id(), account.username(), account.sessionVersion()), "Bearer");
    }
}
