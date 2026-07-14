package com.hrpm.vo;

public record LoginVO(String accessToken, String refreshToken, String tokenType, boolean passwordChangeRequired) {
}
