package br.ecomp.compiler;

import br.ecomp.compiler.lexer.Lexer;
import br.ecomp.compiler.parser.Parser;

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
        } else if (!input.getName().substring(
                input.getName().lastIndexOf('.')).equals(".txt"))
            return;

        Lexer lexer = new Lexer();
        Parser parser = new Parser();

        System.out.println("Analisando o Arquivo: " + input.getName());
        parser.parse(lexer.createTokens(input), "output" + File.separator + "sin_" + input.getName());
    }
}
