package com.vitor.estoque.repository.jdbc;

import com.vitor.estoque.exception.ErroDeBancoException;
import com.vitor.estoque.model.Fornecedor;
import com.vitor.estoque.repository.Repositorio;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FornecedorRepositorioJdbc implements Repositorio<Fornecedor, String> {

    private final BancoDeDados banco;

    public FornecedorRepositorioJdbc(BancoDeDados banco) {
        this.banco = banco;
    }

    /** Insere o fornecedor; se o CNPJ já existir, atualiza nome e telefone. */
    @Override
    public void salvar(Fornecedor fornecedor) {
        String sql = """
                INSERT INTO fornecedor (cnpj, nome, telefone) VALUES (?, ?, ?)
                ON CONFLICT(cnpj) DO UPDATE SET nome = excluded.nome, telefone = excluded.telefone
                """;
        try (PreparedStatement comando = banco.conexao().prepareStatement(sql)) {
            comando.setString(1, fornecedor.getCnpj());
            comando.setString(2, fornecedor.getNome());
            comando.setString(3, fornecedor.getTelefone());
            comando.executeUpdate();
        } catch (SQLException e) {
            throw new ErroDeBancoException("Não foi possível salvar o fornecedor", e);
        }
    }

    @Override
    public Optional<Fornecedor> buscarPorId(String cnpj) {
        String sql = "SELECT cnpj, nome, telefone FROM fornecedor WHERE cnpj = ?";
        try (PreparedStatement comando = banco.conexao().prepareStatement(sql)) {
            comando.setString(1, cnpj);
            try (ResultSet resultado = comando.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ErroDeBancoException("Não foi possível buscar o fornecedor", e);
        }
    }

    @Override
    public List<Fornecedor> listarTodos() {
        String sql = "SELECT cnpj, nome, telefone FROM fornecedor ORDER BY nome COLLATE NOCASE";
        try (PreparedStatement comando = banco.conexao().prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {
            List<Fornecedor> lista = new ArrayList<>();
            while (resultado.next()) {
                lista.add(mapear(resultado));
            }
            return lista;
        } catch (SQLException e) {
            throw new ErroDeBancoException("Não foi possível listar os fornecedores", e);
        }
    }

    @Override
    public boolean remover(String cnpj) {
        try (PreparedStatement comando = banco.conexao().prepareStatement("DELETE FROM fornecedor WHERE cnpj = ?")) {
            comando.setString(1, cnpj);
            return comando.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ErroDeBancoException("Não foi possível remover o fornecedor (ele pode ter produtos cadastrados)", e);
        }
    }

    private static Fornecedor mapear(ResultSet resultado) throws SQLException {
        return new Fornecedor(resultado.getString("cnpj"), resultado.getString("nome"), resultado.getString("telefone"));
    }
}
