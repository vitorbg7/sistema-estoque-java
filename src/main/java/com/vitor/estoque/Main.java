package com.vitor.estoque;

import com.vitor.estoque.exception.EstoqueException;
import com.vitor.estoque.model.Categoria;
import com.vitor.estoque.model.Fornecedor;
import com.vitor.estoque.model.MovimentacaoEstoque;
import com.vitor.estoque.model.Produto;
import com.vitor.estoque.repository.RepositorioEmMemoria;
import com.vitor.estoque.service.EstoqueService;
import com.vitor.estoque.service.EstoqueService.VendaPorProduto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Interface de console do sistema de estoque. Só cuida de ler e mostrar dados;
 * toda a regra de negócio está em EstoqueService e nas classes de model.
 */
public class Main {

    private static final Scanner ENTRADA = new Scanner(System.in);
    private static final Locale BR = Locale.of("pt", "BR");

    private static final EstoqueService SERVICE = new EstoqueService(
            new RepositorioEmMemoria<>(Produto::getCodigo),
            new RepositorioEmMemoria<>(Fornecedor::getCnpj),
            new RepositorioEmMemoria<>(MovimentacaoEstoque::getId));

    public static void main(String[] args) {
        System.out.println("=====================================");
        System.out.println("        SISTEMA DE ESTOQUE / LOJA");
        System.out.println("=====================================");

        boolean rodando = true;
        while (rodando) {
            exibirMenu();
            try {
                int opcao = lerInt("Escolha uma opção: ");
                switch (opcao) {
                    case 1 -> cadastrarFornecedor();
                    case 2 -> cadastrarProduto();
                    case 3 -> listarProdutos();
                    case 4 -> listarPorCategoria();
                    case 5 -> listarPorFornecedor();
                    case 6 -> registrarEntrada();
                    case 7 -> registrarSaida();
                    case 8 -> mostrarEstoqueBaixo();
                    case 9 -> mostrarRelatorios();
                    case 10 -> removerProduto();
                    case 11 -> carregarDadosDeExemplo();
                    case 0 -> rodando = false;
                    default -> System.out.println("Opção inválida.");
                }
            } catch (EstoqueException | IllegalArgumentException e) {
                System.out.println("Erro: " + e.getMessage());
            } catch (NoSuchElementException e) {
                // Fim da entrada (Ctrl+D / Ctrl+Z): encerra com educação.
                rodando = false;
            }
        }
        System.out.println("Até mais!");
    }

    // ---------- Menu ----------

    private static void exibirMenu() {
        System.out.println();
        System.out.println(" 1 - Cadastrar fornecedor");
        System.out.println(" 2 - Cadastrar produto");
        System.out.println(" 3 - Listar produtos");
        System.out.println(" 4 - Listar produtos por categoria");
        System.out.println(" 5 - Listar produtos por fornecedor");
        System.out.println(" 6 - Registrar entrada de estoque (compra)");
        System.out.println(" 7 - Registrar saída de estoque (venda)");
        System.out.println(" 8 - Alertas de estoque baixo");
        System.out.println(" 9 - Relatórios");
        System.out.println("10 - Remover produto");
        System.out.println("11 - Carregar dados de exemplo");
        System.out.println(" 0 - Sair");
    }

    // ---------- Ações ----------

    private static void cadastrarFornecedor() {
        String cnpj = lerTexto("CNPJ: ");
        String nome = lerTexto("Nome: ");
        String telefone = lerTexto("Telefone (opcional): ");
        SERVICE.cadastrarFornecedor(new Fornecedor(cnpj, nome, telefone));
        System.out.println("Fornecedor cadastrado.");
    }

    private static void cadastrarProduto() {
        if (SERVICE.listarFornecedores().isEmpty()) {
            System.out.println("Cadastre pelo menos um fornecedor antes de cadastrar produtos.");
            return;
        }
        String codigo = lerTexto("Código: ");
        String nome = lerTexto("Nome: ");
        BigDecimal preco = lerDecimal("Preço (R$): ");
        int minimo = lerInt("Estoque mínimo para alerta: ");
        Categoria categoria = escolherCategoria();
        Fornecedor fornecedor = escolherFornecedor();

        SERVICE.cadastrarProduto(new Produto(codigo, nome, preco, minimo, categoria, fornecedor));
        System.out.println("Produto cadastrado (estoque inicial 0; use a opção 6 para dar entrada).");
    }

    private static void listarProdutos() {
        imprimirLista(SERVICE.listarProdutos(), "Nenhum produto cadastrado.");
    }

    private static void listarPorCategoria() {
        Categoria categoria = escolherCategoria();
        imprimirLista(SERVICE.listarPorCategoria(categoria), "Nenhum produto nessa categoria.");
    }

    private static void listarPorFornecedor() {
        Fornecedor fornecedor = escolherFornecedor();
        imprimirLista(SERVICE.listarPorFornecedor(fornecedor.getCnpj()), "Nenhum produto desse fornecedor.");
    }

    private static void registrarEntrada() {
        String codigo = lerTexto("Código do produto: ");
        int quantidade = lerInt("Quantidade que entrou: ");
        SERVICE.registrarEntrada(codigo, quantidade);
        System.out.println("Entrada registrada. " + SERVICE.buscarProduto(codigo));
    }

