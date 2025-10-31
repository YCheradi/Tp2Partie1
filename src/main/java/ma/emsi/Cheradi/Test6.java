package ma.emsi.Cheradi;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import ma.emsi.Cheradi.tools.meteo.MeteoTool;

import java.time.Duration;
import java.util.Scanner;

public class Test6 {

    // Interface de l’assistant IA
    interface AssistantMeteo {
        String repond(String question);
    }

    public static void main(String[] args) {

        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("⚠️  Variable d'environnement GEMINI_API_KEY manquante !");
            return;
        }

        // Création du modèle de chat Gemini
        ChatModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.0-flash")
                .temperature(0.3)
                .responseFormat(ResponseFormat.TEXT)
                .timeout(Duration.ofSeconds(90))
                .logRequestsAndResponses(true)
                .build();

        // Création de l’assistant météo avec outil
        AssistantMeteo assistant = AiServices.builder(AssistantMeteo.class)
                .chatModel(model)
                .tools(new MeteoTool())   // Ajout de l’outil
                .build();

        // Choisir le mode de test
        // modeAuto(assistant);      // → tests automatiques
        modeInteractif(assistant);   // → tests interactifs
    }

    // Mode automatique (tests fixes)
    private static void modeAuto(AssistantMeteo assistant) {
        demander(assistant, "Quel temps fait-il à Paris ?");
        demander(assistant, "J'ai prévu d'aller aujourd'hui à Casablanca. Est-ce que je prends un parapluie ?");
        demander(assistant, "Peux-tu me donner la météo de Zqxyz-ville ?");
        demander(assistant, "Explique-moi la différence entre compilation et interprétation.");
    }

    // Mode interactif (l’utilisateur tape)
    private static void modeInteractif(AssistantMeteo assistant) {
        System.out.println(">>> Mode interactif. Tape une question (ou 'fin' pour quitter).");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\nTa question > ");
                String question = scanner.nextLine();

                if (question == null || question.isBlank()) continue;
                if ("fin".equalsIgnoreCase(question.trim())) {
                    System.out.println("👋 Fin de la conversation.");
                    break;
                }

                String reponse = assistant.repond(question);
                System.out.println("Assistant : " + reponse);
            }
        }
    }

    // Méthode utilitaire pour le mode automatique
    private static void demander(AssistantMeteo assistant, String question) {
        System.out.println("==================================================");
        System.out.println("Question : " + question);
        String reponse = assistant.repond(question);
        System.out.println("Assistant : " + reponse);
    }
}
