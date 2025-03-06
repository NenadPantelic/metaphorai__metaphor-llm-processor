package ai.metaphor.metaphor_llm_processor.indexing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ResourceDocumentReader {

    public List<Document> readFrom(Resource resource) {
        log.info("Reading a document from {}", resource.getDescription());
        return new TikaDocumentReader(resource).read();
    }
}
