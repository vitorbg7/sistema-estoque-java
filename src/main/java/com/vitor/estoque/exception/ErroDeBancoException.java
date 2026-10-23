package com.vitor.estoque.exception;

/**
 * Lançada quando o banco de dados falha (arquivo inacessível, regra violada, etc.).
 * Embrulha a SQLException, que é "checked", para que o service não precise
 * declarar throws em todos os métodos.
 */
public class ErroDeBancoException extends EstoqueException {

    public ErroDeBancoException(String mensagem, Throwable causa) {
        super(causa == null || causa.getMessage() == null ? mensagem : mensagem + " (" + causa.getMessage() + ")", causa);
    }
}
