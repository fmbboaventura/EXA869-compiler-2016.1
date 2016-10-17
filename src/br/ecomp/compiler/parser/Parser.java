package br.ecomp.compiler.parser;

import br.ecomp.compiler.lexer.Token;
import br.ecomp.compiler.lexer.Token.TokenType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Filipe Boaventura
 * @since 24/09/2016.
 */
public class Parser {
    /**
     * Ponteiro para o simbolo atual da entrada.
     */
    private Token currentToken;
    private List<Token> tokenList;
    private int index;
    private int errorCount;
    private BufferedWriter writer;

    /**
     * Inicia a análise sintática sobre a coleção de
     * {@link Token}s.
     * @param tokens coleção de entrada contendo os
     * {@link Token}s para a análise sintática.
     */
    public void parse(List<Token> tokens, String outputPath) throws IOException {
        errorCount = 0;
        tokenList = tokens;
        index = -1;
        writer = new BufferedWriter(new FileWriter(new File(outputPath)));

        System.out.println("Passo 2: Analise Sintatica");
        programa();
        System.out.println(String.format("\t%d erros sintáticos foram encontrados", errorCount));
        writer.write(String.format("%d erros sintáticos foram encontrados", errorCount));
        writer.newLine();
        if (errorCount == 0) {
            System.out.println("\tAnalise Sintatica concluida com sucesso.");
            writer.write("Analise Sintatica concluida com sucesso.");
            writer.newLine();
        }
        writer.close();
        System.out.println("O status da analise foi salvo no arquivo " + outputPath);
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
            currentToken = tokenList.get(index);
            accept(Token.TokenType.COMMENT); // Pulando comentarios
            System.out.println("Token Atual: " + currentToken.toString());
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
        error(type);
        return false;
    }

    private boolean lookAheadToken(int n, TokenType type) {
        if (index + n < tokenList.size()) {
            return tokenList.get(index + n).getType() == type;
        } return false;
    }

