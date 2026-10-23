package com.vitor.estoque.repository.jdbc;

import java.math.BigDecimal;

/**
 * Converte entre BigDecimal (usado no programa) e centavos inteiros (guardados no banco).
 */
final class Dinheiro {

    private Dinheiro() {
    }

    /** 19.90 -> 1990. Lança ArithmeticException se houver mais de 2 casas decimais. */
    static long paraCentavos(BigDecimal valor) {
        return valor.movePointRight(2).longValueExact();
    }

    /** 1990 -> 19.90 */
    static BigDecimal deCentavos(long centavos) {
        return BigDecimal.valueOf(centavos, 2);
    }
}
