package br.ecomp.compiler.parser;

import br.ecomp.compiler.lexer.Token;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Filipe Boaventura
 * @since 24/09/2016.
 */
public class Parser {
    /**
     * Ponteiro para o simbolo atual da entrada.
     */
    private Token currentToken;
    private Iterator<Token> tokenIt;
    private int errorCount;

    /**
     * Inicia a análise sintática sobre a coleção de
     * {@link Token}s.
     * @param tokens coleção de entrada contendo os
     * {@link Token}s para a análise sintática.
     */
    public void parse(Collection<Token> tokens) {
        errorCount = 0;
        tokenIt = tokens.iterator();
        System.out.println("Passo 2: Analise Sintatica");
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
        if (tokenIt.hasNext()) currentToken = tokenIt.next();
        accept(Token.TokenType.COMMENT); // Pulando comentarios
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
        error("Erro! Token inesperado :"
                + "\n\t" + currentToken.toString());
        return false;
    }

    private void error(String message) {
        errorCount++;
        System.out.println(message);
        // TODO: gravar mensagem num txt
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
            expect(Token.TokenType.INICIO); // Espera um inicio
            varlist();
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

    // <Bloco> ::= 'inicio' 'fim'
    // bloco simplificado por agora
    private void bloco() {
        expect(Token.TokenType.INICIO);
        expect(Token.TokenType.FIM);
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
        if (accept(Token.TokenType.DATA_TYPE)) {
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
        if (accept(Token.TokenType.DATA_TYPE)) { // espera um tipo
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

    // <Literal> ::= caractere_t | cadeia_t | numero_t | booleano_t
    private void literal() {
        if (accept(Token.TokenType.NUMBER));
        else if (accept(Token.TokenType.CHARACTER));
        else if (accept(Token.TokenType.CHAR_STRING));
        else if (accept(Token.TokenType.BOOL_V));
        else error("Erro! Token inesperado :"
                    + "\n\t" + currentToken.toString());
    }
}