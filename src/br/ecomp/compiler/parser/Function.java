package br.ecomp.compiler.parser;

import br.ecomp.compiler.lexer.Token;

/**
 * @author Filipe Boaventura
 * @since 21/10/2016.
 */
public class Function extends Symbol {

    private final Symbol[] args;

    public Function(Token token, Type type, Symbol... args) {
        super(token, type);
        this.args = args;
    }

    @Override
    public String toString() {
        String s = super.toString() + "\targs: {\n";
        for (Symbol arg : args) {
            s += "\t\t" + arg.toString() + "\n";
        }
        s += "\t}";
        return s;
    }

    public int getArgCount() {
        if (args == null) return 0;
        return args.length;
    }

    public Symbol getArg(int index) {
        return args[index];
    }
}
