package com.vitor.estoque.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovimentacaoEstoqueTest {

    private final Produto produto = new Produto("P1", "Caneta", new BigDecimal("2.50"), 1,
            Categoria.PAPELARIA, new Fornecedor("123", "Fornecedor", ""));

    @Test
    void novaMovimentacaoGuardaOPrecoDaEpoca() {
        MovimentacaoEstoque m = new MovimentacaoEstoque(produto, TipoMovimentacao.ENTRADA, 4);
        produto.setPreco(new BigDecimal("9.99"));

        assertEquals(new BigDecimal("2.50"), m.getPrecoUnitario());
        assertEquals(new BigDecimal("10.00"), m.getValorTotal());
    }

    @Test
    void restaurarReconstroiExatamenteOQueFoiGravado() {
        LocalDateTime dataHora = LocalDateTime.of(2026, 3, 15, 10, 30, 5);
        MovimentacaoEstoque m = MovimentacaoEstoque.restaurar(
                42L, produto, TipoMovimentacao.SAIDA, 3, new BigDecimal("7.00"), dataHora);

        assertEquals(42L, m.getId());
        assertEquals(TipoMovimentacao.SAIDA, m.getTipo());
        assertEquals(3, m.getQuantidade());
        assertEquals(new BigDecimal("21.00"), m.getValorTotal());
        assertEquals(dataHora, m.getDataHora());
    }

    @Test
    void sincronizarSequenciaFazOsProximosIdsPassarDoUltimoUsado() {
        MovimentacaoEstoque.sincronizarSequencia(100_000);
        MovimentacaoEstoque m = new MovimentacaoEstoque(produto, TipoMovimentacao.ENTRADA, 1);
        assertTrue(m.getId() > 100_000);

        // Sincronizar com um valor menor nunca faz a contagem voltar para trás.
        MovimentacaoEstoque.sincronizarSequencia(5);
        MovimentacaoEstoque seguinte = new MovimentacaoEstoque(produto, TipoMovimentacao.ENTRADA, 1);
        assertTrue(seguinte.getId() > m.getId());
    }
}
