package com.vitor.estoque.repository.jdbc;

import com.vitor.estoque.exception.ErroDeBancoException;
import com.vitor.estoque.model.Categoria;
import com.vitor.estoque.model.Fornecedor;
import com.vitor.estoque.model.MovimentacaoEstoque;
import com.vitor.estoque.model.Produto;
import com.vitor.estoque.model.TipoMovimentacao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testa cada repositório JDBC isoladamente, contra um banco SQLite temporário em memória. */
class RepositoriosJdbcTest {

    private BancoDeDados banco;
    private FornecedorRepositorioJdbc fornecedores;
    private ProdutoRepositorioJdbc produtos;
    private MovimentacaoRepositorioJdbc movimentacoes;
    private Fornecedor fornecedor;

    @BeforeEach
    void setUp() {
        banco = BancoDeDados.emMemoria();
        fornecedores = new FornecedorRepositorioJdbc(banco);
        produtos = new ProdutoRepositorioJdbc(banco);
        movimentacoes = new MovimentacaoRepositorioJdbc(banco);

        fornecedor = new Fornecedor("11.111.111/0001-11", "TechDistrib", "(41) 3333-1111");
        fornecedores.salvar(fornecedor);
    }

    @AfterEach
    void tearDown() {
        banco.close();
    }

    private Produto novoProduto(String codigo, String nome, String preco) {
        return new Produto(codigo, nome, new BigDecimal(preco), 3, Categoria.ELETRONICOS, fornecedor);
    }

    // ---------- Fornecedor ----------

    @Test
    void fornecedorGravadoPodeSerLido() {
        Fornecedor lido = fornecedores.buscarPorId("11.111.111/0001-11").orElseThrow();

        assertEquals("TechDistrib", lido.getNome());
        assertEquals("(41) 3333-1111", lido.getTelefone());
        assertTrue(fornecedores.buscarPorId("999").isEmpty());
    }

    @Test
    void salvarFornecedorExistenteAtualizaOsDados() {
        Fornecedor alterado = new Fornecedor("11.111.111/0001-11", "TechDistrib Brasil", "");
        fornecedores.salvar(alterado);

        List<Fornecedor> todos = fornecedores.listarTodos();
        assertEquals(1, todos.size());
        assertEquals("TechDistrib Brasil", todos.get(0).getNome());
        assertEquals("", todos.get(0).getTelefone());
    }

    @Test
    void removerFornecedorInformaSeExistia() {
        assertTrue(fornecedores.remover("11.111.111/0001-11"));
        assertFalse(fornecedores.remover("11.111.111/0001-11"));
        assertTrue(fornecedores.listarTodos().isEmpty());
    }

    // ---------- Produto ----------

    @Test
    void produtoVoltaDoBancoComTodosOsCampos() {
        Produto produto = new Produto("TEC001", "Mouse Gamer", new BigDecimal("129.90"), 5,
                Categoria.ELETRONICOS, fornecedor);
        produto.adicionarEstoque(7);
        produtos.salvar(produto);

        Produto lido = produtos.buscarPorId("TEC001").orElseThrow();

        assertEquals("Mouse Gamer", lido.getNome());
        assertEquals(new BigDecimal("129.90"), lido.getPreco());
        assertEquals(7, lido.getQuantidadeEmEstoque());
        assertEquals(5, lido.getQuantidadeMinima());
        assertEquals(Categoria.ELETRONICOS, lido.getCategoria());
        assertEquals("TechDistrib", lido.getFornecedor().getNome());
        assertEquals("(41) 3333-1111", lido.getFornecedor().getTelefone());
    }

    @Test
    void salvarProdutoExistenteAtualizaEmVezDeDuplicar() {
        Produto produto = novoProduto("P1", "Fone", "50.00");
        produtos.salvar(produto);

        produto.setPreco(new BigDecimal("55.50"));
        produto.adicionarEstoque(4);
        produtos.salvar(produto);

        assertEquals(1, produtos.listarTodos().size());
        Produto lido = produtos.buscarPorId("P1").orElseThrow();
        assertEquals(new BigDecimal("55.50"), lido.getPreco());
        assertEquals(4, lido.getQuantidadeEmEstoque());
    }

    @Test
    void listaProdutosEmOrdemAlfabeticaSemDiferenciarMaiusculas() {
        produtos.salvar(novoProduto("P1", "teclado", "10.00"));
        produtos.salvar(novoProduto("P2", "Mouse", "10.00"));
        produtos.salvar(novoProduto("P3", "Cabo", "10.00"));

        List<String> nomes = produtos.listarTodos().stream().map(Produto::getNome).toList();
        assertEquals(List.of("Cabo", "Mouse", "teclado"), nomes);
    }

    @Test
    void removerProdutoInformaSeExistia() {
        produtos.salvar(novoProduto("P1", "Fone", "50.00"));

        assertTrue(produtos.remover("P1"));
        assertFalse(produtos.remover("P1"));
        assertTrue(produtos.buscarPorId("P1").isEmpty());
    }

    @Test
    void bancoRecusaProdutoDeFornecedorQueNaoExiste() {
        Fornecedor fantasma = new Fornecedor("000", "Fantasma", "");
        Produto produto = new Produto("P1", "Fone", new BigDecimal("50.00"), 1, Categoria.OUTROS, fantasma);

        assertThrows(ErroDeBancoException.class, () -> produtos.salvar(produto));
    }