    private static void registrarSaida() {
        String codigo = lerTexto("Código do produto: ");
        int quantidade = lerInt("Quantidade vendida: ");
        SERVICE.registrarSaida(codigo, quantidade);
        Produto produto = SERVICE.buscarProduto(codigo);
        System.out.println("Saída registrada. " + produto);
        if (produto.estoqueBaixo()) {
            System.out.println("ATENÇÃO: estoque baixo para '" + produto.getNome() + "'!");
        }
    }

    private static void mostrarEstoqueBaixo() {
        imprimirLista(SERVICE.produtosComEstoqueBaixo(), "Nenhum produto com estoque baixo.");
    }

    private static void mostrarRelatorios() {
        System.out.println("--- Relatório ---");
        System.out.println(String.format(BR, "Valor total em estoque: R$ %.2f", SERVICE.valorTotalEmEstoque()));

        System.out.println("Produtos mais vendidos:");
        List<VendaPorProduto> maisVendidos = SERVICE.produtosMaisVendidos(5);
        if (maisVendidos.isEmpty()) {
            System.out.println("  (nenhuma venda registrada)");
        }
        int posicao = 1;
        for (VendaPorProduto venda : maisVendidos) {
            System.out.println("  " + posicao++ + ". " + venda.produto().getNome()
                    + " - " + venda.quantidade() + " un.");
        }

        System.out.println("Histórico de movimentações:");
        imprimirLista(SERVICE.listarMovimentacoes(), "  (nenhuma movimentação)");
    }

    private static void removerProduto() {
        String codigo = lerTexto("Código do produto a remover: ");
        SERVICE.removerProduto(codigo);
        System.out.println("Produto removido.");
    }

    private static void carregarDadosDeExemplo() {
        Fornecedor techDistrib = new Fornecedor("11.111.111/0001-11", "TechDistrib", "(41) 3333-1111");
        Fornecedor mercadaoSul = new Fornecedor("22.222.222/0001-22", "Mercadão Sul", "(41) 3333-2222");
        SERVICE.cadastrarFornecedor(techDistrib);
        SERVICE.cadastrarFornecedor(mercadaoSul);

        SERVICE.cadastrarProduto(new Produto("TEC001", "Mouse Gamer", new BigDecimal("129.90"), 5, Categoria.ELETRONICOS, techDistrib));
        SERVICE.cadastrarProduto(new Produto("TEC002", "Teclado Mecânico", new BigDecimal("289.90"), 3, Categoria.ELETRONICOS, techDistrib));
        SERVICE.cadastrarProduto(new Produto("ALI001", "Café 500g", new BigDecimal("18.50"), 10, Categoria.ALIMENTOS, mercadaoSul));
        SERVICE.cadastrarProduto(new Produto("LIM001", "Detergente", new BigDecimal("3.20"), 20, Categoria.LIMPEZA, mercadaoSul));

        SERVICE.registrarEntrada("TEC001", 12);
        SERVICE.registrarEntrada("TEC002", 6);
        SERVICE.registrarEntrada("ALI001", 30);
        SERVICE.registrarEntrada("LIM001", 25);
        SERVICE.registrarSaida("TEC001", 8);
        SERVICE.registrarSaida("ALI001", 22);
        SERVICE.registrarSaida("LIM001", 3);

        System.out.println("Dados de exemplo carregados (2 fornecedores, 4 produtos e algumas movimentações).");
    }

    // ---------- Escolhas auxiliares ----------

    private static Categoria escolherCategoria() {
        Categoria[] categorias = Categoria.values();
        for (int i = 0; i < categorias.length; i++) {
            System.out.println("  " + (i + 1) + " - " + categorias[i]);
        }
        int escolha = lerInt("Categoria: ");
        if (escolha < 1 || escolha > categorias.length) {
            throw new IllegalArgumentException("Categoria inválida.");
        }
        return categorias[escolha - 1];
    }

    private static Fornecedor escolherFornecedor() {
        List<Fornecedor> fornecedores = SERVICE.listarFornecedores();
        if (fornecedores.isEmpty()) {
            throw new IllegalArgumentException("Nenhum fornecedor cadastrado.");
        }
        for (Fornecedor f : fornecedores) {
            System.out.println("  " + f);
        }
        return SERVICE.buscarFornecedor(lerTexto("CNPJ do fornecedor: "));
    }

    // ---------- Leitura segura de entrada ----------

    private static String lerTexto(String mensagem) {
        System.out.print(mensagem);
        return ENTRADA.nextLine().trim();
    }

    private static int lerInt(String mensagem) {
        while (true) {
            String texto = lerTexto(mensagem);
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("Digite um número inteiro válido.");
            }
        }
    }

    private static BigDecimal lerDecimal(String mensagem) {
        while (true) {
            String texto = lerTexto(mensagem).replace(',', '.');
            try {
                return new BigDecimal(texto);
            } catch (NumberFormatException e) {
                System.out.println("Digite um valor válido (ex: 19,90).");
            }
        }
    }

    private static <T> void imprimirLista(List<T> itens, String mensagemVazia) {
        if (itens.isEmpty()) {
            System.out.println(mensagemVazia);
            return;
        }
        itens.forEach(item -> System.out.println("  " + item));
    }
}
