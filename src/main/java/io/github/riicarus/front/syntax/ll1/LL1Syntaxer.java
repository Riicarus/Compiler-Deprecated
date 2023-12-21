package io.github.riicarus.front.syntax.ll1;

import io.github.riicarus.common.data.SyntaxParseResult;
import io.github.riicarus.common.data.Token;
import io.github.riicarus.common.data.ast.detailed.DetailedASTNode;
import io.github.riicarus.common.data.ast.detailed.TerminalASTNode;
import io.github.riicarus.front.lex.LexicalSymbol;
import io.github.riicarus.front.syntax.SyntaxDefiner;
import io.github.riicarus.front.syntax.SyntaxProduction;
import io.github.riicarus.front.syntax.SyntaxSymbol;
import io.github.riicarus.front.syntax.Syntaxer;

import java.util.*;

/**
 * LL1 文法分析器.
 *
 * @author Riicarus
 * @create 2023-11-22 21:27
 * @since 1.0.0
 */
public class LL1Syntaxer implements Syntaxer {

    /**
     * 产生式 AST 生成埋点, 为非终结符.
     *
     * @param <T> 产生式对应的 AST 类型
     */
    class ProductionCreateSymbol<T extends DetailedASTNode> extends LL1SyntaxSymbol {

        private final SyntaxProduction<T> production;

        public ProductionCreateSymbol(SyntaxProduction<T> production) {
            super("ProductionCreateSymbol");
            this.production = production;
        }

        public void create() {
            List<DetailedASTNode> children = new ArrayList<>();
            for (SyntaxSymbol symbol : production.getRHS()) {
                children.add(0, astStack.pop());
            }

            T nonterminalNode = production.createNode(children);
            astStack.push(nonterminalNode);
        }

        @Override
        public String toString() {
            return super.toString() + " of " + production;
        }
    }

    private final LL1SyntaxDefiner definer;
    private final Map<SyntaxSymbol, Map<String, Set<SyntaxProduction<?>>>> analyzeMap = new HashMap<>();

    private final List<Token> tokens = new ArrayList<>();
    private final Set<LexicalSymbol> assistantLexSymbolSet = new HashSet<>();
    private final Stack<SyntaxSymbol> parseStack = new Stack<>();
    private final Stack<DetailedASTNode> astStack = new Stack<>();

    private int tokenIdx;

    public LL1Syntaxer(LL1SyntaxDefiner definer) {
        this.definer = definer;

        definer.load();
        this.analyzeMap.clear();
        definer.getAnalyzeMap().forEach((symbol, setMap) ->
                setMap.forEach((s, set) ->
                        set.forEach(p -> {
                            analyzeMap.putIfAbsent(symbol, new HashMap<>());
                            analyzeMap.get(symbol).putIfAbsent(s, new HashSet<>());
                            analyzeMap.get(symbol).get(s).add(p);
                        })
                )
        );
    }

