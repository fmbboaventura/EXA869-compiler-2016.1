package br.ecomp.compiler.parser;

import br.ecomp.compiler.lexer.Token;

import java.lang.reflect.Method;

/**
 * @author Filipe Boaventura
 * @since 21/10/2016.
 */
public class Variable extends Symbol{

    private final boolean isConstant;

    public Variable(Token token, Type type, boolean isConstant) {
        super(token, type);
        this.isConstant = isConstant;
    }

    public Variable(Token token, Type type) {
        this (token, type, false);
    }

    public boolean isConstant() {
        return isConstant;
    }

    @Override
    public String toString() {
        return super.toString() + " isConstant:" + isConstant();
    }
}
