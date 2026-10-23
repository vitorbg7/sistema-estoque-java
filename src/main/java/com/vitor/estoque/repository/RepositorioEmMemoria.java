package com.vitor.estoque.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Implementação que guarda tudo em um Map na memória (os dados somem ao
 * fechar o programa). Mantém a ordem de inserção.
 */
public class RepositorioEmMemoria<T, ID> implements Repositorio<T, ID> {

    private final Map<ID, T> dados = new LinkedHashMap<>();
    private final Function<T, ID> extratorDeId;

    public RepositorioEmMemoria(Function<T, ID> extratorDeId) {
        this.extratorDeId = extratorDeId;
    }

    @Override
    public void salvar(T entidade) {
        dados.put(extratorDeId.apply(entidade), entidade);
    }

    @Override
    public Optional<T> buscarPorId(ID id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public List<T> listarTodos() {
        return new ArrayList<>(dados.values());
    }

    @Override
    public boolean remover(ID id) {
        return dados.remove(id) != null;
    }
}
