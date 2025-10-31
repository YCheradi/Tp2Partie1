package ma.emsi.Cheradi;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

import java.time.Duration;
public class Test1 {

    public static void main(String[] args) {
        String cle = System.getenv("GEMINI_API_KEY");
        ChatModel modele =
                GoogleAiGeminiChatModel.builder()
                        .apiKey(cle)
                        .modelName("gemini-2.5-flash")
                        .temperature(0.8)
                        .timeout(Duration.ofSeconds(60))
                        .responseFormat(ResponseFormat.JSON)
                        .build();
        ChatRequest requete = ChatRequest.builder()
                .temperature(0.5)
                .messages(SystemMessage.from("Réponds en bégayant"),
                        UserMessage.from("Superficie de Italie ?"))
                .build();
        ChatResponse reponse = modele.chat(requete);
        System.out.println(reponse.aiMessage().text());
        System.out.println(reponse.tokenUsage().totalTokenCount());
    }
}
