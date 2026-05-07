package org.cassidylabs.marginalia.application.port.in.annotation;

import org.cassidylabs.marginalia.domain.annotation.AnnotationColor;
import org.cassidylabs.marginalia.domain.annotation.AnnotationType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnnotationUseCase {

    record SaveCommand(
            UUID documentId,
            UUID userId,
            int pageNumber,
            String rects,           // JSON 문자열
            String quoteSelector,   // JSON 문자열
            String textSnapshot,
            AnnotationType type,
            AnnotationColor color,
            String memo             // nullable
    ) {}

    record AnnotationResult(
            UUID id,
            UUID documentId,
            int pageNumber,
            String rects,
            String quoteSelector,
            String textSnapshot,
            AnnotationType type,
            AnnotationColor color,
            String memo,
            Instant createdAt
    ) {}

    AnnotationResult save(SaveCommand command);

    List<AnnotationResult> getByPage(UUID documentId, int pageNumber, UUID userId);

    AnnotationResult updateMemo(UUID annotationId, UUID userId, String memo);

    void delete(UUID annotationId, UUID userId);
}
