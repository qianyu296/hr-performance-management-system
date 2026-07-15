package com.hrpm.entity;

public record ExitHandover(long id, long changeId, long handoverEmployeeId, String status, int version) {
}
