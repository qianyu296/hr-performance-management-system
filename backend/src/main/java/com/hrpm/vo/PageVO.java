package com.hrpm.vo;

import java.util.List;

public record PageVO<T>(List<T> records, long total, int page, int pageSize) {
}
