package pt.dcs.example.spring_ai.a2a.editor;

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
public class EditorApplication {

	private final Logger logger = LoggerFactory.getLogger(EditorApplication.class);

	@Value("classpath:/system-prompt-template.st")
	private Resource systemPromptTemplate;

	public static void main(String[] args) {
		SpringApplication.run(EditorApplication.class, args);
	}

	@Bean
	public AgentCard agentCard(@Value("${server.port:8080}") int port,
							   @Value("${server.servlet.context-path:}") String contextPath) {
		// This AgentCard is automatically exposed at /.well-known/agent-card.json
		// Other agents discover this agent's capabilities through this endpoint
		return new AgentCard.Builder()
				.name("Content Editor Agent")
				.description("An agent that can that can proof-read, identify, flag and redact Personal Identifiable Information (PII) from content")
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
						.id("editor")
						.name("Edits and redacts content")
						.description("Edits content and identifies, flags and redacts PII")
						.tags(List.of("editor"))
						.examples(List.of("John ***, born on 1990-**-**, can be contacted at ********@example.com or (***) ***-4567. His SSN is ***-**-6789."))
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
