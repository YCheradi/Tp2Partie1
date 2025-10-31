package ma.emsi.Cheradi;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.time.Duration;
import java.util.List;
import java.util.Scanner;

public class Test5 {

    interface Assistant {
        String repond(String message);
    }

    public static void main(String[] args) {

        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("⚠️  Variable d'environnement GEMINI_API_KEY manquante.");
            return;
        }

        // 1) Modèle de chat
        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.0-flash")
                .temperature(0.2)
                .responseFormat(ResponseFormat.TEXT)
                .timeout(Duration.ofSeconds(90))
                .build();

        // 2) Modèle d'embeddings
        EmbeddingModel embeddingModel = GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-004")
                .build();

        // 3) Charger le PDF
        String nomPdf = "support-ml.pdf"; // mets ici le nom réel de ton PDF
        Document document = FileSystemDocumentLoader.loadDocument(nomPdf);

        // 4) Base vectorielle en mémoire
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 5) Splitter récursif
        DocumentSplitter splitter = DocumentSplitters.recursive(800, 100);

        // 6) Ingestion (chunks -> embeddings -> store)
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .documentSplitter(splitter)
                .embeddingStore(embeddingStore)
                .build();
        ingestor.ingest(document);

        // 7) Retriever top-k
        ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(4)
                .build();

        // 8) Assistant RAG
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(12))
                .contentRetriever(retriever)
                .build();

        // 9) Conversation multi-questions
        conversationAvec(assistant, retriever);
    }

    // Boucle interactive
    private static void conversationAvec(Assistant assistant, ContentRetriever retriever) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("==================================================");
                System.out.println("Posez votre question (ou 'fin' pour quitter) : ");
                String question = scanner.nextLine();
                System.out.println("==================================================");

                if (question == null || question.isBlank()) continue;
                if ("fin".equalsIgnoreCase(question.trim())) {
                    System.out.println("👋 Fin de la conversation.");
                    break;
                }

                // Afficher les segments RAG récupérés (sans introspection avancée des métadonnées)
                try {
                    List<Content> retrieved = retriever.retrieve(Query.from(question));
                    System.out.println("---- Passages récupérés (RAG) ----");
                    for (Content c : retrieved) {
                        TextSegment s = c.textSegment();
                        // Affiche un extrait + métadonnées brutes (toString), sans dépendre d'API non dispo
                        System.out.println("- META: " + String.valueOf(s.metadata()));
                        System.out.println("  TEXTE: " + snip(s.text(), 220));
                    }
                    System.out.println("----------------------------------");
                } catch (Exception e) {
                    System.out.println("(Info) Impossible d’afficher les passages RAG : " + e.getMessage());
                }

                String reponse = assistant.repond(question);
                System.out.println("Assistant : " + reponse);
            }
        }
    }

    // Utilitaire d’aperçu
    private static String snip(String text, int max) {
        return (text == null || text.length() <= max) ? text : text.substring(0, max) + " […]";
    }
}
