package com.example.helloworld.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class AiQuestionGeneratorService {

    private final Random random = new Random();

    public List<GeneratedQuestion> generate(String topic, String difficulty, int count) {
        String normalizedTopic = topic.trim();
        String level = difficulty == null || difficulty.isBlank() ? "средно" : difficulty.trim().toLowerCase(Locale.ROOT);

        List<GeneratedQuestion> questions = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String questionText = "AI: Кое твърдение за \"" + normalizedTopic + "\" е най-правилно? (ниво: " + level + ", №" + i + ")";

            List<String> options = List.of(
                    "Основна дефиниция и приложение на " + normalizedTopic,
                    "Несвързано твърдение за друга тема",
                    "Често срещана грешка при " + normalizedTopic,
                    "Исторически факт без връзка"
            );

            int correctIndex = random.nextInt(options.size());
            questions.add(new GeneratedQuestion(questionText, 1.0, options, correctIndex));
        }

        return questions;
    }

    public record GeneratedQuestion(
            String questionText,
            double points,
            List<String> options,
            int correctIndex
    ) {
    }
}
