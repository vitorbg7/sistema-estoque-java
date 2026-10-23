package com.vitor.estoque.model;

import com.vitor.estoque.exception.EstoqueInsuficienteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProdutoTest {

    private Produto produto;

    @BeforeEach
    void setUp() {
        Fornecedor fornecedor = new Fornecedor("123", "Fornecedor Teste", "");
        produto = new Produto("P1", "Caneta", new BigDecimal("2.50"), 5, Categoria.PAPELARIA, fornecedor);
    }

    @Test
    void novoProdutoComecaSemEstoque() {
        assertEquals(0, produto.getQuantidadeEmEstoque());
    }

    @Test
    void entradaAumentaEstoque() {
        produto.adicionarEstoque(10);
        assertEquals(10, produto.getQuantidadeEmEstoque());
    }

    @Test
    void saidaDiminuiEstoque() {
        produto.adicionarEstoque(10);
        produto.removerEstoque(4);
        assertEquals(6, produto.getQuantidadeEmEstoque());
    }

    @Test
    void saidaMaiorQueEstoqueLancaExcecao() {
        produto.adicionarEstoque(3);
        EstoqueInsuficienteException e =
                assertThrows(EstoqueInsuficienteException.class, () -> produto.removerEstoque(5));
        assertEquals(3, e.getDisponivel());
        assertEquals(5, e.getSolicitado());
        assertEquals(3, produto.getQuantidadeEmEstoque(), "estoque não pode mudar após falha");
    }

    @Test
    void quantidadeInvalidaEhRejeitada() {
        assertThrows(IllegalArgumentException.class, () -> produto.adicionarEstoque(0));
        assertThrows(IllegalArgumentException.class, () -> produto.removerEstoque(-1));
    }

    @Test
    void precoNegativoEhRejeitado() {
        assertThrows(IllegalArgumentException.class, () -> produto.setPreco(new BigDecimal("-1")));
    }

    @Test
    void estoqueBaixoQuandoAbaixoOuIgualAoMinimo() {
        produto.adicionarEstoque(6);
        assertFalse(produto.estoqueBaixo());
        produto.removerEstoque(1); // 5 == mínimo
        assertTrue(produto.estoqueBaixo());
    }

    @Test
    void valorEmEstoqueEhPrecoVezesQuantidade() {
        produto.adicionarEstoque(4);
        assertEquals(new BigDecimal("10.00"), produto.valorEmEstoque());
    }

    @Test
    void precoComMaisDeDuasCasasDecimaisEhRejeitado() {
        assertThrows(IllegalArgumentException.class, () -> produto.setPreco(new BigDecimal("1.999")));
        produto.setPreco(new BigDecimal("1.990")); // zeros à direita não contam: continua sendo R$ 1,99
        produto.setPreco(new BigDecimal("1E+2"));  // 100
    }

    @Test
    void precoAcimaDoMaximoEhRejeitado() {
        assertThrows(IllegalArgumentException.class, () -> produto.setPreco(new BigDecimal("1000000000.01")));
        produto.setPreco(new BigDecimal("1000000000.00"));
    }
}
