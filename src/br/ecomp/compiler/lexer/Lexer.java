package br.ecomp.compiler.lexer;

import java.io.*;
import java.util.LinkedList;
import java.util.regex.Pattern;

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
    
    /* Este metodo recebe uma string e verifica se o token
	 * eh um identificador
	 */
	public static boolean isTokenId(String input){
		//regex para identificadores
		Pattern p = Pattern.compile("^[a-z|A-Z ][\\w]*");
		boolean matches = Pattern.matches(p.pattern(), input);
		return matches;
	}
	
	/* Este metodo recebe uma string e verifica se o token
	 * eh um numero
	 */
	public static boolean isTokenNumber(String input){
		//regex para numeros
		Pattern p = Pattern.compile("^-?[0-9]*\\.?[0-9]*");
		boolean matches = Pattern.matches(p.pattern(), input);
		return matches;
	}
	
	/* Este metodo recebe uma string e verifica se o token
	 * eh uma cadeia de caractere
	 */
	public static boolean isTokenString(String input){
		//regex para cadeia de caracteres
		Pattern p = Pattern.compile("^\"[a-z|A-Z ][a-z|A-Z|\\d| ]*\"");
		boolean matches = Pattern.matches(p.pattern(), input);
		return matches;
	}
	
	/* Este metodo recebe uma string e verifica se o token
	 * eh um comentario
	 */
	public static boolean isTokenComment(String input){
		//regex para cadeia de caracteres
		Pattern p = Pattern.compile("\\{[^\\}]*\\}");
		boolean matches = Pattern.matches(p.pattern(), input);
		return matches;
	}
}
