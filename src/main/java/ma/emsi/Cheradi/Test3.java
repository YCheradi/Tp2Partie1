package ma.emsi.Cheradi;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.CosineSimilarity;

import java.time.Duration;

public class Test3 {
    public static void main(String[] args) {
        String cle = System.getenv("GEMINI_API_KEY"); // vérifie que cette variable existe

        EmbeddingModel modele = GoogleAiEmbeddingModel.builder()
                .apiKey(cle)

                .modelName("text-embedding-004")        // OK

                .taskType(GoogleAiEmbeddingModel.TaskType.SEMANTIC_SIMILARITY)
                .outputDimensionality(300) // optionnel; enlève si tu veux la dimension par défaut
                .timeout(Duration.ofSeconds(100))
                .build();

        String phrase1 = "Bonjour, quel âge avez-vous ?";
        String phrase2 = "Salut ! J’ai 24 ans.";

        Response<Embedding> rep1 = modele.embed(phrase1);
        Response<Embedding> rep2 = modele.embed(phrase2);

        Embedding emb1 = rep1.content();
        Embedding emb2 = rep2.content();

        double similarite = CosineSimilarity.between(emb1, emb2);
        System.out.println("Similarité cosinus : " + similarite);
    }
}
