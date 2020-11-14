package com.boot.mini.enums;

/**
 * @author xhzy
 */

public enum ExceptionEnum {
    /**
     * 异常枚举
     */
    ILLEGAL_ARGUMENT("illegal_argument","参数非法"),
    UNKNOWN_ERROR("unknown_error","未知异常"),
    CLASS_IMPL_NOT_FOUND("class_impl_not_found","未找到实现类"),
    STARTER_NOT_FOUND("starter not found","未找到启动类"),
    STARTER_DUPLICATED("starter duplicated","启动类只能有且仅有一个"),
    CONTROLLER_ADVICE_DUPLICATED("controller_advice_duplicated","异常处理类至多只能有一个"),
    ID_DUPLICATED("id_duplicated","主键冲突"),
    ID_NULL("id_null","缺少主键"),
    LENGTH_NULL("length null","字段长度不能为空"),
    GENERIC_ERROR("generic_null","获取泛型失败"),
    TABLE_NULL("table_name_null","无法获取表名");

    private final String code;

    private final String message;

    ExceptionEnum(String code, String message){
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
