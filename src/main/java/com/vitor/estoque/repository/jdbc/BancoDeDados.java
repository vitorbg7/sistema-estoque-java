package com.vitor.estoque.repository.jdbc;

import com.vitor.estoque.exception.ErroDeBancoException;
import com.vitor.estoque.repository.GerenciadorDeTransacao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Abre a conexão com o SQLite, cria as tabelas (schema.sql) e controla transações.
 * Os repositórios JDBC recebem esta classe e usam a mesma conexão, o que permite
 * que várias gravações façam parte de uma única transação.
 */
public final class BancoDeDados implements GerenciadorDeTransacao, AutoCloseable {

    private final Connection conexao;
    private boolean emTransacao = false;

    public BancoDeDados(String url) {
        Connection nova = null;
        try {
            nova = DriverManager.getConnection(url);
            try (Statement comando = nova.createStatement()) {
                // O SQLite só respeita chaves estrangeiras se isto estiver ligado.
                comando.execute("PRAGMA foreign_keys = ON");
            }
            criarTabelas(nova);
            this.conexao = nova;
        } catch (SQLException | IOException e) {
            fecharSilenciosamente(nova);
            throw new ErroDeBancoException("Não foi possível abrir o banco de dados", e);
        }
    }

    /** Banco gravado em um arquivo (os dados continuam lá quando o programa fecha). */
    public static BancoDeDados arquivo(Path caminho) {
        return new BancoDeDados("jdbc:sqlite:" + caminho.toAbsolutePath());
    }

    /** Banco temporário: some quando a conexão fecha. Usado nos testes. */
    public static BancoDeDados emMemoria() {
        return new BancoDeDados("jdbc:sqlite::memory:");
    }

    Connection conexao() {
        return conexao;
    }

    @Override
    public <T> T executar(Supplier<T> acao) {
        if (emTransacao) {
            return acao.get(); // já está dentro de uma transação: faz parte dela
        }
        emTransacao = true;
        try {
            conexao.setAutoCommit(false);
            T resultado = acao.get();
            conexao.commit();
            return resultado;
        } catch (SQLException e) {
            desfazer();
            throw new ErroDeBancoException("Falha ao gravar no banco de dados", e);
        } catch (RuntimeException e) {
            desfazer(); // qualquer erro no meio do caminho cancela tudo que foi gravado
            throw e;
        } finally {
            emTransacao = false;
            try {
                conexao.setAutoCommit(true);
            } catch (SQLException ignorada) {
                // a conexão já estava com problema; nada a fazer
            }
        }
    }

    @Override
    public void close() {
        fecharSilenciosamente(conexao);
    }

    private void desfazer() {
        try {
            conexao.rollback();
        } catch (SQLException ignorada) {
            // se o rollback falhar, a conexão está perdida de qualquer forma
        }
    }

    private static void criarTabelas(Connection conexao) throws SQLException, IOException {
        String script;
        try (InputStream entrada = BancoDeDados.class.getResourceAsStream("/schema.sql")) {
            if (entrada == null) {
                throw new IOException("arquivo schema.sql não encontrado");
            }
            script = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Tira os comentários e executa um comando por vez (separados por ponto e vírgula).
        String semComentarios = String.join("\n", Arrays.stream(script.split("\n"))
                .filter(linha -> !linha.trim().startsWith("--"))
                .toList());
        try (Statement comando = conexao.createStatement()) {
            for (String sql : semComentarios.split(";")) {
                if (!sql.isBlank()) {
                    comando.executeUpdate(sql);
                }
            }
        }
    }

    private static void fecharSilenciosamente(Connection conexao) {
        if (conexao == null) {
            return;
        }
        try {
            conexao.close();
        } catch (SQLException ignorada) {
            // nada a fazer
        }
    }
}
