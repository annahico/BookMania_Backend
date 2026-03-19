// BusinessException.java — 400
package com.bookmania.bookmania.Exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}