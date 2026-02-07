package pt.dcs.example.spring_ai.a2a.writer;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.a2a.server.executor.DefaultAgentExecutor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import java.util.List;

@SpringBootApplication
public class WriterApplication {

    private final Logger logger = LoggerFactory.getLogger(WriterApplication.class);

    @Value("classpath:/system-prompt-template.st")
    private Resource systemPromptTemplate;

    public static void main(String[] args) {
        SpringApplication.run(WriterApplication.class, args);
    }

    @Bean
    public AgentCard agentCard(@Value("${server.port:8080}") int port,
                               @Value("${server.servlet.context-path:}") String contextPath) {
        // This AgentCard is automatically exposed at /.well-known/agent-card.json
        // Other agents discover this agent's capabilities through this endpoint
        return new AgentCard.Builder()
                .name("Content Writer Agent")
                .description("An agent that can write a comprehensive and engaging piece of content based on the provided outline and high-level description of the content")
                .url("http://localhost:" + port + contextPath + "/")
                .version("1.0.0")
                .documentationUrl("http://localhost:" + port + "/docs")
                .capabilities(
                        new AgentCapabilities.Builder()
                                .streaming(false)
                                .pushNotifications(false)
                                .stateTransitionHistory(false)
                                .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(new AgentSkill.Builder()
                        .id("writer")
                        .name("Writes content using an outline")
                        .description("Writes content using a given outline and high-level description of the content")
                        .tags(List.of("writer"))
                        .examples(List.of("Write a short, upbeat, and encouraging twitter post about learning Java. Base your writing on the given outline."))
                        .build()))
                .protocolVersion("0.3.3")
                .build();
    }

    @Bean
    public AgentExecutor agentExecutor(ChatClient.Builder chatClientBuilder) {

        ChatClient chatClient = chatClientBuilder
                .defaultSystem(promptSystemSpec -> promptSystemSpec
                        .text(systemPromptTemplate))
                .defaultAdvisors(new MyLoggingAdvisor(10))
                .build();

        return new DefaultAgentExecutor(chatClient, (chat, requestContext) -> {
            String userMessage = DefaultAgentExecutor.extractTextFromMessage(requestContext.getMessage());
            return chat.prompt().user(userMessage).call().content();
        });
    }
}
