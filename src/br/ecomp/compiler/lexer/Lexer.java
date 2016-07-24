package br.ecomp.compiler.lexer;

import java.io.*;
import java.util.LinkedList;

/**
 * @author Filipe Boaventura
 * @since 20/07/2016.
 */
public class Lexer {

    public void createTokens(File input) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(input));
        LinkedList<String> lexemeList = new LinkedList<>();
        boolean readingString = false;
        String lexeme = "";

        char c;
        int i;
        while ((i = reader.read()) != -1) {
            c = (char) i;

            if(isLexDelimiter(c, readingString)){
                if (!lexeme.isEmpty() && !readingString) {
                    lexemeList.add(lexeme);
                    lexeme = Character.isWhitespace(c) ? "" : Character.toString(c);
                }

                switch (c){
                    case '"':
                        if(readingString) {
                            lexeme += c;
                            lexemeList.add(lexeme);
                            lexeme = "";
                        }
                        readingString = !readingString;
                        break;
                    case '>':
                        reader.mark(1);
                        c = (char) reader.read();
                        if (c == '=') lexeme += c;
                        else reader.reset();
                        lexemeList.add(lexeme);
                        lexeme = "";
                        break;
                    case '<':
                        reader.mark(1);
                        c = (char) reader.read();
                        if (c == '=' || c == '>') lexeme += c;
                        else reader.reset();
                        lexemeList.add(lexeme);
                        lexeme = "";
                        break;
                    case '-':
                        reader.mark(1);
                        c = (char) reader.read();
                        if (Character.isDigit(c)) lexeme += c;
                        else reader.reset();
                        lexemeList.add(lexeme);
                        lexeme = "";
                        break;
                }

                continue;
            }

            lexeme += c;
        }
        for (String s :
                lexemeList) {
            System.out.println(s);
        }
    }

    /**
     * Retorna se o caractere é um delimitador de lexema.
     */
    private boolean isLexDelimiter(char c, boolean readingString) {
        if (readingString) return (c == '"') || (c == '\n') || (c == '\r');
        return (Character.isWhitespace(c) ||
                (c == '<') ||
                (c == '>') ||
                (c == '+') ||
                (c == '-') ||
                (c == '*') ||
                (c == '/') ||
                (c == ';') ||
                (c == '"') ||
                (c == '\''));
    }
}
