package org.cassidylabs.marginalia.global.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class UploadNotCompleteException extends BusinessException {

    public UploadNotCompleteException(UUID documentId) {
        super("스토리지에 파일이 존재하지 않습니다: " + documentId, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
