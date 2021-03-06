package br.ecomp.compiler.parser;

import br.ecomp.compiler.lexer.Token;
import br.ecomp.compiler.lexer.Token.TokenType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Filipe Boaventura
 * @since 24/09/2016.
 */
public class Parser {
    /**
     * Ponteiro para o simbolo atual da entrada.
     */
    private Token currentToken, previousToken;
    private List<Token> tokenList;
    private int index;
    private int syntaxErrorCount;
    private int semanticErrorCount;
    private BufferedWriter sinWriter, semWriter;
    private boolean firstRun;
    private SymbolTable top;
    private Symbol.Type currentType;
    private boolean vecAtrib;

    /**
     * Inicia a análise sintática sobre a coleção de
     * {@link Token}s.
     * @param tokens coleção de entrada contendo os
     * {@link Token}s para a análise sintática.
     */
    public void parse(List<Token> tokens, String outputPath) throws IOException {
        firstRun = true;
        syntaxErrorCount = 0;
        semanticErrorCount = 0;
        tokenList = tokens;
        index = -1;
        String sinOut = "output" + File.separator + "sin_" +  outputPath;
        String semOut = "output" + File.separator + "sem_" + outputPath;
        sinWriter = new BufferedWriter(new FileWriter(new File(sinOut)));
        semWriter = new BufferedWriter(new FileWriter(new File(semOut)));

        System.out.println("Passo 2: Analise Sintatica e Indexacao de Simbolos Globais");
        programa();
        System.out.println(String.format("\t%d erros sintáticos foram encontrados", syntaxErrorCount));
        sinWriter.write(String.format("%d erros sintáticos foram encontrados", syntaxErrorCount));
        sinWriter.newLine();
        if (syntaxErrorCount == 0) {
            System.out.println("\tAnalise Sintatica concluida com sucesso.");
            sinWriter.write("Analise Sintatica concluida com sucesso.");
            sinWriter.newLine();
        }
        sinWriter.close();
        System.out.println("O status da analise sintatica foi salvo no arquivo " + sinOut);

        // segunda leitura
        firstRun = false;
        index = -1;
        System.out.println("Passo 3: Analise Semantica");
        System.out.println("Simbolos globais encontrados" + top.toString());
        programa();
        System.out.printf("\t%d erros semanticos foram encontrados.\n", semanticErrorCount);
        if (semanticErrorCount == 0) {
            logSemanticAnalysis("\tAnalise semantica concluida com sucesso.");
        }
        semWriter.close();
        System.out.println("O status da analise semantica foi salvo no arquivo " + semOut);
    }

    /**
     * Consome um simbolo do vetor de entrada e atualiza o
     * {@link Parser#currentToken}. Consome mais um simbolo
     * caso um token do tipo {@link br.ecomp.compiler.lexer.Token.TokenType#COMMENT}
     * seja encontrado, para ignorar comentários.
     *
     * @return retorna true se conseguiu atualizar o
     *         {@link Parser#currentToken}. Retorna false
     *         caso tenha chegado no fim da entrada.
     */
    private boolean nextToken() {
        if (index + 1 < tokenList.size()) {
            index++;
            previousToken = currentToken;
            currentToken = tokenList.get(index);
            accept(Token.TokenType.COMMENT); // Pulando comentarios
            //System.out.println("Token Atual: " + currentToken.toString());
            return true;
        } return false;
    }

    /**
     * Verifica se o {@link Parser#currentToken} é do tipo
     * informado por parametro. Caso seja, move o ponteiro
     * para o próximo simbolo da entrada.
     * @param type o {@link br.ecomp.compiler.lexer.Token.TokenType}
     *             que o {@link Parser#currentToken} deve apresentar.
     * @return true, caso o simbolo atual apresente este tipo. false,
     *          caso contrário.
     */
    private boolean accept (Token.TokenType type) {
        if (currentToken.getType() == type) {
            nextToken();
            return true;
        } return false;
    }

    /**
     * Espera que o {@link Parser#currentToken} seja do
     * {@link br.ecomp.compiler.lexer.Token.TokenType}
     * informado por parametro. Incrementa a contagem de
     * erros e exibe mensagem de erro caso não seja.
     * @param type o {@link br.ecomp.compiler.lexer.Token.TokenType}
     *             que o {@link Parser#currentToken} deve apresentar.
     * @return true, caso o simbolo atual apresente este tipo. false,
     *          caso contrário.
     */
    private boolean expect(Token.TokenType type) {
        if (accept(type)) return true;
        syntaxError(type);
        return false;
    }

    private boolean lookAheadToken(int n, TokenType type) {
        if (index + n < tokenList.size()) {
            return tokenList.get(index + n).getType() == type;
        } return false;
    }

