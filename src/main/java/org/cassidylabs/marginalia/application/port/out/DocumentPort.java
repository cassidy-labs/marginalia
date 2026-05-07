package org.cassidylabs.marginalia.application.port.out;

import org.cassidylabs.marginalia.domain.document.Document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentPort {

    Optional<Document> findById(UUID id);

    List<Document> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Document save(Document document);

    void delete(Document document);
}
