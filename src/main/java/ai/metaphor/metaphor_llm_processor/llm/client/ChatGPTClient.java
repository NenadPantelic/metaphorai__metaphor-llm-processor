package ai.metaphor.metaphor_llm_processor.llm.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Optional;

@Slf4j
@Component
public class ChatGPTClient implements LLMClient {

    private final ChatClient chatClient;

    public ChatGPTClient(@Qualifier("openAIChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }


    @Override
    public String generate(String systemPrompt, String userPrompt) {
        log.info("Generating a response based on: systemPrompt = {}, userPrompt = {}", systemPrompt, userPrompt);
        Assert.hasText(userPrompt, "User prompt must not be blank");

        return chatClient.prompt()
                .system(Optional.ofNullable(systemPrompt).orElse(""))
                .user(userPrompt)
                .call()
                .content();
    }
}
