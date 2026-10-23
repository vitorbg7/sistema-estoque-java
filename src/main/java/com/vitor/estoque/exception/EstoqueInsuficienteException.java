package com.vitor.estoque.exception;

/**
 * Lançada quando se tenta dar saída em mais unidades do que existem em estoque.
 */
public class EstoqueInsuficienteException extends EstoqueException {

    private final int disponivel;
    private final int solicitado;

    public EstoqueInsuficienteException(String nomeProduto, int disponivel, int solicitado) {
        super("Estoque insuficiente para '" + nomeProduto + "': disponível " + disponivel
                + ", solicitado " + solicitado + ".");
        this.disponivel = disponivel;
        this.solicitado = solicitado;
    }

    public int getDisponivel() {
        return disponivel;
    }

    public int getSolicitado() {
        return solicitado;
    }
}
