package com.vitor.estoque.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Registro imutável de uma entrada (compra) ou saída (venda) de estoque.
 * Guarda o preço unitário da época, para o histórico não mudar se o preço do
 * produto for alterado depois.
 */
public final class MovimentacaoEstoque {

    private static final AtomicLong SEQUENCIA = new AtomicLong(1);
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Long id;
    private final Produto produto;
    private final TipoMovimentacao tipo;
    private final int quantidade;
    private final BigDecimal precoUnitario;
    private final LocalDateTime dataHora;

    /** Cria uma movimentação nova: id da sequência, preço atual do produto e data/hora de agora. */
    public MovimentacaoEstoque(Produto produto, TipoMovimentacao tipo, int quantidade) {
        this(SEQUENCIA.getAndIncrement(), produto, tipo, quantidade, produto.getPreco(), LocalDateTime.now());
    }

    private MovimentacaoEstoque(Long id, Produto produto, TipoMovimentacao tipo, int quantidade,
                                BigDecimal precoUnitario, LocalDateTime dataHora) {
        this.id = id;
        this.produto = produto;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.dataHora = dataHora;
    }

    /** Reconstrói uma movimentação que já foi gravada (usado ao ler do banco de dados). */
    public static MovimentacaoEstoque restaurar(Long id, Produto produto, TipoMovimentacao tipo, int quantidade,
                                                BigDecimal precoUnitario, LocalDateTime dataHora) {
        return new MovimentacaoEstoque(id, produto, tipo, quantidade, precoUnitario, dataHora);
    }

    /**
     * Garante que os próximos ids sejam maiores que {@code ultimoIdUsado}.
     * Necessário ao abrir um banco que já tem histórico gravado.
     */
    public static void sincronizarSequencia(long ultimoIdUsado) {
        SEQUENCIA.accumulateAndGet(ultimoIdUsado + 1, Math::max);
    }

    public Long getId() {
        return id;
    }

    public Produto getProduto() {
        return produto;
    }

    public TipoMovimentacao getTipo() {
        return tipo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public BigDecimal getValorTotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    @Override
    public String toString() {
        return String.format(Locale.of("pt", "BR"), "#%d | %s | %s | %s x%d | R$ %.2f",
                id, dataHora.format(FORMATO), tipo, produto.getNome(), quantidade, getValorTotal());
    }
}
