package pt.dcs.example.spring_ai.a2a.editor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.model.ModelOptionsUtils;

public class MyLoggingAdvisor implements BaseAdvisor {

    private final Logger logger = LoggerFactory.getLogger(MyLoggingAdvisor.class);

    private final int order;

    public MyLoggingAdvisor(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        print("REQUEST", chatClientRequest.prompt().getInstructions());
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        assert chatClientResponse.chatResponse() != null;
        print("RESPONSE", chatClientResponse.chatResponse().getResults());
        return chatClientResponse;
    }

    private void print(String label, Object object) {
        logger.info(">>> {}:{}", label, ModelOptionsUtils.toJsonString(object));
    }

}
