package com.hrpm.service;


import com.hrpm.common.exception.AuthenticationFailedException;
import com.hrpm.dto.LoginDTO;
import com.hrpm.dto.RefreshTokenDTO;
import com.hrpm.entity.UserAccount;
import com.hrpm.mapper.UserAccountMapper;
import com.hrpm.security.AuthenticatedUser;
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
        return issueSession(account);
    }

    @Transactional(readOnly = true)
    public LoginVO refresh(RefreshTokenDTO request) {
        AuthenticatedUser tokenUser = tokenService.verifyRefresh(request.refreshToken());
        UserAccount account = userAccountMapper.findById(tokenUser.userId());
        if (account == null || !"ACTIVE".equals(account.status()) || account.sessionVersion() != tokenUser.sessionVersion()) {
            throw new AuthenticationFailedException();
        }
        return issueSession(account);
    }

    @Transactional
    public void logout(AuthenticatedUser user) {
        if (userAccountMapper.incrementSessionVersion(user.userId(), user.sessionVersion()) != 1) {
            throw new AuthenticationFailedException();
        }
    }

    private LoginVO issueSession(UserAccount account) {
        return new LoginVO(
                tokenService.issueAccess(account.id(), account.username(), account.sessionVersion()),
                tokenService.issueRefresh(account.id(), account.username(), account.sessionVersion()),
                "Bearer");
    }
}
