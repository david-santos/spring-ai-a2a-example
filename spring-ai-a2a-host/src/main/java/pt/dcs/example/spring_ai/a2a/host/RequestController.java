package pt.dcs.example.spring_ai.a2a.host;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class RequestController {

    private final ChatClient chatClient;

    public RequestController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String request(@RequestBody String userPrompt) {
        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }

}
