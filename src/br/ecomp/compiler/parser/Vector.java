package br.ecomp.compiler.parser;

import br.ecomp.compiler.lexer.Token;

/**
 * @author Filipe Boaventura
 * @since 19/10/2016.
 */
public class Vector extends Symbol {
    private final int dimensions;

    public Vector(Token token, Type type, int d) {
        super(token, type);
        this.dimensions = d;
    }

    public int getDimensions() {
        return dimensions;
    }

    @Override
    public String toString() {
        String s = getType().name() + "<<<";
        for (int i = 0; i < dimensions - 1; i++) {s += ",";}
        s += ">>>";
        return getToken().getLexeme() + ":" + s;
    }
}
