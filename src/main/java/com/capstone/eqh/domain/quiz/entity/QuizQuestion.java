package com.capstone.eqh.domain.quiz.entity;

import com.capstone.eqh.domain.lesson.entity.Lesson;
import com.capstone.eqh.domain.quiz.enums.QuizType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_q")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anchor_id")
    private Lesson anchor;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "q_type", nullable = false, length = 20)
    private QuizType questionType;

    @Column(nullable = false)
    private int score;

    @Column(name = "correct_answer", columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "lesson_page")
    private Integer lessonPage;

    @Column(name = "lesson_paragraph")
    private Integer lessonParagraph;

    @Builder.Default
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizOption> options = new ArrayList<>();

    public void update(String questionText, String correctAnswer, String explanation,
                       int score, Integer lessonPage, Integer lessonParagraph) {
        this.questionText = questionText;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.score = score;
        this.lessonPage = lessonPage;
        this.lessonParagraph = lessonParagraph;
    }

    public void updateAnchor(Lesson anchor) {
        this.anchor = anchor;
    }

    public void replaceOptions(List<QuizOption> newOptions) {
        this.options.clear();
        this.options.addAll(newOptions);
    }
}
