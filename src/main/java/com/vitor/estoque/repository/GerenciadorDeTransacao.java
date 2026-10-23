package com.vitor.estoque.repository;

import java.util.function.Supplier;

/**
 * Executa um bloco de código como uma unidade: ou tudo dá certo, ou nada é gravado.
 * O banco de dados implementa isto com commit/rollback; na versão em memória não
 * há o que desfazer, então o bloco simplesmente roda.
 */
public interface GerenciadorDeTransacao {

    <T> T executar(Supplier<T> acao);

    default void executarSemRetorno(Runnable acao) {
        executar(() -> {
            acao.run();
            return null;
        });
    }

    static GerenciadorDeTransacao semTransacao() {
        return new GerenciadorDeTransacao() {
            @Override
            public <T> T executar(Supplier<T> acao) {
                return acao.get();
            }
        };
    }
}
