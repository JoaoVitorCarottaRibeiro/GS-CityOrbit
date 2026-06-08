package br.com.fiap.cityorbit.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super(String.format("%s com ID %d não encontrado(a)", resource, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
