package pt.dcs.example.spring_ai.a2a.host;

import io.a2a.A2A;
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Service
public class RemoteAgentConnections {

    private final Logger logger = LoggerFactory.getLogger(RemoteAgentConnections.class);

    private final Map<String, AgentCard> agentCards = new HashMap<>();

    public RemoteAgentConnections(@Value("${remote.agents.urls}") List<String> agentUrls) {
        // Discover remote agents at startup (see Agent Discovery section above)
        for (String url : agentUrls) {
            String path;
            try {
                path = new URI(url).getPath();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            AgentCard card = A2A.getAgentCard(url, path + ".well-known/agent-card.json", null);
            this.agentCards.put(card.name(), card);
        }
    }

    @Tool(description = "Sends a task to a remote agent. Use this to delegate work to specialized agents.")
    @SuppressWarnings("unused")
    public String sendMessage(
            @ToolParam(description = "The name of the agent") String agentName,
            @ToolParam(description = "The task description to send") String task)
            throws ExecutionException, InterruptedException, TimeoutException {

        AgentCard agentCard = this.agentCards.get(agentName);

        // Create A2A message
        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(List.of(new TextPart(task, null)))
                .build();

        // Use A2A Java SDK Client
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        AtomicReference<String> responseText = new AtomicReference<>("");

        BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
            if (event instanceof TaskEvent taskEvent) {
                Task consumedTask = taskEvent.getTask();
                logger.info("Received task response: status={}", consumedTask.getStatus().state());

                if (consumedTask.getStatus().state().isFinal()) {
                    // Extract text from artifacts
                    if (consumedTask.getArtifacts() != null) {
                        StringBuilder sb = new StringBuilder();
                        for (Artifact artifact : consumedTask.getArtifacts()) {
                            if (artifact.parts() != null) {
                                for (Part<?> part : artifact.parts()) {
                                    if (part instanceof TextPart textPart) {
                                        sb.append(textPart.getText());
                                    }
                                }
                            }
                        }
                        responseText.set(sb.toString());
                    }
                    responseFuture.complete(responseText.get());
                }
            }
        };

        // Create client with consumer via builder
        ClientConfig clientConfig = new ClientConfig.Builder().setAcceptedOutputModes(List.of("text")).build();

        Client client = Client.builder(agentCard)
                .clientConfig(clientConfig)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
                .addConsumers(List.of(consumer))
                .build();

        client.sendMessage(message);
        // Wait for response (with timeout)
        String result = responseFuture.get(120, TimeUnit.SECONDS);
        logger.info("Agent '{}' response: {}", agentName, result);
        return result;
    }

    public String getAgentDescriptions() {
        return agentCards.values().stream()
                .map(card -> card.name() + ": " + card.description())
                .collect(Collectors.joining("\n"));
    }
}