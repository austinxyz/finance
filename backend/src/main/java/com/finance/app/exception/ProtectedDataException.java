package com.finance.app.exception;

/**
 * 受保护数据操作异常
 * 当尝试对受保护的家庭数据进行危险操作（删除、清空等）时抛出
 */
public class ProtectedDataException extends RuntimeException {

    private final Long familyId;
    private final String operation;

    public ProtectedDataException(Long familyId, String operation) {
        super(String.format("受保护的家庭数据不允许执行操作: %s (Family ID: %d)", operation, familyId));
        this.familyId = familyId;
        this.operation = operation;
    }

    public ProtectedDataException(Long familyId, String operation, String message) {
        super(message);
        this.familyId = familyId;
        this.operation = operation;
    }

    public Long getFamilyId() {
        return familyId;
    }

    public String getOperation() {
        return operation;
    }
}
