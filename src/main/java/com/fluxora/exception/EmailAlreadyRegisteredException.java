package com.fluxora.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException(String email){
        super("The email is already registered: "+ email);
    }
}
