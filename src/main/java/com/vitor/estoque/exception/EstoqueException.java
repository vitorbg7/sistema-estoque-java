package com.vitor.estoque.exception;

/**
 * Exceção base de todas as regras de negócio do sistema de estoque.
 * É "unchecked" (RuntimeException) para não poluir cada método com throws.
 */
public class EstoqueException extends RuntimeException {

    public EstoqueException(String mensagem) {
        super(mensagem);
    }

    public EstoqueException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
