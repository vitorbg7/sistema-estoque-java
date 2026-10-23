package com.vitor.estoque.model;

public enum Categoria {
    ELETRONICOS("Eletrônicos"),
    ALIMENTOS("Alimentos"),
    LIMPEZA("Limpeza"),
    VESTUARIO("Vestuário"),
    PAPELARIA("Papelaria"),
    OUTROS("Outros");

    private final String descricao;

    Categoria(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
