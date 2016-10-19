package br.ecomp.compiler.parser;

import br.ecomp.compiler.lexer.Token;

/**
 * @author Filipe Boaventura
 * @since 12/10/2016.
 */
public class Symbol {
    protected enum Type {
        INTEIRO,
        REAL,
        BOOLEANO,
        CARACTERE,
        CADEIA,
        VOID
    }
    private Token token;
    private Type type;

    public Symbol(Token token, Type type) {
        this.token = token;
        this.type = type;
    }

    public Token getToken() {
        return token;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return token.getLexeme() + ":" + type.name();
    }
}
