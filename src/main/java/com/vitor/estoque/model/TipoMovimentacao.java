package com.vitor.estoque.model;

public enum TipoMovimentacao {
    ENTRADA("Entrada"),
    SAIDA("Saída");

    private final String descricao;

    TipoMovimentacao(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
