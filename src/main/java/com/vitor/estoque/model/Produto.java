package com.vitor.estoque.model;

import com.vitor.estoque.exception.EstoqueInsuficienteException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/**
 * Produto do estoque. As regras de negócio (preço não negativo, não vender mais
 * do que existe) ficam aqui dentro, garantindo que o objeto nunca fique em
 * estado inválido, não importa quem o use.
 */
public final class Produto {

    private final String codigo;
    private String nome;
    private BigDecimal preco;
    private int quantidadeEmEstoque;
    private int quantidadeMinima;
    private Categoria categoria;
    private Fornecedor fornecedor;

    public Produto(String codigo, String nome, BigDecimal preco, int quantidadeMinima,
                   Categoria categoria, Fornecedor fornecedor) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Código do produto é obrigatório.");
        }
        this.codigo = codigo.trim();
        setNome(nome);
        setPreco(preco);
        setQuantidadeMinima(quantidadeMinima);
        setCategoria(categoria);
        setFornecedor(fornecedor);
        this.quantidadeEmEstoque = 0;
    }

    // ---------- Regras de negócio ----------

    public void adicionarEstoque(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }
        this.quantidadeEmEstoque += quantidade;
    }

    public void removerEstoque(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }
        if (quantidade > quantidadeEmEstoque) {
            throw new EstoqueInsuficienteException(nome, quantidadeEmEstoque, quantidade);
        }
        this.quantidadeEmEstoque -= quantidade;
    }

    public boolean estoqueBaixo() {
        return quantidadeEmEstoque <= quantidadeMinima;
    }

    public BigDecimal valorEmEstoque() {
        return preco.multiply(BigDecimal.valueOf(quantidadeEmEstoque));
    }

    // ---------- Getters e setters ----------

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do produto é obrigatório.");
        }
        this.nome = nome.trim();
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        if (preco == null || preco.signum() < 0) {
            throw new IllegalArgumentException("Preço não pode ser nulo nem negativo.");
        }
        this.preco = preco;
    }

    public int getQuantidadeEmEstoque() {
        return quantidadeEmEstoque;
    }

    public int getQuantidadeMinima() {
        return quantidadeMinima;
    }

    public void setQuantidadeMinima(int quantidadeMinima) {
        if (quantidadeMinima < 0) {
            throw new IllegalArgumentException("Quantidade mínima não pode ser negativa.");
        }
        this.quantidadeMinima = quantidadeMinima;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        if (categoria == null) {
            throw new IllegalArgumentException("Categoria é obrigatória.");
        }
        this.categoria = categoria;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(Fornecedor fornecedor) {
        if (fornecedor == null) {
            throw new IllegalArgumentException("Fornecedor é obrigatório.");
        }
        this.fornecedor = fornecedor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Produto outro)) return false;
        return Objects.equals(codigo, outro.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    @Override
    public String toString() {
        return String.format(Locale.of("pt", "BR"),
                "[%s] %s | %s | R$ %.2f | Estoque: %d (mín. %d) | Fornecedor: %s",
                codigo, nome, categoria, preco, quantidadeEmEstoque, quantidadeMinima,
                fornecedor.getNome());
    }
}
