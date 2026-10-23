package com.vitor.estoque.service;

import com.vitor.estoque.exception.EstoqueException;
import com.vitor.estoque.exception.EstoqueInsuficienteException;
import com.vitor.estoque.exception.ProdutoNaoEncontradoException;
import com.vitor.estoque.model.Categoria;
import com.vitor.estoque.model.Fornecedor;
import com.vitor.estoque.model.MovimentacaoEstoque;
import com.vitor.estoque.model.Produto;
import com.vitor.estoque.repository.RepositorioEmMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstoqueServiceTest {

    private EstoqueService service;
    private Fornecedor fornecedorA;
    private Fornecedor fornecedorB;

    @BeforeEach
    void setUp() {
        service = new EstoqueService(
                new RepositorioEmMemoria<>(Produto::getCodigo),
                new RepositorioEmMemoria<>(Fornecedor::getCnpj),
                new RepositorioEmMemoria<>(MovimentacaoEstoque::getId));

        fornecedorA = new Fornecedor("A", "Fornecedor A", "");
        fornecedorB = new Fornecedor("B", "Fornecedor B", "");
        service.cadastrarFornecedor(fornecedorA);
        service.cadastrarFornecedor(fornecedorB);

        service.cadastrarProduto(new Produto("E1", "Fone", new BigDecimal("50.00"), 2, Categoria.ELETRONICOS, fornecedorA));
        service.cadastrarProduto(new Produto("A1", "Arroz", new BigDecimal("10.00"), 5, Categoria.ALIMENTOS, fornecedorB));
    }

    @Test
    void naoPermiteProdutoDuplicado() {
        assertThrows(EstoqueException.class, () -> service.cadastrarProduto(
                new Produto("E1", "Outro", BigDecimal.ONE, 1, Categoria.OUTROS, fornecedorA)));
    }

    @Test
    void produtoInexistenteLancaExcecao() {
        assertThrows(ProdutoNaoEncontradoException.class, () -> service.buscarProduto("XXX"));
        assertThrows(ProdutoNaoEncontradoException.class, () -> service.registrarEntrada("XXX", 1));
    }

    @Test
    void entradaESaidaAtualizamEstoqueEHistorico() {
        service.registrarEntrada("E1", 10);
        service.registrarSaida("E1", 3);

        assertEquals(7, service.buscarProduto("E1").getQuantidadeEmEstoque());
        assertEquals(2, service.listarMovimentacoes().size());
    }

    @Test
    void saidaSemEstoqueNaoRegistraMovimentacao() {
        service.registrarEntrada("E1", 2);
        assertThrows(EstoqueInsuficienteException.class, () -> service.registrarSaida("E1", 5));

        assertEquals(2, service.buscarProduto("E1").getQuantidadeEmEstoque());
        assertEquals(1, service.listarMovimentacoes().size());
    }

    @Test
    void filtraPorCategoriaEFornecedor() {
        assertEquals(1, service.listarPorCategoria(Categoria.ELETRONICOS).size());
        assertEquals("Arroz", service.listarPorFornecedor("B").get(0).getNome());
    }

    @Test
    void alertaDeEstoqueBaixo() {
        service.registrarEntrada("E1", 10); // acima do mínimo (2)
        // "A1" continua com 0 (<= mínimo 5) e deve aparecer no alerta
        List<Produto> baixos = service.produtosComEstoqueBaixo();
        assertEquals(1, baixos.size());
        assertEquals("A1", baixos.get(0).getCodigo());
    }

    @Test
    void valorTotalEmEstoque() {
        service.registrarEntrada("E1", 2);  // 2 x 50 = 100
        service.registrarEntrada("A1", 5);  // 5 x 10 = 50
        assertEquals(new BigDecimal("150.00"), service.valorTotalEmEstoque());
    }

    @Test
    void produtosMaisVendidosEmOrdemDecrescente() {
        service.registrarEntrada("E1", 20);
        service.registrarEntrada("A1", 20);
        service.registrarSaida("E1", 2);
        service.registrarSaida("A1", 7);
        service.registrarSaida("A1", 1);

        List<EstoqueService.VendaPorProduto> ranking = service.produtosMaisVendidos(5);
        assertEquals("A1", ranking.get(0).produto().getCodigo());
        assertEquals(8, ranking.get(0).quantidade());
        assertEquals("E1", ranking.get(1).produto().getCodigo());
    }

    @Test
    void atualizarPrecoNaoAlteraPrecoDoHistorico() {
        service.registrarEntrada("E1", 5);
        BigDecimal precoAntigo = service.listarMovimentacoes().get(0).getPrecoUnitario();

        service.atualizarPreco("E1", new BigDecimal("99.00"));

        assertEquals(precoAntigo, service.listarMovimentacoes().get(0).getPrecoUnitario());
        assertEquals(new BigDecimal("99.00"), service.buscarProduto("E1").getPreco());
    }
}
