package com.vitor.estoque.service;

import com.vitor.estoque.exception.ErroDeBancoException;
import com.vitor.estoque.exception.EstoqueException;
import com.vitor.estoque.exception.EstoqueInsuficienteException;
import com.vitor.estoque.exception.ProdutoNaoEncontradoException;
import com.vitor.estoque.model.Categoria;
import com.vitor.estoque.model.Fornecedor;
import com.vitor.estoque.model.MovimentacaoEstoque;
import com.vitor.estoque.model.Produto;
import com.vitor.estoque.model.TipoMovimentacao;
import com.vitor.estoque.repository.jdbc.BancoDeDados;
import com.vitor.estoque.repository.jdbc.FornecedorRepositorioJdbc;
import com.vitor.estoque.repository.jdbc.MovimentacaoRepositorioJdbc;
import com.vitor.estoque.repository.jdbc.ProdutoRepositorioJdbc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testa o EstoqueService funcionando de verdade em cima do banco SQLite. */
class EstoqueServiceJdbcTest {

    @TempDir
    Path pasta;

    private static EstoqueService criarServico(BancoDeDados banco) {
        return new EstoqueService(
                new ProdutoRepositorioJdbc(banco),
                new FornecedorRepositorioJdbc(banco),
                new MovimentacaoRepositorioJdbc(banco),
                banco);
    }

    private static Fornecedor cadastrarBase(EstoqueService servico) {
        Fornecedor fornecedor = new Fornecedor("A", "Fornecedor A", "");
        servico.cadastrarFornecedor(fornecedor);
        servico.cadastrarProduto(new Produto("E1", "Fone", new BigDecimal("50.00"), 2, Categoria.ELETRONICOS, fornecedor));
        servico.cadastrarProduto(new Produto("A1", "Arroz", new BigDecimal("10.00"), 5, Categoria.ALIMENTOS, fornecedor));
        return fornecedor;
    }

    @Test
    void dadosContinuamLaDepoisDeFecharEAbrirOProgramaDeNovo() {
        Path arquivo = pasta.resolve("estoque.db");

        try (BancoDeDados banco = BancoDeDados.arquivo(arquivo)) {
            EstoqueService servico = criarServico(banco);
            cadastrarBase(servico);
            servico.registrarEntrada("E1", 10);
            servico.registrarSaida("E1", 3);
        }

        try (BancoDeDados banco = BancoDeDados.arquivo(arquivo)) {
            EstoqueService servico = criarServico(banco);

            assertEquals(7, servico.buscarProduto("E1").getQuantidadeEmEstoque());
            assertEquals(2, servico.listarProdutos().size());
            assertEquals(new BigDecimal("350.00"), servico.valorTotalEmEstoque());

            List<MovimentacaoEstoque> historico = servico.listarMovimentacoes();
            assertEquals(2, historico.size());
            assertEquals(TipoMovimentacao.ENTRADA, historico.get(0).getTipo());
            assertEquals(TipoMovimentacao.SAIDA, historico.get(1).getTipo());

            // E o histórico novo se junta ao antigo, sem repetir id.
            MovimentacaoEstoque nova = servico.registrarEntrada("A1", 1);
            assertEquals(3, servico.listarMovimentacoes().size());
            assertTrue(nova.getId() > historico.get(1).getId());
        }
    }

    @Test
    void entradaESaidaAtualizamEstoqueEHistorico() {
        try (BancoDeDados banco = BancoDeDados.emMemoria()) {
            EstoqueService servico = criarServico(banco);
            cadastrarBase(servico);

            servico.registrarEntrada("E1", 10);
            servico.registrarSaida("E1", 3);

            assertEquals(7, servico.buscarProduto("E1").getQuantidadeEmEstoque());
            assertEquals(2, servico.listarMovimentacoes().size());
        }
    }

