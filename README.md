# Sistema de Estoque (Java)

Sistema de gerenciamento de estoque para uma loja, feito em Java puro com interface de console. O foco do projeto é a **modelagem orientada a objetos** e a **separação em camadas**, não a interface.

## Funcionalidades

- Cadastro de fornecedores e produtos (com categoria e fornecedor vinculados)
- Entrada de estoque (compra) e saída de estoque (venda), com atualização automática da quantidade
- Bloqueio de venda quando não há estoque suficiente
- Alerta de estoque baixo (quantidade menor ou igual ao mínimo definido por produto)
- Listagem de produtos por categoria e por fornecedor
- Relatórios: valor total em estoque, produtos mais vendidos e histórico de movimentações
- Opção para carregar dados de exemplo e testar rapidamente

## Tecnologias

- Java 21
- Maven
- JUnit 5 (testes unitários)

## Arquitetura

```
src/main/java/com/vitor/estoque
├── Main.java                 # menu de console (só lê e exibe dados)
├── model/                    # entidades e regras próprias de cada objeto
│   ├── Produto, Fornecedor, MovimentacaoEstoque
│   └── Categoria, TipoMovimentacao (enums)
├── repository/               # persistência
│   ├── Repositorio<T, ID>    # interface (contrato)
│   └── RepositorioEmMemoria  # implementação atual
├── service/
│   └── EstoqueService        # regras de negócio e relatórios
└── exception/                # exceções de negócio customizadas
```

### Decisões de projeto

- **Encapsulamento de verdade:** `Produto` valida seus próprios dados (preço não negativo, não vende mais do que existe), então nenhum código externo consegue deixá-lo em estado inválido.
- **Interface de repositório:** o `EstoqueService` depende apenas da interface `Repositorio`. Trocar a persistência em memória por um banco de dados (JDBC) não exige alterar as regras de negócio.
- **`BigDecimal` para dinheiro:** evita os erros de arredondamento do `double`.
- **Histórico imutável:** cada `MovimentacaoEstoque` guarda o preço unitário da época, então mudar o preço de um produto não altera vendas passadas.
- **Exceções específicas:** `EstoqueInsuficienteException` e `ProdutoNaoEncontradoException` deixam os erros claros e fáceis de tratar.

## Como executar

Pré-requisito: JDK 21 ou superior.

**Pelo IntelliJ IDEA:** abra a pasta do projeto (File > Open > selecione o `pom.xml`), aguarde o Maven carregar e execute a classe `Main`.

**Pelo terminal (com Maven instalado):**

```bash
mvn clean package
java -jar target/sistema-estoque-1.0-SNAPSHOT.jar
```

Na primeira execução, use a opção **11** do menu para carregar dados de exemplo.

## Testes

```bash
mvn test
```

Os testes cobrem as regras de `Produto` (entrada, saída, estoque insuficiente, preço inválido) e do `EstoqueService` (duplicidade, movimentações, filtros, alertas, relatórios).

