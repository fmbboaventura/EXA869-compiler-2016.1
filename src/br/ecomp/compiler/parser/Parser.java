package br.ecomp.compiler.parser;

import br.ecomp.compiler.lexer.Token;
import br.ecomp.compiler.lexer.Token.TokenType;

import javax.smartcardio.ATR;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
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

    /**
     * Inicia a análise sintática sobre a coleção de
     * {@link Token}s.
     * @param tokens coleção de entrada contendo os
     * {@link Token}s para a análise sintática.
     */
    public void parse(List<Token> tokens) {
        errorCount = 0;
        tokenList = tokens;
        index = -1;
        System.out.println("Passo 2: Analise Sintatica");
        programa();
        System.out.println(String.format("\t%d erros sintáticos foram encontrados", errorCount));
        if (errorCount == 0) System.out.println("\tAnalise Sintatica concluida com sucesso.");
    }

    /**
     * Consome um simbolo do vetor de entrada e atualiza o
     * {@link Parser#currentToken}. Consome mais um simbolo
     * caso um token do tipo {@link br.ecomp.compiler.lexer.Token.TokenType#COMMENT}
     * seja encontrado, para ignorar comentários.
     */
    private void nextToken() {
        if (index + 1 < tokenList.size()) {
            index++;
            currentToken = tokenList.get(index);
            accept(Token.TokenType.COMMENT); // Pulando comentarios
            System.out.println("Token Atual: " + currentToken.toString());
        }
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
            expectedTokenNames += expected[i].name();
            if (i < expected.length-1)
                expectedTokenNames += ", ";
        }
        System.out.println(String.format("Erro na linha %d. Esperava: %s. Obteve: %s.",
                currentToken.getLine(), expectedTokenNames, currentToken.getLexeme()
                        + " " + currentToken.getType()));
        // TODO: gravar mensagem num txt
    }

    /**
     * 
     * @param sync
     */
    private void panicMode(TokenType... sync) {
    	List<TokenType> syncTokens = Arrays.asList(sync);
    	while(!syncTokens.contains(currentToken.getType())){
    		System.out.println("\tPulou Token: " + currentToken.toString());
    		nextToken();
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
            expect(Token.TokenType.FIM);
        }
    }

    // <C> ::= <Constantes><P> | <P>
    private void c() {
        constantes();
        p();
    }

    // <P> ::= 'programa'<Bloco>
    private void p() {
        expect(Token.TokenType.PROGRAMA);
        bloco();
    }

    // <Constantes> ::= 'const''inicio'<Const_List>'fim'
    private void constantes() {
        if (accept(Token.TokenType.CONST)) {
            expect(Token.TokenType.INICIO);
            constlist();
            expect(Token.TokenType.FIM);
        }
    }

    // <Const_List> ::= <Tipo><Const_Decl><Const_List>  |<>
    private void constlist() {
        if (tipo()) {
            constdecl();
            constlist();
        }
    }

    // <Const_Decl> ::= id'<<'<Literal><Const_Decl2>
    private void constdecl() {
        expect(Token.TokenType.IDENTIFIER);
        expect(Token.TokenType.ATRIB);
        literal();
        constdecl2();
    }

    // <Const_Decl2> ::= ','<Const_Decl> | ';'
    private void constdecl2() {
        if (accept(Token.TokenType.COMMA)) {
            constdecl();
        } else {
            expect(Token.TokenType.SEMICOLON);
        }
    }

    // <Var_List> ::= <Tipo><Var_Decl><Var_List> |<>
    private void varlist() {
        if (tipo()) { // espera um tipo
            vardecl();
            varlist();
        } // o else eh o vazio
    }

    // <Var_Decl> ::= <Id_Vetor>','<Var_Decl> | <Id_Vetor>';' AMBIGUIDADE! Fatorar a esquerda!
    private void vardecl() {
        idvetor();
        if (accept(Token.TokenType.COMMA)) {
            vardecl();
        } else  { // Se não tem virgula, testa por ponto e virgula. Isso resolve a ambiguidade?
            expect(Token.TokenType.SEMICOLON);
        }
    }

    // <Id_Vetor> ::= id<Vetor>
    private void idvetor() {
        expect(Token.TokenType.IDENTIFIER);
        vetor();
    }

    // <Vetor> ::= '<<<'numero_t<Vetor2>'>>>'  | <>
    // vetor simplificado.
    // Vetor original:
    // <Vetor> ::= '<<<'<Exp_Aritmetica><Vetor2>'>>>'  | <>
    // <Vetor2> ::= ','<Exp_Aritmetica><Vetor2> | <>
    private void vetor() {
        if (accept(Token.TokenType.VEC_DELIM_L)) {//se encontrou <<<
            expect(Token.TokenType.NUMBER); // TODO implementar as expressoes
            vetor2();
            expect(Token.TokenType.VEC_DELIM_R); // espera que feche o vetor com >>>
        }
    }

    // <Vetor2> ::= ','numero_t<Vetor2> | <>
    private void vetor2() {
        if (accept(Token.TokenType.COMMA)) { // se encontrou uma virgula
            expect(Token.TokenType.NUMBER); // tem que encontrar um numero em seguida
            vetor2(); // pode se repetir
        }
    }

    // <Bloco> ::= 'inicio'<Corpo_Bloco>'fim'
    // bloco simplificado por agora
    private void bloco() {
        expect(Token.TokenType.INICIO);
        corpoBloco();
        expect(Token.TokenType.FIM);
    }

    //<Corpo_Bloco> ::= <Comando><Corpo_Bloco> | <Atribuicao><Corpo_Bloco> | <Chamada_Funcao>';'<Corpo_Bloco> | <>
    private void corpoBloco() {
        if (currentToken.getType() == TokenType.IDENTIFIER) {
            if (lookAheadToken(1, TokenType.ATRIB)) {
                atribuicao();
                corpoBloco();
            } else if (lookAheadToken(1, TokenType.PAREN_L)) {
                chamadaFuncao();
                expect(TokenType.SEMICOLON);
                corpoBloco();
            }
        } // abaixo são os comandos
        // <Se> ::= 'se''('<Exp_Logica>')''entao'<Bloco><Senao>
        else if (accept(TokenType.SE)) {
            expect(TokenType.PAREN_L);
            // Não tem expressões ainda. Usando booleano como placeholder
            expect(TokenType.BOOL_V);
            expect(TokenType.PAREN_R);
            expect(TokenType.ENTAO);
            bloco();
            // <Senao> ::= 'senao'<Bloco> | <>
            if (accept(TokenType.SENAO)) {
                bloco();
            }
            corpoBloco();
        }
        // <Enquanto> ::= 'enquanto''('booleano_t')''faca'<Bloco>
        else if (accept(TokenType.ENQUANTO)) {
            expect(TokenType.PAREN_L);
            // Não tem expressões ainda. Usando booleano como placeholder
            expect(TokenType.BOOL_V);
            expect(TokenType.PAREN_R);
            expect(TokenType.FACA);
            bloco();
            corpoBloco();
        }
        // <Escreva> ::= 'escreva''('<Escreva_Params>')'';'
        else if (accept(TokenType.ESCREVA)) {
            expect(TokenType.PAREN_L);
            escrevaParams();
            expect(TokenType.PAREN_R);
            expect(TokenType.SEMICOLON);
            corpoBloco();
        }
        // <Leia> ::= 'leia''('<Leia_Params>')'';'
        else if (accept(TokenType.LEIA)) {
            expect(TokenType.PAREN_L);
            leiaParams();
            expect(TokenType.PAREN_R);
            expect(TokenType.SEMICOLON);
            corpoBloco();
        } // Se não cair em nenhuma das condições acima, significa que corpobloco derivou vazio
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

    // <Atribuicao> ::= <Id_Vetor>'<<'<Literal>';'
    private void atribuicao() {
        idvetor();
        expect(TokenType.ATRIB);
        literal();
        expect(TokenType.SEMICOLON);
    }

    // <Chamada_Funcao>::= id '(' <Chamada_Funcao2>
    private void chamadaFuncao() {
        expect(TokenType.IDENTIFIER);
        expect(TokenType.PAREN_L);
        chamadaFuncao2();
    }

    // <Chamada_Funcao2>::=<Param_Cham>')' |  ')'
    private void chamadaFuncao2() {
        if (!accept(TokenType.PAREN_R)) {
            paramCham();
            expect(TokenType.PAREN_R);
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