    @Test
    void textoComAspasESqlNaoQuebraNemApagaTabelas() {
        String nomeMalicioso = "Robert'); DROP TABLE produto;--";
        produtos.salvar(novoProduto("P1", nomeMalicioso, "1.00"));

        assertEquals(nomeMalicioso, produtos.buscarPorId("P1").orElseThrow().getNome());
        assertEquals(1, produtos.listarTodos().size()); // a tabela continua existindo
        assertTrue(produtos.buscarPorId("P1' OR '1'='1").isEmpty());
    }

    // ---------- Movimentação ----------

    @Test
    void movimentacaoGuardaPrecoDaEpocaEDataHora() {
        Produto produto = novoProduto("P1", "Fone", "10.00");
        produtos.salvar(produto);
        MovimentacaoEstoque original = new MovimentacaoEstoque(produto, TipoMovimentacao.SAIDA, 2);
        movimentacoes.salvar(original);

        produto.setPreco(new BigDecimal("99.00"));
        produtos.salvar(produto);

        MovimentacaoEstoque lida = movimentacoes.buscarPorId(original.getId()).orElseThrow();
        assertEquals(new BigDecimal("10.00"), lida.getPrecoUnitario());
        assertEquals(TipoMovimentacao.SAIDA, lida.getTipo());
        assertEquals(2, lida.getQuantidade());
        assertEquals(original.getDataHora(), lida.getDataHora());
        assertEquals("P1", lida.getProduto().getCodigo());
        assertEquals(new BigDecimal("99.00"), lida.getProduto().getPreco()); // o produto é o atual
    }

    @Test
    void listaMovimentacoesNaOrdemEmQueForamGravadas() {
        Produto produto = novoProduto("P1", "Fone", "10.00");
        produtos.salvar(produto);
        MovimentacaoEstoque a = new MovimentacaoEstoque(produto, TipoMovimentacao.ENTRADA, 5);
        MovimentacaoEstoque b = new MovimentacaoEstoque(produto, TipoMovimentacao.SAIDA, 1);
        movimentacoes.salvar(a);
        movimentacoes.salvar(b);

        List<Long> ids = movimentacoes.listarTodos().stream().map(MovimentacaoEstoque::getId).toList();
        assertEquals(List.of(a.getId(), b.getId()), ids);
    }

    @Test
    void historicoNaoPodeSerReescrito() {
        Produto produto = novoProduto("P1", "Fone", "10.00");
        produtos.salvar(produto);
        MovimentacaoEstoque m = new MovimentacaoEstoque(produto, TipoMovimentacao.ENTRADA, 1);
        movimentacoes.salvar(m);

        assertThrows(ErroDeBancoException.class, () -> movimentacoes.salvar(m)); // mesmo id duas vezes
    }

    @Test
    void bancoNaoDeixaApagarProdutoQueTemMovimentacoes() {
        Produto produto = novoProduto("P1", "Fone", "10.00");
        produtos.salvar(produto);
        movimentacoes.salvar(new MovimentacaoEstoque(produto, TipoMovimentacao.ENTRADA, 1));

        assertThrows(ErroDeBancoException.class, () -> produtos.remover("P1"));
        assertTrue(produtos.buscarPorId("P1").isPresent());
    }

    @Test
    void repositorioNovoContinuaOsIdsDepoisDoUltimoGravado() {
        Produto produto = novoProduto("P1", "Fone", "10.00");
        produtos.salvar(produto);
        movimentacoes.salvar(MovimentacaoEstoque.restaurar(
                50_000L, produto, TipoMovimentacao.ENTRADA, 1, new BigDecimal("10.00"), LocalDateTime.now()));

        new MovimentacaoRepositorioJdbc(banco); // é o que acontece quando o programa abre um banco existente
        MovimentacaoEstoque nova = new MovimentacaoEstoque(produto, TipoMovimentacao.ENTRADA, 1);

        assertTrue(nova.getId() > 50_000L, "id novo repetiria um existente: " + nova.getId());
    }

    // ---------- Transação ----------

    @Test
    void erroDentroDaTransacaoDesfazTudoQueFoiGravado() {
        produtos.salvar(novoProduto("P1", "Fone", "10.00"));

        assertThrows(IllegalStateException.class, () -> banco.executarSemRetorno(() -> {
            produtos.salvar(novoProduto("P2", "Cabo", "5.00"));
            fornecedores.salvar(new Fornecedor("222", "Outro", ""));
            throw new IllegalStateException("falha no meio da operação");
        }));

        assertTrue(produtos.buscarPorId("P2").isEmpty());
        assertTrue(fornecedores.buscarPorId("222").isEmpty());
        assertTrue(produtos.buscarPorId("P1").isPresent(), "o que já existia antes continua");
    }

    @Test
    void transacaoConcluidaGravaTudo() {
        banco.executarSemRetorno(() -> {
            produtos.salvar(novoProduto("P2", "Cabo", "5.00"));
            fornecedores.salvar(new Fornecedor("222", "Outro", ""));
        });

        assertTrue(produtos.buscarPorId("P2").isPresent());
        assertTrue(fornecedores.buscarPorId("222").isPresent());
    }

    @Test
    void transacaoDentroDeTransacaoFazParteDaExterna() {
        assertThrows(IllegalStateException.class, () -> banco.executarSemRetorno(() -> {
            banco.executarSemRetorno(() -> produtos.salvar(novoProduto("P2", "Cabo", "5.00")));
            throw new IllegalStateException("falha depois da transação interna");
        }));

        assertTrue(produtos.buscarPorId("P2").isEmpty(), "a interna não pode gravar sozinha");
    }
}
