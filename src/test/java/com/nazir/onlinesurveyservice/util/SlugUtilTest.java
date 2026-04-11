package com.nazir.onlinesurveyservice.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SlugUtil unit tests")
class SlugUtilTest {

    @Test
    @DisplayName("generates a URL-safe lowercase slug from a title")
    void generate_producesUrlSafeSlug() {
        String slug = SlugUtil.generate("My Awesome Survey!");

        assertThat(slug)
                .matches("[a-z0-9\\-]+")
                .contains("my-awesome-survey");
    }

    @Test
    @DisplayName("two calls with the same title produce different slugs (UUID suffix)")
    void generate_isUnique() {
        String s1 = SlugUtil.generate("Same Title");
        String s2 = SlugUtil.generate("Same Title");
        assertThat(s1).isNotEqualTo(s2);
    }

    @Test
    @DisplayName("handles special characters and accented letters")
    void generate_stripsSpecialChars() {
        String slug = SlugUtil.generate("Café & More? Test#1");
        assertThat(slug).matches("[a-z0-9\\-]+");
    }
}
