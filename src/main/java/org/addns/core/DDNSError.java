package org.addns.core;

public class DDNSError extends RuntimeException {

    public DDNSError(String message, Object... args) {
        super(String.format(message, args));
    }

}
