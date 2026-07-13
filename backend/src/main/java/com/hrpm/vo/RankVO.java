package com.hrpm.vo;

import com.hrpm.entity.Rank;

public record RankVO(String id, String code, String name, int rankOrder, String status, String version) {
    public static RankVO from(Rank value) {
        return new RankVO(Long.toString(value.id()), value.code(), value.name(), value.rankOrder(),
                value.status(), Integer.toString(value.version()));
    }
}
