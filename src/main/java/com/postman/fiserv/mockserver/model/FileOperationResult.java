package com.postman.fiserv.mockserver.model;

public record FileOperationResult(
        String filePath,
        OperationType operation,
        OperationStatus status,
        String message
) {
    public static FileOperationResult ok(String filePath, OperationType operation, String message) {
        return new FileOperationResult(filePath, operation, OperationStatus.OK, message);
    }

    public static FileOperationResult failed(String filePath, OperationType operation, String message) {
        return new FileOperationResult(filePath, operation, OperationStatus.FAILED, message);
    }
}
