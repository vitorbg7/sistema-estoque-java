package com.vitor.estoque.exception;

/**
 * Lançada quando um produto com o código informado não existe.
 */
public class ProdutoNaoEncontradoException extends EstoqueException {

    public ProdutoNaoEncontradoException(String codigo) {
        super("Produto com código '" + codigo + "' não encontrado.");
    }
}
