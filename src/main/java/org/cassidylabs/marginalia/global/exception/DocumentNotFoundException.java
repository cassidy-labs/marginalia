package org.cassidylabs.marginalia.global.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class DocumentNotFoundException extends BusinessException {

    public DocumentNotFoundException(UUID id) {
        super("문서를 찾을 수 없습니다: " + id, HttpStatus.NOT_FOUND);
    }
}
