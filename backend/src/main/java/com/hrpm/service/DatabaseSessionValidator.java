package com.hrpm.service;


import com.hrpm.entity.UserAccount;
import com.hrpm.mapper.UserAccountMapper;
import com.hrpm.security.AuthenticatedUser;
import com.hrpm.security.SessionValidator;

import org.springframework.stereotype.Component;

@Component
public class DatabaseSessionValidator implements SessionValidator {
    private final UserAccountMapper userAccountMapper;

    public DatabaseSessionValidator(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    public boolean isValid(AuthenticatedUser user) {
        UserAccount account = userAccountMapper.findById(user.userId());
        return account != null
                && "ACTIVE".equals(account.status())
                && account.sessionVersion() == user.sessionVersion();
    }
}
