package br.ecomp.compiler;

import br.ecomp.compiler.lexer.Lexer;

import java.io.File;
import java.io.IOException;

/**
 * @author Filipe Boaventura
 * @since 16/07/2016.
 */
public class Main {
    public static void main(String[] args) throws IOException {

        final File input;

        if(args.length > 0) input = new File(args[0]);
        else input = new File("input");

        Lexer lexer = new Lexer();
        lexer.createTokens(input);
    }
}
