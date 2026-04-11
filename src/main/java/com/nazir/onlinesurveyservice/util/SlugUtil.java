package com.nazir.onlinesurveyservice.util;

import lombok.experimental.UtilityClass;

import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

@UtilityClass
public class SlugUtil {

    private static final Pattern NON_ASCII   = Pattern.compile("[^\\p{ASCII}]");
    private static final Pattern NON_SLUG    = Pattern.compile("[^a-z0-9\\-]");
    private static final Pattern MULTI_DASH  = Pattern.compile("-{2,}");

    public static String generate(String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD);
        String ascii      = NON_ASCII.matcher(normalized).replaceAll("");
        String lower      = ascii.toLowerCase().trim().replace(' ', '-');
        String slug       = NON_SLUG.matcher(lower).replaceAll("");
        slug              = MULTI_DASH.matcher(slug).replaceAll("-");
        // Append short UUID suffix to guarantee uniqueness
        String suffix     = UUID.randomUUID().toString().substring(0, 8);
        return slug + "-" + suffix;
    }
}
