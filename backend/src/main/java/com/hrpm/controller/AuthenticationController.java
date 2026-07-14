package com.hrpm.controller;


import com.hrpm.common.ApiResponse;
import com.hrpm.dto.LoginDTO;
import com.hrpm.dto.RefreshTokenDTO;
import com.hrpm.dto.ChangePasswordDTO;
import com.hrpm.security.AuthenticatedUser;
import com.hrpm.service.AuthenticationService;
import com.hrpm.vo.LoginVO;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginDTO request) {
        return ApiResponse.success(authenticationService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginVO> refresh(@Valid @RequestBody RefreshTokenDTO request) {
        return ApiResponse.success(authenticationService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal AuthenticatedUser user) {
        authenticationService.logout(user);
        return ApiResponse.success(null);
    }

    @PostMapping("/change-password")
    public ApiResponse<LoginVO> changePassword(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody ChangePasswordDTO request) {
        return ApiResponse.success(authenticationService.changePassword(user, request));
    }
}
