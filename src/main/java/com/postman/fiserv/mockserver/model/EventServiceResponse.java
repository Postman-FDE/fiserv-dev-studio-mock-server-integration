package com.postman.fiserv.mockserver.model;

import java.util.List;

public record EventServiceResponse(List<FileOperationResult> results) {}
