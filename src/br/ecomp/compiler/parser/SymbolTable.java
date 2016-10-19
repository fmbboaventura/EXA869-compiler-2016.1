package br.ecomp.compiler.parser;

import br.ecomp.compiler.lexer.Token;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Filipe Boaventura
 * @since 12/10/2016.
 */
public class SymbolTable {
    private HashMap<String, Symbol> table;
    private SymbolTable previous;

    protected SymbolTable(SymbolTable p) {
        table = new HashMap<>();
        previous = p;
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

    public boolean isRoot() {
        return previous == null;
    }

    @Override
    public String toString() {
        String symbols = "{\n";
        for (String s : table.keySet()) {
            symbols += "\t" + table.get(s).toString() + "\n";
        }
        symbols += "}";
        return symbols;
    }
}
