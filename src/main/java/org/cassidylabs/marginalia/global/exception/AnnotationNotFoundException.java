package org.cassidylabs.marginalia.global.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AnnotationNotFoundException extends BusinessException {

    public AnnotationNotFoundException(UUID id) {
        super("어노테이션을 찾을 수 없습니다: " + id, HttpStatus.NOT_FOUND);
    }
}