    @Override
    public SyntaxParseResult parse(List<Token> tokenList, Set<LexicalSymbol> assistSet) {
        reset(tokenList, assistSet);

        while (!checkEnds()) {
            SyntaxSymbol topSymbol = parseStack.peek();

            System.out.println();
            System.out.println("Parse Stack:");
            parseStack.forEach(symbol -> System.out.println("\t" + symbol));
            System.out.println("AST Stack:");
            astStack.forEach(node -> System.out.println("\t" + node));
            System.out.println("Top Symbol:");
            System.out.println("\t" + topSymbol);
            System.out.println("Cur Token:");
            System.out.println("\t" + curToken());

            if (topSymbol.isTerminal()) {
                if (topSymbol.equals(definer.getEpsilonSymbol())) {
                    parseStack.pop();
                } else if (topSymbol.getName().equals(curToken().getSymbol().getName())) {
                    parseStack.pop();
                    nextTokenIgnoreAssistant();
                } else {
                    throw new IllegalStateException("LL1Syntax wrong: terminal symbol not match, want: " + topSymbol + ", but get: " + curToken());
                }

                if (!topSymbol.equals(definer.getEndSymbol())) {
                    astStack.push(new TerminalASTNode(topSymbol));
                }
                continue;
            }

            // 根据 parseStack 中的埋点, 说明该埋点对应的 AST 右部已经被完全解析, 可以创建对应的非终结符的 AST
            if (topSymbol instanceof ProductionCreateSymbol<?>) {
                ((ProductionCreateSymbol<?>) topSymbol).create();
                parseStack.pop();
                continue;
            }

            SyntaxProduction<?> production = getProduction(topSymbol);
            if (production != null) {
                parseStack.pop();

                // 在 parseStack 中为产生式创建对应的埋点, 以在其右部被完全解析后, 创建对应的 AST 节点
                ProductionCreateSymbol<?> createSymbol = new ProductionCreateSymbol<>(production);
                parseStack.push(createSymbol);

                // 将产生式的右部符号反向压入分析栈
                List<SyntaxSymbol> rhs = production.getRHS();
                for (int i = rhs.size() - 1; i >= 0; i--) {
                    parseStack.push(rhs.get(i));
                }
                System.out.println("Apply Production:");
                System.out.println("\t" + production);
            } else {
                throw new IllegalStateException("LL1Syntax wrong: not production found for symbol: " + topSymbol);
            }
        }

        System.out.println();
        System.out.println("Parse Stack:");
        parseStack.forEach(symbol -> System.out.println("\t" + symbol));
        System.out.println("AST Stack:");
        astStack.forEach(node -> System.out.println("\t" + node));

        if (astStack.size() != 1) {
            throw new IllegalStateException("LL1Syntax wrong: ast stack should only have one element, but get: " + astStack);
        }

        return new SyntaxParseResult(astStack.pop());
    }

    private void reset(List<Token> tokenList, Set<LexicalSymbol> assistSet) {
        tokenIdx = -1;

        astStack.clear();

        parseStack.clear();
        parseStack.push(definer.getEndSymbol());
        parseStack.push(definer.getStartSymbol());

        tokens.clear();
        tokens.addAll(tokenList);

        assistantLexSymbolSet.clear();
        assistantLexSymbolSet.addAll(assistSet);

        nextTokenIgnoreAssistant();
    }

    public void nextTokenIgnoreAssistant() {
        do {
            nextToken();
        } while (curToken() != null && assistantLexSymbolSet.contains(curToken().getSymbol()));
    }

    public Token curToken() {
        return tokenEnds() ? null : tokens.get(tokenIdx);
    }

    public void nextToken() {
        if (!tokenEnds()) {
            tokenIdx++;
        }
    }

    public boolean tokenEnds() {
        return tokenIdx >= tokens.size();
    }

    public SyntaxProduction<?> getProduction(SyntaxSymbol symbol) {
        if (analyzeMap.get(symbol) == null) return null;

        Set<SyntaxProduction<?>> productionSet = analyzeMap.get(symbol).get(curToken().getSymbol().getName());

        if (productionSet == null) {
            throw new IllegalStateException("LL1Syntax error: can not get production of symbol: " + symbol + ", cur token: " + curToken());
        }

        // LL1 的 productionSet 中只会有一个元素
        return productionSet.stream().findFirst().orElse(null);
    }

    /**
     * 检查是否分析完成.
     *
     * @return true-分析完成; false-分析未完成
     */
    public boolean checkEnds() {
        if (parseStack.isEmpty()) return true;

        SyntaxSymbol topSymbol = parseStack.peek();
        if (topSymbol.equals(definer.getEndSymbol())) {
            if (tokenEnds())  // 如果 tokens 已经遍历完, 报错
                throw new IllegalStateException("LL1Syntax wrong: syntax not complete, want: \"" + topSymbol + "\" but get null.");
            else {
                if (curToken().getSymbol().getName().equals(definer.getEndSymbol().getName())) {  // 如果 tokens 也是结束符号, 就直接消去
                    parseStack.pop();
                    return true;
                } else    // 否则报错
                    throw new IllegalStateException("LL1Syntax wrong: syntax complete, but get \"" + topSymbol + "\".");
            }
        }

        return false;
    }

    public SyntaxDefiner getDefiner() {
        return definer;
    }
}

