package com.mvc.enums;

/**
 * @author xhzy
 */

public enum ModifiersEnum {
    /**
     * 访问修饰符
     */
    PUBLIC("public"),
    PRIVATE("private"),
    PROTECTED("protected"),
    NULL("*");

    private final String key;

    ModifiersEnum(String key){
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
