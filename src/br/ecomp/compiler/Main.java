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

        if (input.isDirectory())
            for (File file : input.listFiles()) {
                // Chamar o lexer para cada arquivo da pasta
                System.out.println(file.getName());
                lexer.createTokens(file);
            }
    }
}
