package ai.metaphor.metaphor_llm_processor.indexing;

import ai.metaphor.metaphor_llm_processor.exception.IndexingException;
import ai.metaphor.metaphor_llm_processor.model.DocumentStatus;
import ai.metaphor.metaphor_llm_processor.model.IndexedDocument;
import ai.metaphor.metaphor_llm_processor.model.IndexedDocumentChunk;
import ai.metaphor.metaphor_llm_processor.model.OriginType;
import ai.metaphor.metaphor_llm_processor.repository.IndexedDocumentChunkRepository;
import ai.metaphor.metaphor_llm_processor.repository.IndexedDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DocumentIndexingServiceImpl implements DocumentIndexingService {

    private final ResourceDocumentReader resourceDocumentReader;
    private final TextSplitter textSplitter;
    private final IndexedDocumentRepository documentRepository;
    private final IndexedDocumentChunkRepository chunkRepository;

    public DocumentIndexingServiceImpl(ResourceDocumentReader resourceDocumentReader,
                                       TextSplitter textSplitter,
                                       IndexedDocumentRepository documentRepository,
                                       IndexedDocumentChunkRepository chunkRepository) {
        this.resourceDocumentReader = resourceDocumentReader;
        this.textSplitter = textSplitter;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    @Override
    public List<IndexedDocumentChunk> indexFromURL(String source, String origin) {
        log.info("Indexing a document: sourcePath = {}, sourceOrigin = {}", source, origin);
        try {
            var resource = new UrlResource(source);
            if (!resource.exists()) {
                throw new IndexingException(String.format("Resource %s does not exist", source));
            }

            IndexedDocument indexedDocument = createDocument(resource, source, origin);
            List<IndexedDocumentChunk> chunks = sliceDocumentToChunks(indexedDocument, resource);
            log.info("Successfully indexed {}/{} and stored {} chunks.",
                    indexedDocument.getId(), indexedDocument.getName(), chunks.size()
            );
            // the document is now ready for processing
            indexedDocument.setStatus(DocumentStatus.PENDING);
            documentRepository.save(indexedDocument);
            return chunks;
        } catch (MalformedURLException e) {
            String errMessage = String.format("Malformed URL: %s", source);
            log.error("Unable to index a document from URL. {}", errMessage, e);
            throw new IndexingException(errMessage, e);
        } catch (IOException e) {
            String errMessage = String.format("Unable to access URL %s", source);
            log.error("Unable to index a document from URL. {}", errMessage, e);
            throw new IndexingException(errMessage, e);
        }
    }

    private IndexedDocument createDocument(Resource resource, String sourcePath, String sourceOrigin) throws IOException {
        String documentContent = resource.getContentAsString(StandardCharsets.UTF_8);
        IndexedDocument indexedDocument = IndexedDocument
                .builder()
                .name(resource.getDescription())
                .text(documentContent)
                .type(OriginType.URL)
                .path(sourcePath)
                .origin(sourceOrigin)
                .build();
        return documentRepository.save(indexedDocument);
    }

    private List<IndexedDocumentChunk> sliceDocumentToChunks(IndexedDocument indexedDocument, Resource resource) {
        List<Document> processedDocuments = processDocument(resource);
        int indexOrder = 1;
        List<IndexedDocumentChunk> chunks = new ArrayList<>();
        for (Document document : processedDocuments) {
            var chunk = IndexedDocumentChunk.builder()
                    .documentId(indexedDocument.getId())
                    // TODO: check if text or formatted content is more suitable
                    .text(document.getText())
                    .order(indexOrder++)
                    .build();
            chunks.add(chunk);
        }
        return chunkRepository.saveAll(chunks);
    }

    private List<Document> processDocument(Resource resource) {
        var parsedDocuments = resourceDocumentReader.readFrom(resource);
        return textSplitter.split(parsedDocuments);
    }
}
