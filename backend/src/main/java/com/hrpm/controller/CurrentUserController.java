package com.hrpm.controller;


import com.hrpm.common.ApiResponse;
import com.hrpm.security.AuthenticatedUser;
import com.hrpm.vo.CurrentUserVO;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrentUserController {
    @GetMapping("/me")
    public ApiResponse<CurrentUserVO> currentUser(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(new CurrentUserVO(Long.toString(user.userId()), user.username()));
    }
}
