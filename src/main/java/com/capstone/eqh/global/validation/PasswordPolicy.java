package com.capstone.eqh.global.validation;

/**
 * 비밀번호 정책 공통 상수.
 * <ul>
 *   <li>최소 8자</li>
 *   <li>영문 1자 이상</li>
 *   <li>숫자 1자 이상</li>
 *   <li>특수문자 1자 이상</li>
 * </ul>
 */
public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    /**
     * 영문 + 숫자 + 특수문자, 최소 8자 (최대 128자).
     */
    public static final String REGEX =
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,128}$";

    public static final String MESSAGE =
            "비밀번호는 8자 이상이며 영문, 숫자, 특수문자를 포함해야 합니다.";
}
