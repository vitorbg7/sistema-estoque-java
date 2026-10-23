package com.vitor.estoque.service;

import com.vitor.estoque.exception.EstoqueException;
import com.vitor.estoque.exception.ProdutoNaoEncontradoException;
import com.vitor.estoque.model.Categoria;
import com.vitor.estoque.model.Fornecedor;
import com.vitor.estoque.model.MovimentacaoEstoque;
import com.vitor.estoque.model.Produto;
import com.vitor.estoque.model.TipoMovimentacao;
import com.vitor.estoque.repository.GerenciadorDeTransacao;
import com.vitor.estoque.repository.Repositorio;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Regras de negócio do estoque: cadastros, entradas, saídas e relatórios.
 * Não sabe onde os dados ficam guardados (memória ou banco): só usa as interfaces.
 */
public class EstoqueService {

    /** Total de unidades vendidas de um produto (usado no relatório). */
    public record VendaPorProduto(Produto produto, int quantidade) {
    }

    private final Repositorio<Produto, String> produtos;
    private final Repositorio<Fornecedor, String> fornecedores;
    private final Repositorio<MovimentacaoEstoque, Long> movimentacoes;
    private final GerenciadorDeTransacao transacao;

    /** Sem banco de dados: não há transação a controlar. */
    public EstoqueService(Repositorio<Produto, String> produtos,
                          Repositorio<Fornecedor, String> fornecedores,
                          Repositorio<MovimentacaoEstoque, Long> movimentacoes) {
        this(produtos, fornecedores, movimentacoes, GerenciadorDeTransacao.semTransacao());
    }

    public EstoqueService(Repositorio<Produto, String> produtos,
                          Repositorio<Fornecedor, String> fornecedores,
                          Repositorio<MovimentacaoEstoque, Long> movimentacoes,
                          GerenciadorDeTransacao transacao) {
        this.produtos = produtos;
        this.fornecedores = fornecedores;
        this.movimentacoes = movimentacoes;
        this.transacao = transacao;
    }

    // ---------- Fornecedores ----------

    public void cadastrarFornecedor(Fornecedor fornecedor) {
        transacao.executarSemRetorno(() -> {
            if (fornecedores.existe(fornecedor.getCnpj())) {
                throw new EstoqueException("Já existe um fornecedor com o CNPJ " + fornecedor.getCnpj() + ".");
            }
            fornecedores.salvar(fornecedor);
        });
    }

    public Fornecedor buscarFornecedor(String cnpj) {
        return fornecedores.buscarPorId(cnpj.trim())
                .orElseThrow(() -> new EstoqueException("Fornecedor com CNPJ '" + cnpj + "' não encontrado."));
    }

    public List<Fornecedor> listarFornecedores() {
        return fornecedores.listarTodos().stream()
                .sorted(Comparator.comparing(Fornecedor::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    // ---------- Produtos ----------

    public void cadastrarProduto(Produto produto) {
        transacao.executarSemRetorno(() -> {
            if (produtos.existe(produto.getCodigo())) {
                throw new EstoqueException("Já existe um produto com o código " + produto.getCodigo() + ".");
            }
            if (!fornecedores.existe(produto.getFornecedor().getCnpj())) {
                throw new EstoqueException("O fornecedor " + produto.getFornecedor().getNome()
                        + " não está cadastrado. Cadastre-o antes do produto.");
            }
            produtos.salvar(produto);
        });
    }

    public Produto buscarProduto(String codigo) {
        return produtos.buscarPorId(codigo.trim())
                .orElseThrow(() -> new ProdutoNaoEncontradoException(codigo));
    }

    /** Produto que já teve entrada ou saída não pode ser removido: o histórico precisa dele. */
    public void removerProduto(String codigo) {
        transacao.executarSemRetorno(() -> {
            Produto produto = buscarProduto(codigo);
            boolean temHistorico = movimentacoes.listarTodos().stream()
                    .anyMatch(m -> m.getProduto().equals(produto));
            if (temHistorico) {
                throw new EstoqueException("O produto '" + produto.getNome()
                        + "' tem movimentações registradas e não pode ser removido.");
            }
            produtos.remover(produto.getCodigo());
        });
    }

    public void atualizarPreco(String codigo, BigDecimal novoPreco) {
        Produto produto = buscarProduto(codigo);
        produto.setPreco(novoPreco);
        produtos.salvar(produto);
    }

    public List<Produto> listarProdutos() {
        return produtos.listarTodos().stream()
                .sorted(Comparator.comparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<Produto> listarPorCategoria(Categoria categoria) {
        return listarProdutos().stream()
                .filter(p -> p.getCategoria() == categoria)
                .toList();
    }

    public List<Produto> listarPorFornecedor(String cnpj) {
        Fornecedor fornecedor = buscarFornecedor(cnpj);
        return listarProdutos().stream()
                .filter(p -> p.getFornecedor().equals(fornecedor))
                .toList();
    }

    // ---------- Movimentações ----------

    // Cada movimentação altera o produto E grava o histórico. Dentro da transação,
    // se uma das duas gravações falhar, a outra é desfeita.

    public MovimentacaoEstoque registrarEntrada(String codigo, int quantidade) {
        return transacao.executar(() -> {
            Produto produto = buscarProduto(codigo);
            produto.adicionarEstoque(quantidade);
            produtos.salvar(produto);
            return registrar(produto, TipoMovimentacao.ENTRADA, quantidade);
        });
    }

    public MovimentacaoEstoque registrarSaida(String codigo, int quantidade) {
        return transacao.executar(() -> {
            Produto produto = buscarProduto(codigo);
            produto.removerEstoque(quantidade); // lança EstoqueInsuficienteException se não houver
            produtos.salvar(produto);
            return registrar(produto, TipoMovimentacao.SAIDA, quantidade);
        });
    }

    private MovimentacaoEstoque registrar(Produto produto, TipoMovimentacao tipo, int quantidade) {
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque(produto, tipo, quantidade);
        movimentacoes.salvar(movimentacao);
        return movimentacao;
    }

    public List<MovimentacaoEstoque> listarMovimentacoes() {
        return movimentacoes.listarTodos();
    }

    // ---------- Relatórios ----------

    public List<Produto> produtosComEstoqueBaixo() {
        return listarProdutos().stream()
                .filter(Produto::estoqueBaixo)
                .toList();
    }

    public BigDecimal valorTotalEmEstoque() {
        return produtos.listarTodos().stream()
                .map(Produto::valorEmEstoque)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<VendaPorProduto> produtosMaisVendidos(int limite) {
        Map<Produto, Integer> totais = new HashMap<>();
        for (MovimentacaoEstoque m : movimentacoes.listarTodos()) {
            if (m.getTipo() == TipoMovimentacao.SAIDA) {
                totais.merge(m.getProduto(), m.getQuantidade(), Integer::sum);
            }
        }
        return totais.entrySet().stream()
                .map(e -> new VendaPorProduto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(VendaPorProduto::quantidade).reversed())
                .limit(limite)
                .toList();
    }
}
