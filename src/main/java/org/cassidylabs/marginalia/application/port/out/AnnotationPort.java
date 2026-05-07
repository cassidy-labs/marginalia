package org.cassidylabs.marginalia.application.port.out;

import org.cassidylabs.marginalia.domain.annotation.Annotation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnnotationPort {

    /** idx_annotations_doc_page 인덱스 적중 */
    List<Annotation> findByDocumentIdAndPageNumberAndUserId(UUID documentId, int pageNumber, UUID userId);

    Optional<Annotation> findById(UUID id);

    Annotation save(Annotation annotation);

    void delete(Annotation annotation);
}
