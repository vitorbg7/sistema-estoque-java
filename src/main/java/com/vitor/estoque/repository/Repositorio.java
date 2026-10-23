package com.vitor.estoque.repository;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistência. A lógica de negócio só conhece esta interface,
 * então trocar "memória" por "banco de dados (JDBC)" depois não exige mexer
 * nas classes de service.
 */
public interface Repositorio<T, ID> {

    void salvar(T entidade);

    Optional<T> buscarPorId(ID id);

    List<T> listarTodos();

    boolean remover(ID id);

    default boolean existe(ID id) {
        return buscarPorId(id).isPresent();
    }
}
