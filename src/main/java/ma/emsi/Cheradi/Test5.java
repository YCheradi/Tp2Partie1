package ma.emsi.Cheradi;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
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

        System.out.println("▶ Démarrage Test5…");

        // 0) Clé API (fallback GEMINI_KEY)
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("GEMINI_KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("❌ Variable d'environnement GEMINI_API_KEY (ou GEMINI_KEY) manquante.");
            System.out.println("   IntelliJ > Run/Debug Configurations > Environment variables");
            return;
        }

        // 1) Modèle de chat
        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.0-flash")
                .temperature(0.2)
                .responseFormat(ResponseFormat.TEXT)
                .timeout(Duration.ofSeconds(90))
                .logRequestsAndResponses(false)
                .build();

        // 2) Modèle d'embeddings (avec timeout)
        EmbeddingModel embeddingModel = GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-004")
                .timeout(Duration.ofSeconds(120))
                .build();

        // 3) Charger le PDF avec PARSEUR PDFBOX
        String nomPdf = "support-ml.pdf"; // adapte si besoin
        System.out.println("📂 Répertoire de travail: " + java.nio.file.Paths.get("").toAbsolutePath());
        java.nio.file.Path pdfPath = java.nio.file.Paths.get(nomPdf);
        if (!java.nio.file.Files.exists(pdfPath)) {
            System.out.println("❌ PDF introuvable: " + pdfPath.toAbsolutePath());
            return;
        }
        System.out.println("✅ PDF trouvé: " + pdfPath.toAbsolutePath());

        Document document = FileSystemDocumentLoader.loadDocument(
                nomPdf,
                new ApachePdfBoxDocumentParser() // <<< IMPORTANT
        );

        // 4) Base vectorielle en mémoire
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 5) Splitter (moins de chunks => moins d’appels réseau)
        DocumentSplitter splitter = DocumentSplitters.recursive(3000, 200);

        // 6) Ingestion avec logs
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .documentSplitter(splitter)
                .embeddingStore(embeddingStore)
                .build();

        System.out.println("⏳ Ingestion en cours (génération des embeddings) …");
        try {
            ingestor.ingest(document);
            System.out.println("✅ Ingestion terminée.");
        } catch (Exception e) {
            System.out.println("❌ Erreur pendant l’ingestion : " + e.getMessage());
            e.printStackTrace();
            return;
        }

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

        // 9) Conversation
        conversationAvec(assistant, retriever);
    }

    private static void conversationAvec(Assistant assistant, ContentRetriever retriever) {
        System.out.println("💬 Prêt. Posez votre question (ou 'fin' pour quitter) :");
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("==================================================");
                System.out.print("Votre question > ");
                String question = scanner.nextLine();
                System.out.println("==================================================");

                if (question == null || question.isBlank()) continue;
                if ("fin".equalsIgnoreCase(question.trim())) {
                    System.out.println("👋 Fin de la conversation.");
                    break;
                }

                // Afficher les segments RAG récupérés
                try {
                    List<Content> retrieved = retriever.retrieve(Query.from(question));
                    System.out.println("---- Passages récupérés (RAG) ----");
                    if (retrieved == null || retrieved.isEmpty()) {
                        System.out.println("(aucun passage pertinent trouvé)");
                    } else {
                        for (Content c : retrieved) {
                            TextSegment s = c.textSegment();
                            String meta = (s != null && s.metadata() != null) ? s.metadata().toString() : "{}";
                            String txt = (s != null) ? s.text() : "";
                            System.out.println("- META: " + meta);
                            System.out.println("  TEXTE: " + snip(txt, 220));
                        }
                    }
                    System.out.println("----------------------------------");
                } catch (Exception e) {
                    System.out.println("(Info) Impossible d’afficher les passages RAG : " + e.getMessage());
                }

                String reponse;
                try {
                    reponse = assistant.repond(question);
                } catch (Exception e) {
                    reponse = "Erreur pendant l'appel au modèle: " + e.getMessage();
                }
                System.out.println("Assistant : " + reponse);
            }
        }
    }

    // Utilitaire d’aperçu
    private static String snip(String text, int max) {
        return (text == null || text.length() <= max) ? text : text.substring(0, max) + " […]";
    }
}
