package org.cassidylabs.marginalia.application.service;

import lombok.RequiredArgsConstructor;
import org.cassidylabs.marginalia.application.port.in.annotation.AnnotationUseCase;
import org.cassidylabs.marginalia.application.port.out.AnnotationPort;
import org.cassidylabs.marginalia.application.port.out.DocumentPort;
import org.cassidylabs.marginalia.domain.annotation.Annotation;
import org.cassidylabs.marginalia.global.exception.AnnotationNotFoundException;
import org.cassidylabs.marginalia.global.exception.DocumentNotFoundException;
import org.cassidylabs.marginalia.global.exception.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AnnotationService implements AnnotationUseCase {

    private final AnnotationPort annotationPort;
    private final DocumentPort documentPort;

    @Override
    public AnnotationResult save(SaveCommand command) {
        var document = documentPort.findById(command.documentId())
                .orElseThrow(() -> new DocumentNotFoundException(command.documentId()));
        if (!document.isOwnedBy(command.userId())) {
            throw new UnauthorizedException("문서에 접근 권한이 없습니다.");
        }

        Annotation annotation = annotationPort.save(
                Annotation.create(
                        command.documentId(),
                        command.userId(),
                        command.pageNumber(),
                        command.rects(),
                        command.quoteSelector(),
                        command.textSnapshot(),
                        command.type(),
                        command.color(),
                        command.memo()
                )
        );
        return toResult(annotation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnotationResult> getByPage(UUID documentId, int pageNumber, UUID userId) {
        return annotationPort
                .findByDocumentIdAndPageNumberAndUserId(documentId, pageNumber, userId)
                .stream()
                .map(this::toResult)
                .toList();
    }

    @Override
    public AnnotationResult updateMemo(UUID annotationId, UUID userId, String memo) {
        Annotation annotation = getOwnedAnnotation(annotationId, userId);
        annotation.updateMemo(memo);
        return toResult(annotationPort.save(annotation));
    }

    @Override
    public void delete(UUID annotationId, UUID userId) {
        annotationPort.delete(getOwnedAnnotation(annotationId, userId));
    }

    // ── private ───────────────────────────────────────────────────────────────

    private Annotation getOwnedAnnotation(UUID annotationId, UUID userId) {
        Annotation annotation = annotationPort.findById(annotationId)
                .orElseThrow(() -> new AnnotationNotFoundException(annotationId));
        if (!annotation.isOwnedBy(userId)) {
            throw new UnauthorizedException("어노테이션에 접근 권한이 없습니다.");
        }
        return annotation;
    }

    private AnnotationResult toResult(Annotation a) {
        return new AnnotationResult(
                a.getId(), a.getDocumentId(), a.getPageNumber(),
                a.getRects(), a.getQuoteSelector(), a.getTextSnapshot(),
                a.getType(), a.getColor(), a.getMemo(), a.getCreatedAt()
        );
    }
}
