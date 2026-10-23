package com.vitor.estoque.repository.jdbc;

import com.vitor.estoque.exception.ErroDeBancoException;
import com.vitor.estoque.model.Categoria;
import com.vitor.estoque.model.Fornecedor;
import com.vitor.estoque.model.Produto;
import com.vitor.estoque.repository.Repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProdutoRepositorioJdbc implements Repositorio<Produto, String> {

    /** Produto junto com os dados do fornecedor (um JOIN, em vez de uma consulta por produto). */
    static final String SELECT_COM_FORNECEDOR = """
            SELECT p.codigo, p.nome, p.preco_centavos, p.quantidade_estoque, p.quantidade_minima, p.categoria,
                   f.cnpj AS fornecedor_cnpj, f.nome AS fornecedor_nome, f.telefone AS fornecedor_telefone
            FROM produto p
            JOIN fornecedor f ON f.cnpj = p.fornecedor_cnpj
            """;

    private final BancoDeDados banco;

    public ProdutoRepositorioJdbc(BancoDeDados banco) {
        this.banco = banco;
    }

    /** Insere o produto; se o código já existir, atualiza todos os campos. */
    @Override
    public void salvar(Produto produto) {
        String sql = """
                INSERT INTO produto (codigo, nome, preco_centavos, quantidade_estoque, quantidade_minima, categoria, fornecedor_cnpj)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(codigo) DO UPDATE SET
                    nome = excluded.nome,
                    preco_centavos = excluded.preco_centavos,
                    quantidade_estoque = excluded.quantidade_estoque,
                    quantidade_minima = excluded.quantidade_minima,
                    categoria = excluded.categoria,
                    fornecedor_cnpj = excluded.fornecedor_cnpj
                """;
        try (PreparedStatement comando = banco.conexao().prepareStatement(sql)) {
            comando.setString(1, produto.getCodigo());
            comando.setString(2, produto.getNome());
            comando.setLong(3, Dinheiro.paraCentavos(produto.getPreco()));
            comando.setInt(4, produto.getQuantidadeEmEstoque());
            comando.setInt(5, produto.getQuantidadeMinima());
            comando.setString(6, produto.getCategoria().name());
            comando.setString(7, produto.getFornecedor().getCnpj());
            comando.executeUpdate();
        } catch (SQLException e) {
            throw new ErroDeBancoException("Não foi possível salvar o produto", e);
        }
    }

    @Override
    public Optional<Produto> buscarPorId(String codigo) {
        try (PreparedStatement comando = banco.conexao().prepareStatement(SELECT_COM_FORNECEDOR + " WHERE p.codigo = ?")) {
            comando.setString(1, codigo);
            try (ResultSet resultado = comando.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ErroDeBancoException("Não foi possível buscar o produto", e);
        }
    }

    @Override
    public List<Produto> listarTodos() {
        try (PreparedStatement comando = banco.conexao().prepareStatement(SELECT_COM_FORNECEDOR + " ORDER BY p.nome COLLATE NOCASE");
             ResultSet resultado = comando.executeQuery()) {
            List<Produto> lista = new ArrayList<>();
            while (resultado.next()) {
                lista.add(mapear(resultado));
            }
            return lista;
        } catch (SQLException e) {
            throw new ErroDeBancoException("Não foi possível listar os produtos", e);
        }
    }

    @Override
    public boolean remover(String codigo) {
        try (PreparedStatement comando = banco.conexao().prepareStatement("DELETE FROM produto WHERE codigo = ?")) {
            comando.setString(1, codigo);
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ErroDeBancoException("Não foi possível remover o produto (ele pode ter movimentações registradas)", e);
        }
    }

    /** Monta um Produto a partir de uma linha da consulta SELECT_COM_FORNECEDOR. */
    static Produto mapear(ResultSet resultado) throws SQLException {
        Fornecedor fornecedor = new Fornecedor(
                resultado.getString("fornecedor_cnpj"),
                resultado.getString("fornecedor_nome"),
                resultado.getString("fornecedor_telefone"));

        Produto produto = new Produto(
                resultado.getString("codigo"),
                resultado.getString("nome"),
                Dinheiro.deCentavos(resultado.getLong("preco_centavos")),
                resultado.getInt("quantidade_minima"),
                Categoria.valueOf(resultado.getString("categoria")),
                fornecedor);

        // O construtor sempre começa com estoque 0; aqui devolvemos a quantidade que estava gravada.
        int emEstoque = resultado.getInt("quantidade_estoque");
        if (emEstoque > 0) {
            produto.adicionarEstoque(emEstoque);
        }
        return produto;
    }
}
