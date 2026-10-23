package com.vitor.estoque.model;

import java.util.Objects;

public final class Fornecedor {

    private final String cnpj;
    private String nome;
    private String telefone;

    public Fornecedor(String cnpj, String nome, String telefone) {
        if (cnpj == null || cnpj.isBlank()) {
            throw new IllegalArgumentException("CNPJ é obrigatório.");
        }
        this.cnpj = cnpj.trim();
        setNome(nome);
        this.telefone = telefone == null ? "" : telefone.trim();
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do fornecedor é obrigatório.");
        }
        this.nome = nome.trim();
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone == null ? "" : telefone.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fornecedor outro)) return false;
        return Objects.equals(cnpj, outro.cnpj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cnpj);
    }

    @Override
    public String toString() {
        return nome + " (CNPJ " + cnpj + ")" + (telefone.isEmpty() ? "" : " - " + telefone);
    }
}