    private void syntaxError(TokenType... expected) {
        if (firstRun) {
            if (expected.length == 0)
                throw new IllegalArgumentException("informe pelo menos um TokenType esperado");
            syntaxErrorCount++;
            String expectedTokenNames = "";

            for (int i = 0; i < expected.length; i++) {
                expectedTokenNames += expected[i].toString();
                if (i < expected.length - 1)
                    expectedTokenNames += ", ";
            }
            String errorMsg = String.format("Erro na linha %d. Esperava: %s. Obteve: %s.",
                    currentToken.getLine(), expectedTokenNames, currentToken.getLexeme()
                            + " " + currentToken.getType());
            System.out.println(errorMsg);
            try {
                sinWriter.write(errorMsg);
                sinWriter.newLine();
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    /**
     *
     * @param sync
     */
    private void panicMode(TokenType... sync) {
    	List<TokenType> syncTokens = Arrays.asList(sync);
    	while(!syncTokens.contains(currentToken.getType())){
    		System.out.println("\tPulou Token: " + currentToken.toString());
    		if (!nextToken()) return;
    	}
	}

    private void logSemanticAnalysis(String message) {
        semanticErrorCount++;
        System.out.println(message);
        try {
            semWriter.write(message);
            semWriter.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mismatchedTypeError(int line, Symbol.Type expected, Symbol.Type actual) {
        String s = "Erro! Tipo invalido na linha " + line + ": " +
                "\n\tEsperava " + expected.name() +
                "\n\tObteve " + actual.name();
        logSemanticAnalysis(s);
    }

    private void variableAlreadyDefinedError(Token token) {
        String s = String.format("Erro na linha %d: \"%s\" ja foi definido no escopo.",
                token.getLine(), token.getLexeme());
        logSemanticAnalysis(s);
    }

    private void symbolNotFoundError(Token t) {
        String s = String.format("Erro na linha %d: nao foi possivel encontrar o simbolo \"%s\".",
                t.getLine(), t.getLexeme());
        logSemanticAnalysis(s);
    }

    private void putSymbol(Symbol s) {
        if (top.containsSymbolLocal(s)) variableAlreadyDefinedError(s.getToken());
        else top.put(s);
    }

    private Symbol getSymbol(Token t) {
        return getSymbol(t, false);
    }

    private Symbol getSymbol(Token t, boolean isFunction) {
        if (top.containsSymbol(t, isFunction)) return top.get(t, isFunction);
        else {
            symbolNotFoundError(t);
            return null;
        }
    }

    private void constantAssignmentError(Token t) {
        System.out.printf("Erro na linha %d: nao eh possivel atribuir valores a constante \"%s\".\n",
                t.getLine(), t.getLexeme());
    }

    private void vectDimensionError(Vector expected, Vector actual) {
        String msg = String.format("Erro na linha %d: O vetor %d-dimensional \"%s\" " +
                        "nao pode ser usado como um vetor %d-dimensional.",
                actual.getToken().getLine(), expected.getDimensions(),
                expected.getToken().getLexeme(), actual.getDimensions());
        logSemanticAnalysis(msg);
    }

    /******************************************
     *            Nao-Terminais
     *****************************************/

    // <Programa> ::= <Variaveis><C>|<C>
    private void programa() {
        nextToken();
        if (firstRun) top = new SymbolTable(null);
        variaveis();
        c();
    }

    // <Variaveis> ::= 'var''inicio'<Var_List>'fim'
    private void variaveis() {
        if (accept(Token.TokenType.VAR)){ // Se aceitou um var

            // Espera um inicio
            if (!expect(Token.TokenType.INICIO)) {
                panicMode(Token.TokenType.INICIO, Token.TokenType.FIM,
                        Token.TokenType.BOOLEANO, Token.TokenType.CADEIA,
                        Token.TokenType.CARACTERE, Token.TokenType.REAL,
                        Token.TokenType.INTEIRO, Token.TokenType.CONST,
                        Token.TokenType.PROGRAMA);
                accept(TokenType.INICIO);
            }

            varlist();

            if(!expect(Token.TokenType.FIM)){
            	panicMode(Token.TokenType.PROGRAMA, Token.TokenType.CONST,
            			Token.TokenType.FIM);
            	accept(TokenType.FIM);
            }
        }

    }

    // <C> ::= <Constantes><P> | <P>
    private void c() {
        constantes();
        p();
    }

    // <P> ::= 'programa'<Bloco><Funcoes>
    private void p() {
        if(!expect(Token.TokenType.PROGRAMA)){
        	panicMode(Token.TokenType.PROGRAMA);
        	accept(Token.TokenType.PROGRAMA);
        }
        bloco();
        funcoes();
    }

    // <Constantes> ::= 'const''inicio'<Const_List>'fim'
    private void constantes() {
        if (accept(Token.TokenType.CONST)) {

        	 // Espera um inicio
            if (!expect(Token.TokenType.INICIO)) {
                panicMode(Token.TokenType.INICIO, Token.TokenType.FIM,
                        Token.TokenType.BOOLEANO, Token.TokenType.CADEIA,
                        Token.TokenType.CARACTERE, Token.TokenType.REAL,
                        Token.TokenType.INTEIRO, Token.TokenType.PROGRAMA);
                accept(TokenType.INICIO);
            }

            constlist();

            if(!expect(Token.TokenType.FIM)){
            	panicMode(Token.TokenType.PROGRAMA, Token.TokenType.FIM);
            	accept(TokenType.FIM);
            }
        }
    }

    // <Const_List> ::= <Tipo><Const_Decl><Const_List>  |<>
    private void constlist() {
        if (tipo()) {
            constdecl();
            constlist();
        }
        else if(currentToken.getType() != Token.TokenType.FIM){ //se nao for vazio entra aqui
        	syntaxError(currentToken.getType()); //nao podia usar o accept pq nao pode consumir o FIM
        	panicMode(Token.TokenType.IDENTIFIER);
        	constdecl();
        	constlist();
        } // o else eh o vazio
    }

    // <Const_Decl> ::= id'<<'<Literal><Const_Decl2>
    private void constdecl() {

        Symbol s = null;
        if(!expect(Token.TokenType.IDENTIFIER)){
        	panicMode(Token.TokenType.ATRIB, Token.TokenType.IDENTIFIER);
        	accept(TokenType.IDENTIFIER);
        } else {
            s = new Variable(previousToken, currentType, true);
        }

        if(!expect(Token.TokenType.ATRIB)){
        	panicMode(Token.TokenType.ATRIB, Token.TokenType.NUMBER,
        			Token.TokenType.CHARACTER, Token.TokenType.CHAR_STRING,
        			Token.TokenType.BOOL_V);
        	accept(Token.TokenType.ATRIB);
        }
        Symbol.Type t = literal();
        if (firstRun)
            if (s != null && currentType.equals(t)) {
                putSymbol(s);
            } else mismatchedTypeError(previousToken.getLine(), currentType, t);

        constdecl2();
    }

    // <Const_Decl2> ::= ','<Const_Decl> | ';'
    private void constdecl2() {
        if (accept(Token.TokenType.COMMA)) {
            constdecl();
        } else {
        	if(!expect(Token.TokenType.SEMICOLON)){
            	panicMode(Token.TokenType.SEMICOLON, Token.TokenType.INTEIRO,
            			Token.TokenType.BOOLEANO,
            			Token.TokenType.CARACTERE, Token.TokenType.CADEIA,
            			Token.TokenType.REAL, Token.TokenType.FIM);
            }
        }
    }

    // <Var_List> ::= <Tipo><Var_Decl><Var_List> |<>
    private void varlist() {
        if (tipo()) { // espera um tipo
            vardecl();
            varlist();
        }
        // FIXME
//        else if(currentToken.getType() != Token.TokenType.FIM){ //se nao for vazio entra aqui
//        	syntaxError(currentToken.getType()); //nao podia usar o accept pq nao pode consumir o FIM
//        	panicMode(Token.TokenType.IDENTIFIER);
//        	vardecl();
//        	varlist();
//        } // o else eh o vazio
    }

    // <Var_Decl> ::= <Id_Vetor>','<Var_Decl> | <Id_Vetor>';' AMBIGUIDADE! Fatorar a esquerda!
    private void vardecl() {
        Symbol s = idvetor();
        if (firstRun || (!firstRun && !top.isRoot())) putSymbol(s);
        if (accept(Token.TokenType.COMMA)) {
            vardecl();
        }
        else  { // Se não tem virgula, testa por ponto e virgula. Isso resolve a ambiguidade?
            if(!expect(Token.TokenType.SEMICOLON)){
            	panicMode(Token.TokenType.SEMICOLON, Token.TokenType.INTEIRO,
            			Token.TokenType.BOOLEANO,
            			Token.TokenType.CARACTERE, Token.TokenType.CADEIA,
            			Token.TokenType.REAL, Token.TokenType.FIM);
            }
        }
    }

    // <Id_Vetor> ::= id<Vetor>
    private Symbol idvetor() {
        if(!expect(Token.TokenType.IDENTIFIER)){
        	panicMode(Token.TokenType.COMMA, Token.TokenType.SEMICOLON,
        			Token.TokenType.IDENTIFIER);
        	accept(TokenType.IDENTIFIER);
        }
        return vetor();
    }

    // <Vetor> ::= '<<<'numero_t<Vetor2>'>>>'  | <>
    // vetor simplificado.
    // Vetor original:
    // <Vetor> ::= '<<<'<Exp_Aritmetica><Vetor2>'>>>'  | <>
    // <Vetor2> ::= ','<Exp_Aritmetica><Vetor2> | <>
    private Symbol vetor() {
        Token identifier = previousToken;
        Symbol s;
        if (accept(Token.TokenType.VEC_DELIM_L)) {//se encontrou <<<
            Symbol.Type t = expAritimetica();
            if ((!firstRun) && t != Symbol.Type.INTEIRO)
                mismatchedTypeError(identifier.getLine(), Symbol.Type.INTEIRO, t);
            int dimensions = 1 + vetor2();
            if(!expect(Token.TokenType.VEC_DELIM_R)){ // espera que feche o vetor com >>>
            	panicMode(Token.TokenType.VEC_DELIM_R, Token.TokenType.IDENTIFIER,
            			Token.TokenType.COMMA, Token.TokenType.SEMICOLON);
            	accept(TokenType.VEC_DELIM_R);
            }
            s = new Vector(identifier, currentType, dimensions);
        } else s = new Variable(identifier, currentType);
        return s;
    }

    // <Vetor2> ::= ','<Exp_Aritmetica><Vetor2> | <>
    private int vetor2() {
        if (accept(Token.TokenType.COMMA)) { // se encontrou uma virgula
            Symbol.Type t = expAritimetica();
            if ((!firstRun) && t != Symbol.Type.INTEIRO)
                mismatchedTypeError(previousToken.getLine(), Symbol.Type.INTEIRO, t);
            return 1 + vetor2(); // pode se repetir
        } return 0;
    }

    // <Bloco> ::= 'inicio'<Corpo_Bloco>'fim'
    private void bloco(Symbol... args) {
        if(!expect(Token.TokenType.INICIO)){
        	 panicMode(Token.TokenType.INICIO, Token.TokenType.FIM,
                     Token.TokenType.IDENTIFIER, Token.TokenType.ENQUANTO,
                     Token.TokenType.SE, Token.TokenType.ESCREVA,
                     Token.TokenType.LEIA);
        	accept(Token.TokenType.INICIO);
        }
        SymbolTable saved = top;
        top = new SymbolTable(top);

        // se for uma função, arg contém seus argumentos
        //  eles são inseridos na tabela de simbolos do
        // bloco da funcao
        for (Symbol s : args) {
            putSymbol(s);
        }
        bloco2();
        top = saved;
    }

    private void bloco2() {
        if (currentToken.getType() == TokenType.VAR){
            variaveis();
        }
        varlist(); // varlist pode ser vazio

        corpoBloco();

        if(!expect(Token.TokenType.FIM)){
            panicMode(Token.TokenType.FIM, Token.TokenType.FUNCAO);
            accept(TokenType.FIM);
        }
    }

    //<Corpo_Bloco> ::= <Comando><Corpo_Bloco> | <Atribuicao><Corpo_Bloco> | <Chamada_Funcao>';'<Corpo_Bloco> | <>
    private void corpoBloco() {
        if (currentToken.getType() == TokenType.IDENTIFIER) {
        	if (lookAheadToken(1, TokenType.PAREN_L)) {
        		chamadaFuncao();
        		expect(TokenType.SEMICOLON);
        		corpoBloco();
        	}
        	else{ //caso nao encontre PAREN_L depois de id
        		atribuicao();
        		corpoBloco();
        	}

        } // abaixo são os comandos
        // <Se> ::= 'se''('<Exp_Logica>')''entao'<Bloco><Senao>
        else if (accept(TokenType.SE)) {
            int line = previousToken.getLine();
            if(!expect(TokenType.PAREN_L)){
            	panicMode(Token.TokenType.PAREN_L, Token.TokenType.IDENTIFIER,
            			Token.TokenType.NUMBER);
            	accept(Token.TokenType.PAREN_L);
            }

            Symbol.Type type = expLogica();
            if (!firstRun && type != Symbol.Type.BOOLEANO)
                mismatchedTypeError(line, Symbol.Type.BOOLEANO, type);

            if(!expect(TokenType.PAREN_R)){
            	panicMode(Token.TokenType.PAREN_R, Token.TokenType.ENTAO);
            	accept(Token.TokenType.PAREN_R);
            }
            if(!expect(TokenType.ENTAO)){
            	panicMode(Token.TokenType.ENTAO, Token.TokenType.INICIO);
            	accept(Token.TokenType.ENTAO);
            }

            bloco();

            // <Senao> ::= 'senao'<Bloco> | <>
            if (accept(TokenType.SENAO)) {
                bloco();
            }

            corpoBloco();
        }
        // <Enquanto> ::= 'enquanto''('booleano_t')''faca'<Bloco>
        else if (accept(TokenType.ENQUANTO)) {
            int line = previousToken.getLine();
        	if(!expect(TokenType.PAREN_L)){
            	panicMode(Token.TokenType.PAREN_L, Token.TokenType.IDENTIFIER,
            			Token.TokenType.NUMBER);
            	accept(Token.TokenType.PAREN_L);
            }

            Symbol.Type type = expLogica();
            if (!firstRun && type != Symbol.Type.BOOLEANO)
                mismatchedTypeError(line, Symbol.Type.BOOLEANO, type);

            if(!expect(TokenType.PAREN_R)){
            	panicMode(Token.TokenType.PAREN_R, Token.TokenType.FACA);
            	accept(Token.TokenType.PAREN_R);
            }

            if(!expect(TokenType.FACA)){
            	panicMode(Token.TokenType.FACA, Token.TokenType.INICIO);
            	accept(Token.TokenType.FACA);
            }

            bloco();
            corpoBloco();
        }
        // <Escreva> ::= 'escreva''('<Escreva_Params>')'';'
        else if (accept(TokenType.ESCREVA)) {
            if(!expect(TokenType.PAREN_L)){
            	panicMode(Token.TokenType.PAREN_L, Token.TokenType.NUMBER,
            			Token.TokenType.IDENTIFIER, Token.TokenType.CHAR_STRING,
            			Token.TokenType.CARACTERE);
            	accept(Token.TokenType.PAREN_L);
            }

            escrevaParams();
            if(!expect(TokenType.PAREN_R)){
            	panicMode(Token.TokenType.PAREN_R, Token.TokenType.SEMICOLON);
            	accept(Token.TokenType.PAREN_R);
            }

            if(!expect(TokenType.SEMICOLON)){
            	panicMode(Token.TokenType.SEMICOLON, Token.TokenType.FIM,
            			Token.TokenType.IDENTIFIER, Token.TokenType.SE,
            			Token.TokenType.ENQUANTO, Token.TokenType.ESCREVA,
            			Token.TokenType.LEIA);
            }

            corpoBloco();
        }
        // <Leia> ::= 'leia''('<Leia_Params>')'';'
        else if (accept(TokenType.LEIA)) {

        	if(!expect(TokenType.PAREN_L)){
            	panicMode(Token.TokenType.PAREN_L, Token.TokenType.IDENTIFIER);
            	accept(Token.TokenType.PAREN_L);
            }

            leiaParams();

            if(!expect(TokenType.PAREN_R)){
            	panicMode(Token.TokenType.PAREN_R, Token.TokenType.SEMICOLON);
            	accept(Token.TokenType.PAREN_R);
            }

            if(!expect(TokenType.SEMICOLON)){
            	panicMode(Token.TokenType.SEMICOLON, Token.TokenType.FIM,
            			Token.TokenType.IDENTIFIER, Token.TokenType.SE,
            			Token.TokenType.ENQUANTO, Token.TokenType.ESCREVA,
            			Token.TokenType.LEIA);
            }

            corpoBloco();
        } // Se não cair em nenhuma das condições acima, significa que corpobloco derivou vazio

        else if(currentToken.getType() != Token.TokenType.FIM){ //se nao for vazio entra aqui
        	syntaxError(currentToken.getType()); //nao podia usar o accept pq nao pode consumir o FIM
        	panicMode(Token.TokenType.IDENTIFIER, Token.TokenType.SE,
        			Token.TokenType.ENQUANTO, Token.TokenType.LEIA,
        			Token.TokenType.ESCREVA);
        	corpoBloco();
        } // o else eh o vazio

    }

    // <Escreva_Params> ::= numero_t<Escreva_Param2> | caractere_t<Escreva_Param2> | cadeia_t<Escreva_Param2>
    private void escrevaParams() {
        // Usando numero no lugar de expressão aritmética
        if (accept(TokenType.NUMBER)) escrevaParams2();
        else if (accept(TokenType.CHARACTER)) escrevaParams2();
        else if (accept(TokenType.CHAR_STRING)) escrevaParams2();
        else syntaxError(TokenType.NUMBER, TokenType.CHARACTER, TokenType.CHAR_STRING);
    }

    // <Escreva_Param2> ::= ','<Escreva_Params> | <>
    private void escrevaParams2() {
        if (accept(TokenType.COMMA)) {
            escrevaParams();
        }
    }

    // <Leia_Params> ::= <Id_Vetor><Leia_Param2>
    private void leiaParams() {
        Token t = idvetor().getToken();
        // gets so ocorrem depois da primeira leitura
        if (!firstRun) {
            Variable v = (Variable) getSymbol(t);
            if (v != null && v.isConstant()) constantAssignmentError(t);
        }
        leiaParam2();
    }

    // <Leia_Param2> ::= ','<Leia_Params> | <>
    private void leiaParam2() {
        if (accept(TokenType.COMMA)) {
            leiaParams();
        }
    }

    // <Atribuicao> ::= <Id_Vetor>'<<'<Valor>';'
    private void atribuicao() {
        // simbolo que contém o token atual. Pode não estar na tabela
        Symbol tokenSymbol = idvetor();
        Symbol tableSymbol = null;
        vecAtrib = false;
        boolean atribAllowed = true;

        if (!firstRun) {
            tableSymbol = getSymbol(tokenSymbol.getToken());
            // vê se o tokenSymbol existe na tabela
            if (tableSymbol != null) {
                if ( tableSymbol instanceof  Variable && ((Variable)tableSymbol).isConstant()) {
                    // tem que ser o tokenSymbol aqui, pois ele contém as informações da linha atual
                    atribAllowed = false;
                    constantAssignmentError(tokenSymbol.getToken());
                } else if (tableSymbol instanceof Vector) {
                    if (!(tokenSymbol instanceof Vector)) vecAtrib = true;
                    else if (((Vector)tableSymbol).getDimensions() != ((Vector)tokenSymbol).getDimensions())
                        vectDimensionError((Vector)tableSymbol, (Vector)tokenSymbol);
                }

                // currentType vai conter o tipo esperado para atribuição
                currentType = tableSymbol.getType();
            } else atribAllowed = false;
        }
        if(!expect(TokenType.ATRIB)){
        	panicMode(Token.TokenType.ATRIB, Token.TokenType.NUMBER,
        			Token.TokenType.IDENTIFIER, Token.TokenType.PAREN_L,
        			Token.TokenType.BOOL_V, Token.TokenType.CHAR_STRING,
        			Token.TokenType.CHARACTER);
        	accept(Token.TokenType.ATRIB);
        }

        if (vecAtrib) {
            int i;

            for (i = 0; !lookAheadToken(i, TokenType.SEMICOLON); i++);

            if (i > 1) {
                logSemanticAnalysis(String.format(
                        "Erro na linha %d: valores soh podem ser atribuidos a posicoes especificas de um vetor.",
                        tokenSymbol.getToken().getLine()));
                vecAtrib = false;
            }
        }

        Symbol.Type actual = valor();
        if (!firstRun && atribAllowed && currentType != actual) mismatchedTypeError(previousToken.getLine(), currentType, actual);

        if (vecAtrib) {
            Symbol vec = getSymbol(previousToken);

            if (vec != null) {
                if (!(vec instanceof Vector) || ((vec instanceof Vector) &&
                        (((Vector)tableSymbol).getDimensions() != ((Vector)vec).getDimensions())))
                    logSemanticAnalysis(String.format(
                            "Erro na linha %d: a variavel \"%s\" nao eh um vetor %d-dimensional.",
                            previousToken.getLine(), previousToken.getLexeme(), ((Vector)tableSymbol).getDimensions())
                    );
            } else if (previousToken.getType() == TokenType.NUMBER || previousToken.getType() == TokenType.BOOL_V)
                logSemanticAnalysis(String.format(
                    "Erro na linha %d: valores soh podem ser atribuidos a posicoes especificas de um vetor.",
                    previousToken.getLine())
                );
        }

        if(!expect(TokenType.SEMICOLON)){
        	panicMode(Token.TokenType.IDENTIFIER, Token.TokenType.SEMICOLON,
        			Token.TokenType.ENQUANTO, Token.TokenType.SE,
        			Token.TokenType.LEIA, Token.TokenType.ESCREVA);
        	accept(Token.TokenType.SEMICOLON);
        }
    }

    // <Funcoes>::= <Funcao_Decl><Funcoes>|<>
    private void funcoes() {
        if (currentToken.getType() == TokenType.FUNCAO) {
            funcaoDecl();
            funcoes();
        }
    }

    // <Funcao_Decl> ::= 'funcao'<Funcao_Decl2>
    private void funcaoDecl() {
        expect(TokenType.FUNCAO);
        funcaoDecl2();
    }

    // <Funcao_Decl2>::= <Tipo>id'('<Param_Decl>')'<Bloco> | id'('<Param_Decl>')'<Bloco>
    private void funcaoDecl2() {
        Token identifier = null;
        Symbol.Type t;
        if (tipo()) // trata o caso da função ser void
            t = Symbol.Type.valueOf(previousToken.getLexeme().toUpperCase());
        else t = Symbol.Type.VOID;

        if(!expect(TokenType.IDENTIFIER)){ // Mas o identificador é obrigatório
            panicMode(Token.TokenType.IDENTIFIER, Token.TokenType.PAREN_L);
            accept(Token.TokenType.IDENTIFIER);
        } else identifier = previousToken;
        if(!expect(TokenType.PAREN_L)){
            panicMode(Token.TokenType.PAREN_L, Token.TokenType.PAREN_R,
                    Token.TokenType.IDENTIFIER, Token.TokenType.INTEIRO,
                    Token.TokenType.BOOLEANO, Token.TokenType.CADEIA,
                    Token.TokenType.REAL, Token.TokenType.CARACTERE);
        }

        // Argumentos serão adcionados ao escopo do bloco que segue a declaração de uma função
        LinkedList<Symbol> args = paramDecl();
        // Se a função tiver tipo de retorno, adciona a variável de retorno ao escopo
        if (t != Symbol.Type.VOID) args.add(new Variable(identifier, t));

        if(!expect(TokenType.PAREN_R)){
            panicMode(Token.TokenType.PAREN_R, Token.TokenType.INICIO);
            accept(Token.TokenType.PAREN_R);
        }

        // adiciona os parametros e a variável de retorno ao escopo do bloco
        if (!firstRun) bloco(args.toArray(new Symbol[args.size()]));
        else bloco();

        // remove a variavel de retorno da lista. Sobram apenas os argumentos da função
        if (t != Symbol.Type.VOID) args.removeLast();

        // adciona os argumentos da função ao objeto Function
        Function f = new Function(identifier, t, args.toArray(new Symbol[args.size()]));
        if (firstRun) putSymbol(f);
    }

    //<Param_Decl> ::=  <tipo><Id_Vetor><Param_Decl_List> | <>
    // Modifiquei removendo produções unitárias
    private LinkedList<Symbol> paramDecl() {
        LinkedList<Symbol> args = new LinkedList<>();
        // se o token atual for o fecha parentese, param_decl derivou vazio
        if (currentToken.getType() == TokenType.PAREN_R) return args;
        else {
            if (!tipo()) {
                syntaxError(TokenType.INTEIRO, TokenType.REAL,
                        TokenType.BOOLEANO, TokenType.CARACTERE,
                        TokenType.CADEIA);
            }
            args.add(idvetor());
            args.addAll(paramDeclList());
            return args;
        }
    }

    // <Param_Decl_List> ::=  ','<Param_Decl>|<>
    private LinkedList<Symbol> paramDeclList() {
        LinkedList<Symbol> args = new LinkedList<>();
        if (accept(TokenType.COMMA)) {
            args.addAll(paramDecl());
        }
        return args;
    }

    // <Chamada_Funcao>::= id '(' <Chamada_Funcao2>
    private Symbol.Type chamadaFuncao() {
        if(!expect(TokenType.IDENTIFIER)){
            panicMode(Token.TokenType.IDENTIFIER, Token.TokenType.PAREN_L);
            accept(Token.TokenType.IDENTIFIER);
        }
        Function f = null;
        int line = previousToken.getLine();
        if (!firstRun) {
            Symbol s = getSymbol(previousToken, true);

            if (s != null) {
                if (!(s instanceof Function))
                    logSemanticAnalysis(String.format("Erro na linha %d: nao foi possivel encontrar a funcao %s.",
                            line, previousToken.getLexeme()));
                else f = (Function) s;
            }
        }
        if(!expect(TokenType.PAREN_L)){
            panicMode(Token.TokenType.PAREN_L, Token.TokenType.IDENTIFIER,
                    Token.TokenType.NUMBER, Token.TokenType.CARACTERE,
                    Token.TokenType.BOOL_V, Token.TokenType.CHAR_STRING);
            accept(Token.TokenType.PAREN_L);
        }

        LinkedList<Symbol.Type> argTypes = chamadaFuncao2();

        if (f != null) {

            if (f.getArgCount() == 0 && f.getArgCount() != argTypes.size()) {
                logSemanticAnalysis(String.format("Erro na linha %d: a funcao %s nao recebe argumentos.",
                        line, f.getToken().getLexeme()));
                return Symbol.Type.VOID;
            }

            boolean error = false;
            int i;
            String msg =
                    String.format("Erro na linha %d: a funcao %s nao pode ser aplicada aos seguintes argumentos:\n" +
                                    "Parametros Esperados\tArgumentos Obtidos\n",
                            line, f.getToken().getLexeme());

            for (i = 0; i < f.getArgCount(); i++) {
                msg += (i < argTypes.size()) ? f.getArg(i).getType().name() + "\t" + argTypes.get(i).name() + "\n"
                        : f.getArg(i).getType().name() + "\n";
                error = error ||
                        ((i >= argTypes.size()) || ((i < argTypes.size()) && f.getArg(i).getType() != argTypes.get(i)));
            }

            // se entrar aqui, o cara passou mais argumentos que devia
            for (int j = i; j < argTypes.size(); j++) {
                msg += "\t\t" + argTypes.get(j).name();
                error = true;
            }

            if (error) logSemanticAnalysis(msg);
            return f.getType();
        }
        return Symbol.Type.VOID;
    }

    void a(int a, int b){}

    // <Chamada_Funcao2>::=<Param_Cham>')' |  ')'
    private LinkedList<Symbol.Type> chamadaFuncao2() {
        LinkedList<Symbol.Type> argTypes = new LinkedList<>();
        if (!accept(TokenType.PAREN_R)) {
            if (!firstRun) argTypes.addAll(paramCham()); else paramCham();
            if(!expect(TokenType.PAREN_R)){
                panicMode(Token.TokenType.PAREN_R, Token.TokenType.SEMICOLON,
                        Token.TokenType.MINUS, Token.TokenType.DIV,
                        Token.TokenType.PLUS, Token.TokenType.TIMES);
                accept(Token.TokenType.PAREN_R);
            }
        }
        return argTypes;
    }

    // <Param_Cham> ::= <Literal> <Param_Cham2> | <ID_Vetor> <Param_Cham2>
    // simplificado
    private LinkedList<Symbol.Type> paramCham() {
        LinkedList<Symbol.Type> argTypes = new LinkedList<>();

        Token t = null;
        Symbol.Type type = null;
        if (currentToken.getType() == TokenType.IDENTIFIER) {
            t = idvetor().getToken();
        } else type = literal();
        LinkedList<Symbol.Type> types = paramCham2();

        if (!firstRun) {
            if (t != null) {
                Symbol s = getSymbol(t);
                if (s != null) argTypes.add(s.getType());
            } else argTypes.add(type);

            argTypes.addAll(types);
        }
        return argTypes;
    }

    // <Param_Cham2>::= ','<Param_Cham>|<>
    private LinkedList<Symbol.Type> paramCham2() {
        LinkedList<Symbol.Type> argTypes = new LinkedList<>();
        if (accept(TokenType.COMMA)) {
            if (!firstRun) argTypes.addAll(paramCham()); else paramCham();
        }
        return argTypes;
    }

    // <Valor> ::= <Exp_Aritmetica> | <Exp_Logica> | caractere_t | cadeia_t
    private Symbol.Type valor() {
        if (accept(TokenType.CHAR_STRING)){
            if (vecAtrib) logSemanticAnalysis(String.format(
                    "Erro na linha %d: valores soh podem ser atribuidos a posicoes especificas de um vetor.",
                    previousToken.getLine())
            );
            else if (!firstRun && currentType != Symbol.Type.CADEIA)
                mismatchedTypeError(previousToken.getLine(), currentType, Symbol.Type.CADEIA);
            return Symbol.Type.CADEIA;
        } else if (accept(TokenType.CHARACTER)){
            if (vecAtrib) logSemanticAnalysis(String.format(
                    "Erro na linha %d: valores soh podem ser atribuidos a posicoes especificas de um vetor.",
                    previousToken.getLine())
            );
            else if (!firstRun && currentType != Symbol.Type.CARACTERE)
                mismatchedTypeError(previousToken.getLine(), currentType, Symbol.Type.CARACTERE);
            return Symbol.Type.CARACTERE;
        } else {
            // Procura por um operador lógico. Se encontrar, chama expressão lógica
            boolean logOpFound = false;

            // Lista de operadores lógicos e relacionais
            List<TokenType> logRelOpList = Arrays.asList(TokenType.EQ, TokenType.NEQ,
                    TokenType.LT, TokenType.LE, TokenType.GT, TokenType.GE, TokenType.E,
                    TokenType.NAO, TokenType.OU);

            // pare se encontrar qualquer um desses tokens
            List<TokenType> stopToken = Arrays.asList(TokenType.SEMICOLON, TokenType.FIM,
                    TokenType.SE, TokenType.ENQUANTO, TokenType.LEIA, TokenType.ESCREVA);

            logOpFound = searchForTokens(logRelOpList, stopToken);

            if (logOpFound) {
                return expLogica();
            } else {
                return expAritimetica();
            }
        }
    }

    private boolean searchForTokens(List<TokenType> targetTokens, List<TokenType> stopTokens) {
        for (int i = 1; i < tokenList.size() ; i++) {

            for (TokenType s : stopTokens) {
                if (lookAheadToken(i, s)) return false;
            }

            for (TokenType t : targetTokens) {
                if (lookAheadToken(i, t)) return true;
            }
        }
        return false;
    }

    // <Exp_Aritmetica> ::= <Exp_A1> | <Exp_A1><Exp_SomSub>
    // primeiro(<Exp_Aritmetica>) = {'(', numero_t, id}
    private Symbol.Type expAritimetica() {
        Symbol.Type t1 = expA1();
        if (currentToken.getType() == TokenType.PLUS ||
                currentToken.getType() == TokenType.MINUS) {
            Token op = currentToken;
            if (!firstRun) typeCheckArithmeticOperator(op, t1);
            Symbol.Type t2 = expSomaSub();
            if (!firstRun && typeCheckArithmeticOperator(op, t2))
                return realOrInteger(t1, t2);
        }
        return t1;
    }

    // <Exp_A1> ::= <Numerico_Funcao> | <Numerico_Funcao><Exp_MulDiv>
    // primeiro(<Exp_A1>) = primeiro(<Numerico_Funcao>) = {'(', numero_t, id}
    private Symbol.Type expA1() {
        Symbol.Type t1 = numericoFuncao();
        if (currentToken.getType() == TokenType.TIMES ||
                currentToken.getType() == TokenType.DIV) {
            Token op = currentToken;
            if (!firstRun) typeCheckArithmeticOperator(op, t1);
            Symbol.Type t2 = expMulDiv();
            if (!firstRun && typeCheckArithmeticOperator(op, t2))
                return realOrInteger(t1, t2);
        }
        return t1;
    }

    private Symbol.Type realOrInteger(Symbol.Type t1, Symbol.Type t2) {
        return (t1 == Symbol.Type.REAL || t2 == Symbol.Type.REAL) ? Symbol.Type.REAL : Symbol.Type.INTEIRO;
    }

    // <Exp_SomSub> ::= <Operador_A1><Exp_A1> | <Operador_A1><Exp_A1><Exp_SomSub>
    // Primeiro(<Exp_SomSub>) = {'+', '-'}
    private Symbol.Type expSomaSub() {
        Token op = operadorA1();
        Symbol.Type t1 = expA1();
        if (!firstRun) typeCheckArithmeticOperator(op, t1);
        if (currentToken.getType() == TokenType.PLUS ||
                currentToken.getType() == TokenType.MINUS)
            return expSomaSub();
        return t1;
    }

    // <Operador_A1> ::= '+' | '-'
    private Token operadorA1() {
        if (accept(TokenType.PLUS)) return previousToken;
        else if (accept(TokenType.MINUS)) return previousToken;
        else syntaxError(TokenType.PLUS, TokenType.MINUS);
        return null;
    }

    // <Exp_MulDiv> ::= <Operador_A2><Numerico_Funcao>| <Operador_A2><Numerico_Funcao><Exp_MulDiv>
    // Primeiro(<Exp_MulDiv>) = {'*' | '/'}
    private Symbol.Type expMulDiv() {
        Token op = operadorA2();
        Symbol.Type t1 = numericoFuncao();
        if (!firstRun) typeCheckArithmeticOperator(op, t1);

        if (currentToken.getType() == TokenType.TIMES ||
                currentToken.getType() == TokenType.DIV)
            return expMulDiv();
       return t1;
    }

    private boolean typeCheckArithmeticOperator(Token op, Symbol.Type t1) {
        if (t1 != Symbol.Type.REAL && t1 != Symbol.Type.INTEIRO) {
            logSemanticAnalysis(String.format("Erro na linha %d: %s nao pode ser usado com o tipo %s.",
                    op.getLine(), op.getType().toString(), t1.name()));
            return false;
        } return true;
    }

    // <Operador_A2> ::= '*' | '/'
    private Token operadorA2() {
        if (accept(TokenType.TIMES)) return previousToken;
        else if (accept(TokenType.DIV)) return previousToken;
        else syntaxError(TokenType.TIMES, TokenType.DIV);
        return null;
    }

    // <Numerico_Funcao> ::= <Valor_Numerico> | <Vetor_Funcao>
    // Primeiro(<Numerico_Funcao>) = {'(', numero_t, id}
    private Symbol.Type numericoFuncao() {
        if (currentToken.getType() == TokenType.NUMBER ||
                currentToken.getType() == TokenType.PAREN_L) {
            return valorNumerico();
        } else return vetorFuncao();
    }

    // <Valor_Numerico> ::= '('<Exp_Aritmetica>')' | numero_t
    // primeiro(<Valor_Numerico>) = {'(', numero_t}
    private Symbol.Type valorNumerico() {
        if (accept(TokenType.PAREN_L)) {
            Symbol.Type t = expAritimetica();
            if(!expect(TokenType.PAREN_R)){
            	panicMode(Token.TokenType.SEMICOLON, Token.TokenType.PLUS,
            			Token.TokenType.MINUS, Token.TokenType.DIV,
            			Token.TokenType.TIMES);
            }
            return t;
        } else if(!expect(TokenType.NUMBER)){
        	panicMode(Token.TokenType.SEMICOLON, Token.TokenType.PLUS,
        			Token.TokenType.MINUS, Token.TokenType.DIV,
        			Token.TokenType.TIMES);
            return Symbol.Type.VOID;
        }  else return (previousToken.getLexeme().contains(".")) ? Symbol.Type.REAL : Symbol.Type.INTEIRO;
    }

    // <Vetor_Funcao> ::= <Id_Vetor> | <Chamada_Funcao>
    // primeiro(<Vetor_Funcao>) = {id}
    private Symbol.Type vetorFuncao() {
        if (lookAheadToken(1, TokenType.PAREN_L)) return chamadaFuncao();
        else {
            Token t = idvetor().getToken();
            Symbol s;
            return (!firstRun && (s = getSymbol(t)) != null) ? s.getType() : null;
        }
    }

    /* <Exp_Logica> ::= <Vetor_Funcao><Operador_L1><Vetor_Funcao><Exp_Logica2> |
     *                  <Vetor_Funcao><Operador_L1><Valor_Booleano> |
     *                  <Valor_Booleano><Operador_L1><Exp_Logica> |
     *                  <Operador_L2><X4><Exp_Logica2> |
     *                  <Valor_Booleano>
     *  primeiro(<Exp_Logica>) = {'nao', '(', numero_t, id, caractere_t, cadeia_t, booleano_t}
     */
    private Symbol.Type expLogica() {
        if (currentToken.getType() == TokenType.NAO) {
            Token op = currentToken;
            operadorL2();
            Symbol.Type t1 = x4();
            typeCheckBoolOperator(op, t1);
            return (expLogica2() && t1 == Symbol.Type.BOOLEANO)? Symbol.Type.BOOLEANO : Symbol.Type.VOID;
        } else {
            if (lookAheadToken(1, TokenType.PAREN_L) || lookAheadToken(1, TokenType.VEC_DELIM_L)) {
                Symbol.Type t2, t1 = vetorFuncao();
                Token op = operadorL1();
                boolean ok = typeCheckBoolOperator(op, t1);

                if (currentToken.getType() == TokenType.IDENTIFIER) {
                    t2 = vetorFuncao();
                    return (expLogica2() && ok && t2 == Symbol.Type.BOOLEANO) ? Symbol.Type.BOOLEANO : Symbol.Type.VOID;
                } else {
                    t2 = valorBooleano();
                    return (ok && t2 == Symbol.Type.BOOLEANO) ? Symbol.Type.BOOLEANO : Symbol.Type.VOID;
                }
            } else {
                Symbol.Type t1 = valorBooleano();
                if (currentToken.getType() == TokenType.E ||
                        currentToken.getType() == TokenType.OU) {
                    Token op = operadorL1();
                    boolean ok = typeCheckBoolOperator(op, t1);
                    Symbol.Type t2 = expLogica();
                    return (ok && typeCheckBoolOperator(op, t2)) ? Symbol.Type.BOOLEANO : Symbol.Type.VOID;
                }
                return t1;
            }
        }
    }

    private boolean typeCheckBoolOperator(Token op, Symbol.Type t1) {
        if (t1 != Symbol.Type.BOOLEANO) {
            if (!firstRun)logSemanticAnalysis(String.format("Erro na linha %d: %s nao pode ser usado com o tipo %s.",
                    op.getLine(), op.getType().toString(), t1.name()));
            return false;
        } return true;
    }

    // <Exp_Logica2> ::= <Operador_L1><Exp_Logica3> | <>
    private boolean expLogica2() {
        if (currentToken.getType() == TokenType.E ||
                currentToken.getType() == TokenType.OU) {
            Token op = operadorL1();
            Symbol.Type t1 = expLogica3();
            return typeCheckBoolOperator(op, t1);
        } // vazio
        return true;
    }

    // <Exp_Logica3> ::= <X5><Exp_Logica2> | <Operador_L2><X4><Exp_Logica2>
    private Symbol.Type expLogica3() {
        if (currentToken.getType() == TokenType.NAO) {
            Token op = currentToken;
            operadorL2();
            Symbol.Type t1 = x4();
            boolean ok = typeCheckBoolOperator(op, t1);

            return (expLogica2() && ok) ? t1 : Symbol.Type.VOID;
        } else {
            Symbol.Type t1 = x5();
            return (expLogica2() && t1 == Symbol.Type.BOOLEANO) ? Symbol.Type.BOOLEANO: Symbol.Type.VOID;
        }
    }

    // <X4> ::= '('<Vetor_Funcao>')' | <Valor_Booleano>
    // primeiro(<X4>) = {'(', numero_t, id, caractere_t, cadeia_t, booleano_t}
    private Symbol.Type x4() {
        if (currentToken.getType() == TokenType.PAREN_L){
            if (lookAheadToken(2, TokenType.VEC_DELIM_L) ||
                    lookAheadToken(2, TokenType.PAREN_L)){
                expect(TokenType.PAREN_L);
                Symbol.Type t1 = vetorFuncao();
                expect(TokenType.PAREN_R);
                return t1;
            }
            return null;
        } else return valorBooleano();
    }

    // <X5> ::= <X4> | <Vetor_Funcao>
    // primeiro(<X5>) = {'(', numero_t, id, caractere_t, cadeia_t, booleano_t, id}
    private Symbol.Type x5() {
        if (accept(TokenType.IDENTIFIER)) {
            return vetorFuncao();
        } else return x4();
    }

    // <Operador_L1> ::= 'e' | 'ou'
    private Token operadorL1() {
        if (accept(TokenType.E)) return previousToken;
        else if (accept(TokenType.OU)) return previousToken;
        else syntaxError(TokenType.E, TokenType.OU); return null;
    }

    // <Operador_L2> ::= 'nao'
    private void operadorL2() {
        expect(TokenType.NAO);
    }

    // <Valor_Booleano> ::= '('<Exp_Logica>')' | <Exp_Relacional> |  booleano_t
    // primeiro(<Valor_Booleano>) = {'(', numero_t, id, caractere_t, cadeia_t, booleano_t}
    private Symbol.Type valorBooleano() {
        // Como decidir entre as produções?
        if (!accept(TokenType.BOOL_V)){
            // Ambas as produções restantes podem começar com varios (
            if (accept(TokenType.PAREN_L)) {
                Symbol.Type t1 = expLogica();
                expect(TokenType.PAREN_R);
                return t1;
            } else if (accept(TokenType.BOOL_V))return Symbol.Type.BOOLEANO;
            else return expRelacional();
        } else return Symbol.Type.BOOLEANO;
    }

    /* <Exp_Relacional> ::= <Numerico_Funcao><Operador_R1><Numerico_Funcao> |
     *                      <Literal_Numero><Operador_R2><Literal_Numero>
     * primeiro(<Exp_Relacional>) = {'(', numero_t, id, caractere_t, cadeia_t, booleano_t}
     */
    private Symbol.Type expRelacional() {
        if (currentToken.getType() == TokenType.NUMBER ||
                currentToken.getType() == TokenType.PAREN_L ||
                currentToken.getType() == TokenType.IDENTIFIER) {
            Symbol.Type t1 = numericoFuncao();
            Token op = operadorR1();
            Symbol.Type t2 = numericoFuncao();
            return (!firstRun && typeCheckRelop(op, t1, t2)) ? Symbol.Type.BOOLEANO : Symbol.Type.VOID;
        } else {
            Symbol.Type t1 = literalNumero();
            Token op = operadorR2();
            Symbol.Type t2 = literalNumero();
            return (!firstRun && typeCheckRelop(op, t1, t2)) ? Symbol.Type.BOOLEANO : Symbol.Type.VOID;
        }
    }

    private boolean typeCheckRelop(Token op, Symbol.Type t1, Symbol.Type t2) {
        if (t1 == t2) {
            if (t1 == Symbol.Type.CADEIA || t1 == Symbol.Type.CARACTERE || t1 == Symbol.Type.BOOLEANO)
                return (op.getType() == TokenType.EQ || op.getType() == TokenType.NEQ);
            else logSemanticAnalysis(String.format("Erro na linha %d: %s nao pode ser usado com o tipo %s.",
                    op.getLine(), op.getType().toString(), t1.name()));
            return true;
        } else if (((t1 == Symbol.Type.REAL && t2 == Symbol.Type.INTEIRO) ||
                (t1 == Symbol.Type.INTEIRO && t2 == Symbol.Type.REAL))) {
            return true;
        }
        logSemanticAnalysis(String.format("Erro na linha %d: nao eh possivel comparar %s com %s.",
                op.getLine(), t1.name(), t2.name()));
        return false;
    }

    // <Operador_R1> ::= <Operador_R2> | '<' | '<=' | '>' | '>='
    // Primeiro(<Operador_R1>) = {'=', '<>', '<', '<=', '>', '>='}
    private Token operadorR1() {
        if (accept(TokenType.NEQ)) return previousToken;
        else if (accept(TokenType.EQ)) return previousToken;
        else if (accept(TokenType.LT)) return previousToken;
        else if (accept(TokenType.LE)) return previousToken;
        else if (accept(TokenType.GT)) return previousToken;
        else if (accept(TokenType.GE)) return previousToken;
        else syntaxError(TokenType.NEQ, TokenType.EQ, TokenType.LT,
                    TokenType.LE, TokenType.GE, TokenType.GT);
        return null;
    }

    // <Operador_R2> ::= '<>' | '='
    // Primeiro(<Operador_R2>) = {'=', '<>'}
    private Token operadorR2() {
        if (accept(TokenType.NEQ)) return previousToken;
        else if (accept(TokenType.EQ)) return previousToken;
        else syntaxError(TokenType.NEQ, TokenType.EQ);
        return null;
    }

    // <Literal_Numero> ::= caractere_t | cadeia_t | booleano_t
    // literal menos numero?
    // primeiro(<Literal_Numero>) = {caractere_t, cadeia_t, booleano_t}
    private Symbol.Type literalNumero() {
        if (accept(TokenType.CHARACTER)) return Symbol.Type.CARACTERE;
        else if (accept(TokenType.CHAR_STRING)) return Symbol.Type.CADEIA;
        else if (accept(TokenType.BOOL_V)) return Symbol.Type.BOOLEANO;
        else syntaxError(TokenType.CHAR_STRING, TokenType.CHARACTER, TokenType.BOOL_V);
        return null;
    }

    // <Tipo> ::= 'inteiro' | 'real' | 'booleano' | 'cadeia' | 'caractere'
    private boolean tipo() { // mudar pra tipo boolean?
        boolean ok = false;
        if (accept(Token.TokenType.INTEIRO)) ok = true;
        if (accept(Token.TokenType.REAL)) ok = true;
        if (accept(Token.TokenType.CARACTERE)) ok = true;
        if (accept(Token.TokenType.CADEIA)) ok = true;
        if (accept(Token.TokenType.BOOLEANO)) ok = true;
        if (ok)
            currentType = Symbol.Type.valueOf(previousToken.getLexeme().toUpperCase());
        return ok;
    }

    // <Literal> ::= caractere_t | cadeia_t | numero_t | booleano_t
    private Symbol.Type literal() {
        if (accept(Token.TokenType.NUMBER)) {
            if (previousToken.getLexeme().contains(".")) return Symbol.Type.REAL;
            else return Symbol.Type.INTEIRO;
        }
        else if (accept(Token.TokenType.CHARACTER)) return Symbol.Type.CARACTERE;
        else if (accept(Token.TokenType.CHAR_STRING)) return Symbol.Type.CADEIA;
        else if (accept(Token.TokenType.BOOL_V)) return Symbol.Type.BOOLEANO;
        else syntaxError(TokenType.NUMBER, TokenType.CHAR_STRING,
                    TokenType.CHARACTER, TokenType.BOOL_V);
        return null;
    }
}
