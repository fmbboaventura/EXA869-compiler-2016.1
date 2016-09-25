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

        analyze(input);
    }

    private static void analyze(File input) throws IOException {
        if (input.isDirectory()) {
            File[] files = input.listFiles();
            if ((files != null) && (files.length > 0))
                for (File f : files) {
                    analyze(f);
                }
            return;
        }

        Lexer lexer = new Lexer();

        System.out.println("Analisando o Arquivo: " + input.getName());
        lexer.createTokens(input);
    }
}
