package ai.metaphor.metaphor_llm_processor.indexing;

public interface DocumentIndexingService {

    /**
     * Indexes a document by retrieving the content from the given URL.
     *
     * @param sourcePath   a path (URL in this case) of the indexing document
     * @param sourceOrigin an origin (the filesystem or the network identity) which owns the resource
     */
    void indexFromURL(String sourcePath, String sourceOrigin);

    // TODO: later an indexing from the file could be added
}
