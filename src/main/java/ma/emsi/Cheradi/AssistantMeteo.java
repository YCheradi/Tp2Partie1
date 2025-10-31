package ma.emsi.Cheradi;

/**
 * Interface de l'assistant. LangChain4j génère l'implémentation via AiServices.
 * Le nom de méthode est libre (ici: repond).
 */
public interface AssistantMeteo {
    String repond(String message);
}
