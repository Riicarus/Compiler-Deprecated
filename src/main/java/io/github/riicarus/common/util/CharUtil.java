package io.github.riicarus.common.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 字符工具类, 判断输入字符的类型.
 *
 * @author Riicarus
 * @create 2023-11-7 22:52
 * @since 1.0.0
 */
public class CharUtil {

    public static final char L_BRACKET = '(';
    public static final char R_BRACKET = ')';
    public static final char CLOSURE = '*';
    public static final char CONCAT = '.';
    public static final char UNION = '|';
    public static final char BACKSLASH = '\\';
    public static final char L_BRACKET_ESCAPED = (char) 128;
    public static final char R_BRACKET_ESCAPED = (char) 129;
    public static final char CLOSURE_ESCAPED = (char) 130;
    public static final char CONCAT_ESCAPED = (char) 131;
    public static final char UNION_ESCAPED = (char) 132;
    public static final char BACKSLASH_ESCAPED = (char) 133;
    public static final char EPS_TRANS_VALUE = (char) 134;

    private static final Set<Character> FUNCTIONAL_CHAR_SET = Set.of(
            L_BRACKET,
            R_BRACKET,
            CLOSURE,
            CONCAT,
            UNION,
            BACKSLASH
    );

    public static boolean isFunctionalChar(char c) {
        return FUNCTIONAL_CHAR_SET.contains(c);
    }

    private static final Map<Character, Character> ESCAPE_CHAR_MAP = Map.of(
            L_BRACKET, L_BRACKET_ESCAPED,
            R_BRACKET, R_BRACKET_ESCAPED,
            CLOSURE, CLOSURE_ESCAPED,
            CONCAT, CONCAT_ESCAPED,
            UNION, UNION_ESCAPED,
            BACKSLASH, BACKSLASH_ESCAPED
    );

    private static final Map<Character, Character> ESCAPE_BACK_CHAR_MAP = ESCAPE_CHAR_MAP.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    public static boolean canEscape(char c) {
        return ESCAPE_CHAR_MAP.containsKey(c);
    }

    public static char escape(char c) {
        return ESCAPE_CHAR_MAP.get(c);
    }

    public static boolean shouldEscapeBack(char c) {
        return ESCAPE_BACK_CHAR_MAP.containsKey(c);
    }

    public static char escapeBack(char c) {
        return ESCAPE_BACK_CHAR_MAP.get(c);
    }

    public static int precedence(char c) {
        switch (c) {
            case CLOSURE -> {
                return 3;
            }
            case CONCAT -> {
                return 2;
            }
            case UNION -> {
                return 1;
            }
            default -> {
                return -1;
            }
        }
    }

    private static final Set<Character> DEFAULT_CHAR_SET = new HashSet<>();

    static {
        for (int i = 0; i <= 127; i++) {
            DEFAULT_CHAR_SET.add((char) i);
        }
    }

    public static Set<Character> PASCAL_CHAR_SET = new HashSet<>();

    public static Set<Character> getDefaultASCIICharSet() {
        return Collections.unmodifiableSet(DEFAULT_CHAR_SET);
    }

    static {
        for (int i = 48; i <= 62; i++) {
            PASCAL_CHAR_SET.add((char) i);
        }

        for (int i = 65; i <= 90; i++) {
            PASCAL_CHAR_SET.add((char) i);
        }

        for (int i = 97; i <= 122; i++) {
            PASCAL_CHAR_SET.add((char) i);
        }

        PASCAL_CHAR_SET.addAll(List.of('*', '-', '(', ')', '\n', '\r', ' ', (char) 26));
        PASCAL_CHAR_SET.addAll(ESCAPE_CHAR_MAP.values());
    }

    public static final String LETTER_REGEX = "a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z|A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z";
    public static final String DIGIT_REGEX = "0|1|2|3|4|5|6|7|8|9";
    public static final String IDENTIFIER_REGEX = L_BRACKET + LETTER_REGEX + R_BRACKET + L_BRACKET + LETTER_REGEX + UNION + DIGIT_REGEX + R_BRACKET + CLOSURE;
    public static final String NUMBER_REGEX = L_BRACKET + DIGIT_REGEX + R_BRACKET + L_BRACKET + DIGIT_REGEX + R_BRACKET + CLOSURE;

}
