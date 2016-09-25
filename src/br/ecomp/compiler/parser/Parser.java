package br.ecomp.compiler.parser;

import br.ecomp.compiler.lexer.Token;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Filipe Boaventura
 * @since 24/09/2016.
 */
public class Parser {
    private Token currentToken;
    private Iterator<Token> tokenIt;
    private int errorCount;

    public void parse(Collection<Token> tokens) {
        errorCount = 0;
        tokenIt = tokens.iterator();
        System.out.println("Passo 2: Analise Sintatica");
        System.out.println(String.format("\t%d erros sintáticos foram encontrados", errorCount));
        if (errorCount == 0) System.out.println("\tAnalise Sintatica concluida com sucesso.");
    }

    private void nextToken() {
        if (tokenIt.hasNext()) currentToken = tokenIt.next();
    }

    private boolean accept (Token.TokenType type) {
        if (currentToken.getType() == type) {
            nextToken();
            return true;
        } return false;
    }

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

    private void programa() {
        nextToken();
        variaveis();
        c();
    }

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

    private void idvetor() {
        expect(Token.TokenType.IDENTIFIER);
        vetor();
    }

    private void vetor() {
        if (accept(Token.TokenType.VEC_DELIM_L)) {//se encontrou <<<
            expect(Token.TokenType.NUMBER); // TODO implementar as expressoes
            vetor2();
            expect(Token.TokenType.VEC_DELIM_R); // espera que feche o vetor com >>>
        }
    }

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
