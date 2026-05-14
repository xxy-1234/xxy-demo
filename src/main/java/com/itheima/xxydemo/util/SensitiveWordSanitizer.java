package com.itheima.xxydemo.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SensitiveWordSanitizer {

    private static final String[][] REPLACEMENTS = {
            {"傻逼", "***"},
            {"傻b", "***"},
            {"操你", "**"},
            {"fuck", "****"},
            {"shit", "****"},
    };

    public String sanitize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String out = text;
        for (String[] pair : REPLACEMENTS) {
            Pattern p = Pattern.compile(Pattern.quote(pair[0]), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            out = p.matcher(out).replaceAll(pair[1]);
        }
        return out;
    }
}
