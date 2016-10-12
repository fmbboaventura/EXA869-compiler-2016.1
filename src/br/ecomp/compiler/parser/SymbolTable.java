package br.ecomp.compiler.parser;

import br.ecomp.compiler.lexer.Token;

import java.util.HashMap;

/**
 * @author Filipe Boaventura
 * @since 12/10/2016.
 */
public class SymbolTable {
    private HashMap<String, Symbol> table;
    private SymbolTable previous;

    protected SymbolTable(SymbolTable p) {
        table = new HashMap<>();
        previous = null;
    }

    public void put(Symbol s) {
        table.put(s.getToken().getLexeme(), s);
    }

    public Symbol get(Token t) {
        for (SymbolTable env = this; env != null; env = env.previous) {
            if (env.table.containsKey(t.getLexeme()))
                return env.table.get(t.getLexeme());
        }
        return null;
    }
}
