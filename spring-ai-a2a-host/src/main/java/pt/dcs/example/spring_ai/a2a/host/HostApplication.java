package pt.dcs.example.spring_ai.a2a.host;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HostApplication {

	public static void main(String[] args) {
		SpringApplication.run(HostApplication.class, args);
	}

	@Bean
	public ChatClient chatClient(ChatClient.Builder chatClientBuilder, RemoteAgentConnections remoteAgentConnections) {

		String systemPrompt = """
           You coordinate tasks across specialized agents.
           Available agents:
           %s
           Use the sendMessage tool to delegate tasks to the appropriate agent.
           If you decide to delegate tasks to more than one agent, make sure to pass on the output from the previous agent as input to the next.
           """.formatted(remoteAgentConnections.getAgentDescriptions());

		return chatClientBuilder
				.defaultSystem(systemPrompt)
				.defaultTools(remoteAgentConnections)  // Register as Spring AI tool
				.defaultAdvisors(new MyLoggingAdvisor(10))
				.build();
	}
}