    @Test
    void saidaSemEstoqueNaoAlteraNadaNoBanco() {
        try (BancoDeDados banco = BancoDeDados.emMemoria()) {
            EstoqueService servico = criarServico(banco);
            cadastrarBase(servico);
            servico.registrarEntrada("E1", 2);

            assertThrows(EstoqueInsuficienteException.class, () -> servico.registrarSaida("E1", 5));

            assertEquals(2, servico.buscarProduto("E1").getQuantidadeEmEstoque());
            assertEquals(1, servico.listarMovimentacoes().size());
        }
    }

    @Test
    void seOHistoricoFalharOEstoqueNaoMudaTambem() {
        try (BancoDeDados banco = BancoDeDados.emMemoria()) {
            // Repositório de histórico que sempre falha, para simular um erro no meio da operação.
            MovimentacaoRepositorioJdbc historicoComDefeito = new MovimentacaoRepositorioJdbc(banco) {
                @Override
                public void salvar(MovimentacaoEstoque movimentacao) {
                    throw new ErroDeBancoException("disco cheio (simulado)", null);
                }
            };
            EstoqueService servico = new EstoqueService(
                    new ProdutoRepositorioJdbc(banco), new FornecedorRepositorioJdbc(banco), historicoComDefeito, banco);
            cadastrarBase(servico);

            assertThrows(ErroDeBancoException.class, () -> servico.registrarEntrada("E1", 10));

            // Sem a transação, o estoque ficaria 10 sem nenhum registro no histórico.
            assertEquals(0, servico.buscarProduto("E1").getQuantidadeEmEstoque());
            assertTrue(servico.listarMovimentacoes().isEmpty());
        }
    }

    @Test
    void produtoComHistoricoNaoPodeSerRemovido() {
        try (BancoDeDados banco = BancoDeDados.emMemoria()) {
            EstoqueService servico = criarServico(banco);
            cadastrarBase(servico);
            servico.registrarEntrada("E1", 3);

            assertThrows(EstoqueException.class, () -> servico.removerProduto("E1"));
            assertEquals(3, servico.buscarProduto("E1").getQuantidadeEmEstoque());

            servico.removerProduto("A1"); // este nunca teve movimentação
            assertThrows(ProdutoNaoEncontradoException.class, () -> servico.buscarProduto("A1"));
        }
    }

    @Test
    void rejeitaDuplicadosEFornecedorInexistente() {
        try (BancoDeDados banco = BancoDeDados.emMemoria()) {
            EstoqueService servico = criarServico(banco);
            Fornecedor fornecedor = cadastrarBase(servico);

            assertThrows(EstoqueException.class, () -> servico.cadastrarFornecedor(fornecedor));
            assertThrows(EstoqueException.class, () -> servico.cadastrarProduto(
                    new Produto("E1", "Outro", BigDecimal.ONE, 1, Categoria.OUTROS, fornecedor)));
            assertThrows(EstoqueException.class, () -> servico.cadastrarProduto(
                    new Produto("Z1", "Caneta", BigDecimal.ONE, 1, Categoria.OUTROS, new Fornecedor("Z", "Z", ""))));
        }
    }

    @Test
    void relatoriosFuncionamComDadosDoBanco() {
        try (BancoDeDados banco = BancoDeDados.emMemoria()) {
            EstoqueService servico = criarServico(banco);
            cadastrarBase(servico);
            servico.registrarEntrada("E1", 20);
            servico.registrarEntrada("A1", 20);
            servico.registrarSaida("E1", 2);
            servico.registrarSaida("A1", 7);
            servico.registrarSaida("A1", 1);

            List<EstoqueService.VendaPorProduto> ranking = servico.produtosMaisVendidos(5);
            assertEquals("A1", ranking.get(0).produto().getCodigo());
            assertEquals(8, ranking.get(0).quantidade());
            assertEquals("E1", ranking.get(1).produto().getCodigo());

            assertEquals(1, servico.listarPorCategoria(Categoria.ELETRONICOS).size());
            assertEquals("Arroz", servico.listarPorFornecedor("A").get(0).getNome());
            assertEquals(new BigDecimal("1020.00"), servico.valorTotalEmEstoque()); // 18 x 50 + 12 x 10
        }
    }
}
