package com.banking.transactions.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    protected int httpCode;

    public ApiException(String message) {
        super(message);
        this.httpCode = 500;
    }

    public ApiException() {
        super("Something went wrong");
        this.httpCode = 500;
    }
}
