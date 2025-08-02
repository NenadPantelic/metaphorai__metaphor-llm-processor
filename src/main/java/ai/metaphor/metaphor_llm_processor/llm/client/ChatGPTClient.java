package ai.metaphor.metaphor_llm_processor.llm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Slf4j
@Component
public class ChatGPTClient implements LLMClient {

    private static final String JSON_MD_START_MARKER = "```json";
    private static final String JSON_MD_END_MARKER = "```";

    private final ChatClient chatClient;

    public ChatGPTClient(@Qualifier("openAIChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }


    @Override
    public String generate(String systemPrompt, String userPrompt) {
        log.info("Generating a response based on: systemPrompt = {}, userPrompt = {}", systemPrompt, userPrompt);
        Assert.hasText(userPrompt, "User prompt must not be blank");

        String result = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        // check if it has JSON MD markers. If it does, remove them.
        if (result != null && result.startsWith(JSON_MD_START_MARKER) && result.endsWith(JSON_MD_END_MARKER)) {
            return result.substring(JSON_MD_START_MARKER.length(), result.length() - JSON_MD_END_MARKER.length());
        }

        return result;
    }
}
