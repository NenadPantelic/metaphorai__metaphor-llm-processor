package ai.metaphor.metaphor_llm_processor.llm.client;

import org.springframework.ai.chat.model.ChatResponse;

public interface LLMClient {

    String generate(String systemPrompt, String userPrompt);
}
