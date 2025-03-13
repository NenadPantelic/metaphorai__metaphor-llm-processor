package ai.metaphor.metaphor_llm_processor.llm.client;

public interface LLMClient {

    String generate(String systemPrompt, String userPrompt);
}