    private void error(TokenType... expected) {
        if (expected.length == 0)
            throw new IllegalArgumentException("informe pelo menos um TokenType esperado");
        errorCount++;
        String expectedTokenNames = "";

        for (int i = 0; i < expected.length; i++) {
            expectedTokenNames += expected[i].toString();
            if (i < expected.length-1)
                expectedTokenNames += ", ";
        }
        String errorMsg = String.format("Erro na linha %d. Esperava: %s. Obteve: %s.",
                currentToken.getLine(), expectedTokenNames, currentToken.getLexeme()
                        + " " + currentToken.getType());
        System.out.println(errorMsg);
        try {
            writer.write(errorMsg);
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
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

	/******************************************
     *            Nao-Terminais
     *****************************************/

    // <Programa> ::= <Variaveis><C>|<C>
    private void programa() {
        nextToken();
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
        	error(currentToken.getType()); //nao podia usar o accept pq nao pode consumir o FIM
        	panicMode(Token.TokenType.IDENTIFIER);
        	constdecl();
        	constlist();
        } // o else eh o vazio
    }

    // <Const_Decl> ::= id'<<'<Literal><Const_Decl2>
    private void constdecl() {
        
        
        if(!expect(Token.TokenType.IDENTIFIER)){
        	panicMode(Token.TokenType.ATRIB, Token.TokenType.IDENTIFIER);
        	accept(TokenType.IDENTIFIER);
        }
        
        if(!expect(Token.TokenType.ATRIB)){
        	panicMode(Token.TokenType.ATRIB, Token.TokenType.NUMBER,
        			Token.TokenType.CHARACTER, Token.TokenType.CHAR_STRING,
        			Token.TokenType.BOOL_V);
        	accept(Token.TokenType.ATRIB);
        }
        literal();
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
        else if(currentToken.getType() != Token.TokenType.FIM){ //se nao for vazio entra aqui
        	error(currentToken.getType()); //nao podia usar o accept pq nao pode consumir o FIM
        	panicMode(Token.TokenType.IDENTIFIER);
        	vardecl();
        	varlist();
        } // o else eh o vazio
    }

    // <Var_Decl> ::= <Id_Vetor>','<Var_Decl> | <Id_Vetor>';' AMBIGUIDADE! Fatorar a esquerda!
    private void vardecl() {
        idvetor();
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
    private void idvetor() {
        if(!expect(Token.TokenType.IDENTIFIER)){
        	panicMode(Token.TokenType.COMMA, Token.TokenType.SEMICOLON,
        			Token.TokenType.IDENTIFIER);
        	accept(TokenType.IDENTIFIER);
        }
        vetor();
    }

    // <Vetor> ::= '<<<'numero_t<Vetor2>'>>>'  | <>
    // vetor simplificado.
    // Vetor original:
    // <Vetor> ::= '<<<'<Exp_Aritmetica><Vetor2>'>>>'  | <>
    // <Vetor2> ::= ','<Exp_Aritmetica><Vetor2> | <>
    private void vetor() {
        if (accept(Token.TokenType.VEC_DELIM_L)) {//se encontrou <<<
            expAritimetica(); // TODO implementar as expressoes
            vetor2();
            if(!expect(Token.TokenType.VEC_DELIM_R)){ // espera que feche o vetor com >>>
            	panicMode(Token.TokenType.VEC_DELIM_R, Token.TokenType.IDENTIFIER,
            			Token.TokenType.COMMA, Token.TokenType.SEMICOLON);
            	accept(TokenType.VEC_DELIM_R);
            }
        }
    }

    // <Vetor2> ::= ','<Exp_Aritmetica><Vetor2> | <>
    private void vetor2() {
        if (accept(Token.TokenType.COMMA)) { // se encontrou uma virgula
            expAritimetica();
            vetor2(); // pode se repetir
        }
    }

    // <Bloco> ::= 'inicio'<Corpo_Bloco>'fim'
    // bloco simplificado por agora
    private void bloco() {
        if(!expect(Token.TokenType.INICIO)){
        	 panicMode(Token.TokenType.INICIO, Token.TokenType.FIM,
                     Token.TokenType.IDENTIFIER, Token.TokenType.ENQUANTO,
                     Token.TokenType.SE, Token.TokenType.ESCREVA,
                     Token.TokenType.LEIA);
        	accept(Token.TokenType.INICIO);
        }
        bloco2();
    }

    private void bloco2() {
        if (currentToken.getType() == TokenType.VAR){
            variaveis();
        }

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
            if(!expect(TokenType.PAREN_L)){
            	panicMode(Token.TokenType.PAREN_L, Token.TokenType.IDENTIFIER,
            			Token.TokenType.NUMBER);
            	accept(Token.TokenType.PAREN_L);
            }
            
            expLogica();
            
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
        	if(!expect(TokenType.PAREN_L)){
            	panicMode(Token.TokenType.PAREN_L, Token.TokenType.IDENTIFIER,
            			Token.TokenType.NUMBER);
            	accept(Token.TokenType.PAREN_L);
            }
        	
            expLogica();
            
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
        	error(currentToken.getType()); //nao podia usar o accept pq nao pode consumir o FIM
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
        else error(TokenType.NUMBER, TokenType.CHARACTER, TokenType.CHAR_STRING);
    }

    // <Escreva_Param2> ::= ','<Escreva_Params> | <>
    private void escrevaParams2() {
        if (accept(TokenType.COMMA)) {
            escrevaParams();
        }
    }

    // <Leia_Params> ::= <Id_Vetor><Leia_Param2>
    private void leiaParams() {
        idvetor();
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
        idvetor();
        if(!expect(TokenType.ATRIB)){
        	panicMode(Token.TokenType.ATRIB, Token.TokenType.NUMBER,
        			Token.TokenType.IDENTIFIER, Token.TokenType.PAREN_L,
        			Token.TokenType.BOOL_V, Token.TokenType.CHAR_STRING,
        			Token.TokenType.CHARACTER);
        	accept(Token.TokenType.ATRIB);
        }
        
        valor();
        
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
        tipo(); // tipo() não vai disparar erro caso não encontre um tipo
        if(!expect(TokenType.IDENTIFIER)){ // Mas o identificador é obrigatório
        	panicMode(Token.TokenType.IDENTIFIER, Token.TokenType.PAREN_L);
        	accept(Token.TokenType.IDENTIFIER);
        }
        if(!expect(TokenType.PAREN_L)){
        	panicMode(Token.TokenType.PAREN_L, Token.TokenType.PAREN_R,
        			Token.TokenType.IDENTIFIER, Token.TokenType.INTEIRO,
        			Token.TokenType.BOOLEANO, Token.TokenType.CADEIA,
        			Token.TokenType.REAL, Token.TokenType.CARACTERE);
        }
        
        paramDecl();
        
        if(!expect(TokenType.PAREN_R)){
        	panicMode(Token.TokenType.PAREN_R, Token.TokenType.INICIO);
        	accept(Token.TokenType.PAREN_R);
        }
        bloco();
    }

    //<Param_Decl> ::=  <tipo><Id_Vetor><Param_Decl_List> | <>
    // Modifiquei removendo produções unitárias
    private void paramDecl() {
        // se o token atual for o fecha parentese, param_decl derivou vazio
        if (currentToken.getType() == TokenType.PAREN_R) return;
        else {
            if (!tipo()) {
                error(TokenType.INTEIRO, TokenType.REAL,
                        TokenType.BOOLEANO, TokenType.CARACTERE,
                        TokenType.CADEIA);
            }
            idvetor();
            paramDeclList();
        }
    }

    // <Param_Decl_List> ::=  ','<Param_Decl>|<>
    private void paramDeclList() {
        if (accept(TokenType.COMMA)) {
            paramDecl();
        }
    }

    // <Chamada_Funcao>::= id '(' <Chamada_Funcao2>
    private void chamadaFuncao() {
        if(!expect(TokenType.IDENTIFIER)){
        	panicMode(Token.TokenType.IDENTIFIER, Token.TokenType.PAREN_L);
        	accept(Token.TokenType.IDENTIFIER);
        }
        if(!expect(TokenType.PAREN_L)){
        	panicMode(Token.TokenType.PAREN_L, Token.TokenType.IDENTIFIER,
        			Token.TokenType.NUMBER, Token.TokenType.CARACTERE,
        			Token.TokenType.BOOL_V, Token.TokenType.CHAR_STRING);
        	accept(Token.TokenType.PAREN_L);
        }
        chamadaFuncao2();
    }

    // <Chamada_Funcao2>::=<Param_Cham>')' |  ')'
    private void chamadaFuncao2() {
        if (!accept(TokenType.PAREN_R)) {
            paramCham();
            if(!expect(TokenType.PAREN_R)){
            	panicMode(Token.TokenType.PAREN_R, Token.TokenType.SEMICOLON,
            			Token.TokenType.MINUS, Token.TokenType.DIV,
            			Token.TokenType.PLUS, Token.TokenType.TIMES);
            	accept(Token.TokenType.PAREN_R);
            }
        }
    }

    // <Param_Cham> ::= <Literal> <Param_Cham2>
    // simplificado
    private void paramCham() {
        literal();
        paramCham2();
    }

    // <Param_Cham2>::= ','<Param_Cham>|<>
    private void paramCham2() {
        if (accept(TokenType.COMMA)) {
            paramCham();
        }
    }

    // <Valor> ::= <Exp_Aritmetica> | <Exp_Logica> | caractere_t | cadeia_t
    private void valor() {
        if (accept(TokenType.CHAR_STRING));
        else if (accept(TokenType.CHARACTER));
        else {
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

            if (logOpFound) expLogica();
            else expAritimetica();
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
    private void expAritimetica() {
        expA1();
        if (currentToken.getType() == TokenType.PLUS ||
                currentToken.getType() == TokenType.MINUS)
            expSomaSub();
    }

    // <Exp_A1> ::= <Numerico_Funcao> | <Numerico_Funcao><Exp_MulDiv>
    // primeiro(<Exp_A1>) = primeiro(<Numerico_Funcao>) = {'(', numero_t, id}
    private void expA1() {
        numericoFuncao();
        if (currentToken.getType() == TokenType.TIMES ||
                currentToken.getType() == TokenType.DIV)
            expMulDiv();
    }

    // <Exp_SomSub> ::= <Operador_A1><Exp_A1> | <Operador_A1><Exp_A1><Exp_SomSub>
    // Primeiro(<Exp_SomSub>) = {'+', '-'}
    private void expSomaSub() {
        operadorA1();
        expA1();
        if (currentToken.getType() == TokenType.PLUS ||
                currentToken.getType() == TokenType.MINUS)
            expSomaSub();
    }

    // <Operador_A1> ::= '+' | '-'
    private void operadorA1() {
        if (accept(TokenType.PLUS));
        else if (accept(TokenType.MINUS));
        else error(TokenType.PLUS, TokenType.MINUS);
    }

    // <Exp_MulDiv> ::= <Operador_A2><Numerico_Funcao>| <Operador_A2><Numerico_Funcao><Exp_MulDiv>
    // Primeiro(<Exp_MulDiv>) = {'*' | '/'}
    private void expMulDiv() {
        operadorA2();
        numericoFuncao();
        if (currentToken.getType() == TokenType.TIMES ||
                currentToken.getType() == TokenType.DIV)
            expMulDiv();
    }

    // <Operador_A2> ::= '*' | '/'
    private void operadorA2() {
        if (accept(TokenType.TIMES));
        else if (accept(TokenType.DIV));
        else error(TokenType.TIMES, TokenType.DIV);
    }

    // <Numerico_Funcao> ::= <Valor_Numerico> | <Vetor_Funcao>
    // Primeiro(<Numerico_Funcao>) = {'(', numero_t, id}
    private void numericoFuncao() {
        if (currentToken.getType() == TokenType.NUMBER ||
                currentToken.getType() == TokenType.PAREN_L) {
            valorNumerico();
        } else vetorFuncao();
    }

    // <Valor_Numerico> ::= '('<Exp_Aritmetica>')' | numero_t
    // primeiro(<Valor_Numerico>) = {'(', numero_t}
    private void valorNumerico() {
        if (accept(TokenType.PAREN_L)) {
            expAritimetica();
            if(!expect(TokenType.PAREN_R)){
            	panicMode(Token.TokenType.SEMICOLON, Token.TokenType.PLUS,
            			Token.TokenType.MINUS, Token.TokenType.DIV,
            			Token.TokenType.TIMES);
            }
        } else if(!expect(TokenType.NUMBER)){
        	panicMode(Token.TokenType.SEMICOLON, Token.TokenType.PLUS,
        			Token.TokenType.MINUS, Token.TokenType.DIV,
        			Token.TokenType.TIMES);
        }
    }

    // <Vetor_Funcao> ::= <Id_Vetor> | <Chamada_Funcao>
    // primeiro(<Vetor_Funcao>) = {id}
    private void vetorFuncao() {
        if (lookAheadToken(1, TokenType.PAREN_L)) chamadaFuncao();
        else idvetor();
    }

    /* <Exp_Logica> ::= <Vetor_Funcao><Operador_L1><Vetor_Funcao><Exp_Logica2> |
     *                  <Vetor_Funcao><Operador_L1><Valor_Booleano> |
     *                  <Valor_Booleano><Operador_L1><Exp_Logica> |
     *                  <Operador_L2><X4><Exp_Logica2> |
     *                  <Valor_Booleano>
     *  primeiro(<Exp_Logica>) = {'nao', '(', numero_t, id, caractere_t, cadeia_t, booleano_t}
     */
    private void expLogica() {
        if (currentToken.getType() == TokenType.NAO) {
            operadorL2();
            x4();
            expLogica2();
        } else {
            if (lookAheadToken(1, TokenType.PAREN_L) || lookAheadToken(1, TokenType.VEC_DELIM_L)) {
                vetorFuncao();
                operadorL1();

                if (currentToken.getType() == TokenType.IDENTIFIER) {
                    vetorFuncao();
                    expLogica2();
                } else valorBooleano();
            } else {
                valorBooleano();
                if (currentToken.getType() == TokenType.E ||
                        currentToken.getType() == TokenType.OU) {
                    operadorL1();
                    expLogica();
                }
            }
        }
    }

    // <Exp_Logica2> ::= <Operador_L1><Exp_Logica3> | <>
    private void expLogica2() {
        if (currentToken.getType() == TokenType.E ||
                currentToken.getType() == TokenType.OU) {
            operadorL1();
            expLogica3();
        } // vazio
    }

    // <Exp_Logica3> ::= <X5><Exp_Logica2> | <Operador_L2><X4><Exp_Logica2>
    private void expLogica3() {
        if (currentToken.getType() == TokenType.NAO) {
            operadorL2();
            x4();
            expLogica2();
        } else {
            x5();
            expLogica2();
        }
    }

    // <X4> ::= '('<Vetor_Funcao>')' | <Valor_Booleano>
    // primeiro(<X4>) = {'(', numero_t, id, caractere_t, cadeia_t, booleano_t}
    private void x4() {
        if (currentToken.getType() == TokenType.PAREN_L){
            if (lookAheadToken(2, TokenType.VEC_DELIM_L) ||
                    lookAheadToken(2, TokenType.PAREN_L)){
                expect(TokenType.PAREN_L);
                vetorFuncao();
                expect(TokenType.PAREN_R);
            }
        } else valorBooleano();
    }

    // <X5> ::= <X4> | <Vetor_Funcao>
    // primeiro(<X5>) = {'(', numero_t, id, caractere_t, cadeia_t, booleano_t, id}
    private void x5() {
        if (accept(TokenType.IDENTIFIER)) {
            vetorFuncao();
        } else x4();
    }

    // <Operador_L1> ::= 'e' | 'ou'
    private void operadorL1() {
        if (accept(TokenType.E));
        else if (accept(TokenType.OU));
        else error(TokenType.E, TokenType.OU);
    }

    // <Operador_L2> ::= 'nao'
    private void operadorL2() {
        expect(TokenType.NAO);
    }

    // <Valor_Booleano> ::= '('<Exp_Logica>')' | <Exp_Relacional> |  booleano_t
    // primeiro(<Valor_Booleano>) = {'(', numero_t, id, caractere_t, cadeia_t, booleano_t}
    private void valorBooleano() {
        // Como decidir entre as produções?
        if (!accept(TokenType.BOOL_V)){
            // Ambas as produções restantes podem começar com varios (
            if (accept(TokenType.PAREN_L)) {
                expLogica();
                expect(TokenType.PAREN_R);
            } else if (accept(TokenType.BOOL_V));
            else expRelacional();
        }
    }

    /* <Exp_Relacional> ::= <Numerico_Funcao><Operador_R1><Numerico_Funcao> |
     *                      <Literal_Numero><Operador_R2><Literal_Numero>
     * primeiro(<Exp_Relacional>) = {'(', numero_t, id, caractere_t, cadeia_t, booleano_t}
     */
    private void expRelacional() {
        if (currentToken.getType() == TokenType.NUMBER ||
                currentToken.getType() == TokenType.PAREN_L ||
                currentToken.getType() == TokenType.IDENTIFIER) {
            numericoFuncao();
            operadorR1();
            numericoFuncao();
        } else {
            literalNumero();
            operadorR2();
            literalNumero();
        }
    }

    // <Operador_R1> ::= <Operador_R2> | '<' | '<=' | '>' | '>='
    // Primeiro(<Operador_R1>) = {'=', '<>', '<', '<=', '>', '>='}
    private void operadorR1() {
        if (accept(TokenType.NEQ));
        else if (accept(TokenType.EQ));
        else if (accept(TokenType.LT));
        else if (accept(TokenType.LE));
        else if (accept(TokenType.GT));
        else if (accept(TokenType.GE));
        else error(TokenType.NEQ, TokenType.EQ, TokenType.LT,
                    TokenType.LE, TokenType.GE, TokenType.GT);
    }

    // <Operador_R2> ::= '<>' | '='
    // Primeiro(<Operador_R2>) = {'=', '<>'}
    private void operadorR2() {
        if (accept(TokenType.NEQ));
        else if (accept(TokenType.EQ));
        else error(TokenType.NEQ, TokenType.EQ);
    }

    // <Literal_Numero> ::= caractere_t | cadeia_t | booleano_t
    // literal menos numero?
    // primeiro(<Literal_Numero>) = {caractere_t, cadeia_t, booleano_t}
    private void literalNumero() {
        if (accept(TokenType.CHARACTER));
        else if (accept(TokenType.CHAR_STRING));
        else if (accept(TokenType.BOOL_V));
        else error(TokenType.CHAR_STRING, TokenType.CHARACTER, TokenType.BOOL_V);
    }

    // <Tipo> ::= 'inteiro' | 'real' | 'booleano' | 'cadeia' | 'caractere'
    private boolean tipo() { // mudar pra tipo boolean?
        if (accept(Token.TokenType.INTEIRO)) return true;
        if (accept(Token.TokenType.REAL)) return true;
        if (accept(Token.TokenType.CARACTERE)) return true;
        if (accept(Token.TokenType.CADEIA)) return true;
        if (accept(Token.TokenType.BOOLEANO)) return true;
        return false;
    }

    // <Literal> ::= caractere_t | cadeia_t | numero_t | booleano_t
    private void literal() {
        if (accept(Token.TokenType.NUMBER));
        else if (accept(Token.TokenType.CHARACTER));
        else if (accept(Token.TokenType.CHAR_STRING));
        else if (accept(Token.TokenType.BOOL_V));
        else error(TokenType.NUMBER, TokenType.CHAR_STRING,
                    TokenType.CHARACTER, TokenType.BOOL_V);
    }
}
