package com.vitor.estoque.repository.jdbc;

import com.vitor.estoque.exception.ErroDeBancoException;
import com.vitor.estoque.model.MovimentacaoEstoque;
import com.vitor.estoque.model.TipoMovimentacao;
import com.vitor.estoque.repository.Repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Movimentações são um histórico: só se insere. Chamar salvar() com um id que já
 * existe é um erro (não faz sentido reescrever o passado).
 */
public class MovimentacaoRepositorioJdbc implements Repositorio<MovimentacaoEstoque, Long> {

    private static final String SELECT = """
            SELECT m.id, m.tipo, m.quantidade, m.preco_unitario_centavos, m.data_hora,
                   p.codigo, p.nome, p.preco_centavos, p.quantidade_estoque, p.quantidade_minima, p.categoria,
                   f.cnpj AS fornecedor_cnpj, f.nome AS fornecedor_nome, f.telefone AS fornecedor_telefone
            FROM movimentacao m
            JOIN produto p ON p.codigo = m.produto_codigo
            JOIN fornecedor f ON f.cnpj = p.fornecedor_cnpj
            """;

    private final BancoDeDados banco;

    public MovimentacaoRepositorioJdbc(BancoDeDados banco) {
        this.banco = banco;
        sincronizarSequenciaDeIds();
    }

    @Override
    public void salvar(MovimentacaoEstoque movimentacao) {
        String sql = """
                INSERT INTO movimentacao (id, produto_codigo, tipo, quantidade, preco_unitario_centavos, data_hora)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement comando = banco.conexao().prepareStatement(sql)) {
            comando.setLong(1, movimentacao.getId());
            comando.setString(2, movimentacao.getProduto().getCodigo());
            comando.setString(3, movimentacao.getTipo().name());
            comando.setInt(4, movimentacao.getQuantidade());
            comando.setLong(5, Dinheiro.paraCentavos(movimentacao.getPrecoUnitario()));
            comando.setString(6, movimentacao.getDataHora().toString());
            comando.executeUpdate();
        } catch (SQLException e) {
            throw new ErroDeBancoException("Não foi possível registrar a movimentação", e);
        }
    }

    @Override
    public Optional<MovimentacaoEstoque> buscarPorId(Long id) {
        try (PreparedStatement comando = banco.conexao().prepareStatement(SELECT + " WHERE m.id = ?")) {
            comando.setLong(1, id);
            try (ResultSet resultado = comando.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ErroDeBancoException("Não foi possível buscar a movimentação", e);
        }
    }

    @Override
    public List<MovimentacaoEstoque> listarTodos() {
        try (PreparedStatement comando = banco.conexao().prepareStatement(SELECT + " ORDER BY m.id");
             ResultSet resultado = comando.executeQuery()) {
            List<MovimentacaoEstoque> lista = new ArrayList<>();
            while (resultado.next()) {
                lista.add(mapear(resultado));
            }
            return lista;
        } catch (SQLException e) {
            throw new ErroDeBancoException("Não foi possível listar as movimentações", e);
        }
    }

    @Override
    public boolean remover(Long id) {
        try (PreparedStatement comando = banco.conexao().prepareStatement("DELETE FROM movimentacao WHERE id = ?")) {
            comando.setLong(1, id);
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ErroDeBancoException("Não foi possível remover a movimentação", e);
        }
    }

    private static MovimentacaoEstoque mapear(ResultSet resultado) throws SQLException {
        return MovimentacaoEstoque.restaurar(
                resultado.getLong("id"),
                ProdutoRepositorioJdbc.mapear(resultado),
                TipoMovimentacao.valueOf(resultado.getString("tipo")),
                resultado.getInt("quantidade"),
                Dinheiro.deCentavos(resultado.getLong("preco_unitario_centavos")),
                LocalDateTime.parse(resultado.getString("data_hora")));
    }

    /**
     * Os ids das movimentações são gerados pelo programa. Ao abrir um banco que já tem
     * histórico, a contagem precisa continuar do último id gravado, senão o primeiro
     * registro novo repetiria um id antigo.
     */
    private void sincronizarSequenciaDeIds() {
        try (Statement comando = banco.conexao().createStatement();
             ResultSet resultado = comando.executeQuery("SELECT COALESCE(MAX(id), 0) FROM movimentacao")) {
            resultado.next();
            MovimentacaoEstoque.sincronizarSequencia(resultado.getLong(1));
        } catch (SQLException e) {
            throw new ErroDeBancoException("Não foi possível ler o histórico de movimentações", e);
        }
    }
}
