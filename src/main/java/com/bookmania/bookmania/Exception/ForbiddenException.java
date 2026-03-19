// ForbiddenException.java — 403
package com.bookmania.bookmania.Exception;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}