package pt.dcs.example.spring_ai.a2a.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Map;
import java.util.Random;

@SpringBootApplication
public class HostApplication {

	private final Logger logger = LoggerFactory.getLogger(HostApplication.class);

	private static final String USER_PROMPT = """
			Check the weather in Oeiras (Portugal) now and show the creative response!
			Please incorporate all creative responses from all LLM providers.

			Please use the weather poem (returned from the tool) to find a publisher online.
			List the top 3 most relevant publishers for this poem.
			""";

	public static void main(String[] args) {
		SpringApplication.run(HostApplication.class, args).close(); // The application starts, executes the weather query, displays the result, and then exits cleanly.
	}

	@Bean
	public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
		// Creates a configured ChatClient bean using Spring AI's auto-configured builder.
		// The builder is automatically populated with default settings and configurations from application.properties.
		return chatClientBuilder.build();
	}

	@Bean
	public CommandLineRunner predefinedQuestions(ChatClient chatClient, ToolCallbackProvider mcpToolProvider) {
		// Runs automatically after the application context is fully loaded. It injects the configured ChatClient for AI model interaction and the ToolCallbackProvider which contains all registered MCP tools from connected servers.
		return args -> logger.info(chatClient.prompt(USER_PROMPT) // Instructs the AI model to get Oeiras's current weather. The AI model automatically discovers and calls the appropriate MCP tools based on the prompt.
				.toolContext(Map.of("progressToken", "token-" + new Random().nextInt())) // Uses the toolContext to pass a unique progressToken to MCP tools annotated with @McpProgressToken parameter.
				.toolCallbacks(mcpToolProvider) // This crucial line connects the ChatClient to all available MCP tools. mcpToolProvider is auto-configured by Spring AI's MCP Client starter.
				.call()                         // Contains all tools from connected MCP servers (configured via spring.ai.mcp.client.*.connections.*).
				.content());                    // The AI model can automatically discover and invoke these tools during conversation.
	}

}
