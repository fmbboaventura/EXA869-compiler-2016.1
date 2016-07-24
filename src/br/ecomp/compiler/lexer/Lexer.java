package br.ecomp.compiler.lexer;

import java.io.*;
import java.util.LinkedList;
import java.util.regex.Pattern;

/**
 * @author Filipe Boaventura
 * @since 20/07/2016.
 */
public class Lexer {

    private BufferedReader reader;
    private int lineCount, colCount;
    private final char eof;

    public Lexer () {
        lineCount = 1;
        colCount = 1;
        eof = (char)-1;
    }

    public void createTokens(File input) throws IOException {
        reader = new BufferedReader(new FileReader(input));
        LinkedList<String> lexemeList = new LinkedList<>();

        char c;
        while ((c = lookAheadChar()) != eof) {

            if (Character.isWhitespace(c)){
                nextChar();
            } else if (c == '>' || c == '<') {
                lexemeList.add(buildRelopLexeme());
            } else if (c == '{') {

            } else if (c == '"') {

            }
        }
        
        lexemeList.forEach(System.out::println);
        reader.close();
    }

    public String buildRelopLexeme() throws IOException {
        String lexeme = "";
        int state = 0;
        char c;

        while (true) {
            switch (state) {
                case 0: // Estado 0: não leu nada
                    c = nextChar();
                    if (c == '<') state = 1;
                    else if (c == '>') state = 2;
                    lexeme += c;
                    break;
                case 1: // Estado 1: leu <
                    c = lookAheadChar(); // olha um caractere a frente
                    if (c == '=' || c == '>') state = 3;
                    else state = 4;
                    break;
                case 2: // Estado 2: leu >
                    c = lookAheadChar(); // olha um caractere a frente
                    if (c == '=') state = 3;
                    else state = 4;
                    break;
                case 3: // Estado 3: leu < ou > e outro caractere que forma lexema com eles
                    lexeme += nextChar(); // É sabido que o proximo char deve ser concatenado
                    return lexeme;
                case 4: // > ou < sozinhos
                    return lexeme;
            }
        }
    }

    /**
     * Retorna se o caractere é um delimitador de lexema.
     */
    private boolean isLexDelimiter(char c, boolean readingString) {
        if (readingString) return (c == '"') || isNewline(c);
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

    private boolean isNewline(char c) {
        return (c == '\n') || (c == '\r');
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

    /**
     * Retorna o proximo caractere do stream.
     * @throws IOException
     */
	private char nextChar() throws IOException {
	    char c = (char)reader.read();

        if (isNewline(c)) {
            lineCount++;
            colCount = 1;
        } else colCount++;
	    return c;
    }

    /**
     * Le o proximo caractere sem mover o ponteiro de leitura.
     * Usa {@link Reader#mark(int)} para marcar a posição atual
     * do ponteiro e {@link Reader#reset()} para retornar para
     * a posição marcada, uma vez que a leitura foi realizada.
     * @return
     * @throws IOException
     */
    private char lookAheadChar() throws IOException {
        char c;
        reader.mark(1);
        c = (char) reader.read();
        reader.reset();
        return c;
    }

    private void reset() throws IOException {
        reader.close();
        lineCount = 1;
        colCount = 1;
    }
}
