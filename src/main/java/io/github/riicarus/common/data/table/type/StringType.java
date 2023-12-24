package io.github.riicarus.common.data.table.type;

/**
 * String 类型
 *
 * @author Riicarus
 * @create 2023-12-24 10:49
 * @since 1.0.0
 */
public class StringType extends VarType {

    private static final StringType INSTANCE = new StringType();

    private StringType() {
        super("STRING");
    }

    public static StringType getInstance() {
        return INSTANCE;
    }

}
