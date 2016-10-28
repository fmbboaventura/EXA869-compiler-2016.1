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
        previous = p;
    }

    public void put(Symbol s) {
        String key = (s instanceof Function) ? "f_" + s.getToken().getLexeme()
                : s.getToken().getLexeme();
        table.put(key, s);
    }

    public Symbol get(Token t) {
        return get(t, false);
    }

    public Symbol get(Token t, boolean isFunction) {
        String key = (isFunction) ? "f_" + t.getLexeme() : t.getLexeme();
        for (SymbolTable env = this; env != null; env = env.previous) {
            if (env.table.containsKey(key))
                return env.table.get(key);
        }
        return null;
    }

    public boolean containsSymbol(Token t) {
        return containsSymbol(t, false);
    }

    public boolean containsSymbol(Token t, boolean isFunction) {
        String key = (isFunction) ? "f_" + t.getLexeme() : t.getLexeme();
        for (SymbolTable env = this; env != null; env = env.previous) {
            if (env.table.containsKey(key)) return true;
        }
        return false;
    }

    public boolean containsSymbol(Symbol s) {
        return containsSymbol(s.getToken());
    }

    public boolean containsSymbolLocal(Token t) {
        return table.containsKey(t.getLexeme());
    }

    public boolean containsSymbolLocal(Symbol s) {
        return this.containsSymbolLocal(s.getToken());
    }

    public boolean isRoot() {
        return previous == null;
    }

    public Symbol[] getSymbols() {
        return table.values().toArray(new Symbol[table.values().size()]);
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
