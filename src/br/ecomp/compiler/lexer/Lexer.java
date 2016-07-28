package br.ecomp.compiler.lexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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
        if (input.isDirectory()) {
            File[] files = input.listFiles();
            if ((files != null) && (files.length > 0))
                for (File f :
                        files) {
                    createTokens(f);
                }
            return;
        }

        reader = new BufferedReader(new FileReader(input));
        LinkedList<Token> tokenList = new LinkedList<>();
        LinkedList<Token> faultyTokenList = new LinkedList<>();

        char c;
        while ((c = lookAheadChar()) != eof) {

            if (Character.isWhitespace(c)){
                nextChar();
            } else if (c == '>' || c == '<' || c == '=') {
                tokenList.add(buildRelopToken());
            } else if (c == '{') {
                tokenList.add(buildCommentLexeme());
            } else if (c == '"') {
                tokenList.add(buildStringLexeme());
            } else if (c == '-' || Character.isDigit(c)) {
            	tokenList.add(buildNumberLexeme());
            } else if (Character.isLetter(c)){
            	tokenList.add(buildIdLexeme());
            } else if (isLexDelimiter(c)) {
            	tokenList.add(new Token(lineCount,
                        Character.toString(nextChar())));
            } else {
            	faultyTokenList.add(new Token(lineCount,
                        Character.toString(nextChar()),
                        Token.TokenType.INVALID));
            }
        }

        tokenList.forEach(System.out::println);
        faultyTokenList.forEach(System.out::println);

        reset();
        reader.close();
    }

    private Token buildIdLexeme() throws IOException {
        int line = lineCount;
    	String lexeme = Character.toString(nextChar());
    	
        //enquanto o proximo nao for delimitador
        while (!isLexDelimiter(lookAheadChar())){
        	lexeme+= nextChar(); 	
        }
		return new Token(line, lexeme);
	}

	/**
     * Constrói os tokens dos operadores relacionais.
     * @return O tokens construido
     * @throws IOException - caso ocorra algum erro na leitura do arquivo de entrada
     */
    private Token buildRelopToken() throws IOException {
        String lexeme = "";
        int state = 0;
        int line = lineCount;
        char c;

        while (true) {
            switch (state) {
                case 0: // Estado 0: não leu nada
                    c = nextChar();
                    if (c == '<') state = 1;
                    else if (c == '>') state = 2;
                    else if (c == '=') state = 4;
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
                    return new Token(line, lexeme, Token.TokenType.OPERATOR);
                case 4: // > ou < sozinhos ou =
                    return new Token(line, lexeme, Token.TokenType.OPERATOR);
            }
        }
    }

    /**
	 * Constroi os {@link Token}s de numeros ou o token do sinal de subtração.
	 * @return O lexema de um numero contido numa stancia de {@link Token}, que
     * deve ser validada, ou o token do sinal de subtracao, que não precisa de
     * validação.
	 * @throws IOException - Caso ocorra um erro de leitura no arquivo.
	 */
	private Token buildNumberLexeme() throws IOException {
		String lexeme = "";
	    int state = 0;
        int line = lineCount;
	    char c;
	    
	    while (true) {
	    	switch (state) {
	    		case 0:
	    			c = nextChar();
	    			if (c == '-') state = 1;
	    			else state = 2;
	    			lexeme += c;
	    			break;
	    		case 1: // Estado 1: leu um -
	    			c = lookAheadChar();
	    			if (Character.isDigit(c)) state = 2;
	    			else return new Token(line, lexeme, Token.TokenType.OPERATOR);
	    			break;
	    		case 2: // Estado 2: leu - e um digito
	    			lexeme += nextChar();
	    			if (isLexDelimiter(lookAheadChar())) return new Token(line, lexeme);
	    			state = 2;
	    			break;
	    	}
	    }
	}

	/**
     * Constrói um lexema que começa com (") e o armazena num {@link Token}.
     * Os lexemas devem ser validados pare que o token possa receber um
     * {@link Token.TokenType} válido.
     * @return O Token construído. O lexema consiste de uma string que termina
     * no próximo (") ou numa quebra de linha.
     * @throws IOException caso ocorra algum erro de leitura no arquivo
     */
    private Token buildStringLexeme () throws IOException {
        String lexeme = "";
        int state = 0;
        char c;
        int line = lineCount;

        while (true) {
            switch (state) {
                case 0: // Estado 0: nada foi lido
                    lexeme += nextChar();
                    state = 1;
                    break;
                case 1: // Estado 1: leu o primeiro "
                    c = lookAheadChar();
                    if (c == '"') state = 2;
                    else if (isNewline(c) || c == eof)return new Token(line, lexeme);
                    else lexeme += nextChar();
                    break;
                case 2:
                    lexeme += nextChar();
                   return new Token(line, lexeme);
            }
        }
    }
    
    /**
     * Constrói um lexema que começa com ({) e o armazena num token.
     * O lexema devem ser validado para que o token seja considerado
     * valido e possa receber seu {@link Token.TokenType}.
     * @return O um {@link Token} contendo o lexema construído e com o
     * {@link Token.TokenType} a sr confimado. O lexema é uma string
     * que termina no próximo(}) ou no final do arquivo.
     * @throws IOException caso ocorra algum erro de leitura no arquivo
     */
    private Token buildCommentLexeme() throws IOException{
        // Se chegou aqui, é sabido que o caractere lido é o {
        int line = lineCount;
		String lexeme = Character.toString(nextChar());
		char c;

        // Checa se o proximo char é o fim de arquivo
        while (lookAheadChar() != eof) {
            c = nextChar(); // Se não, lê o próximo char
            lexeme += c;    // Concatena ao lexema
            if (c == '}') break; // Se o char lido foi o }, cai fora do loop
        }
        return new Token(line, lexeme);
	}

    /**
     * Retorna se o caractere é um delimitador de lexema.
     */
    private boolean isLexDelimiter(char c) {
        return (Character.isWhitespace(c) ||
                (c == '<') ||
                (c == '>') ||
                (c == '+') ||
                (c == '-') ||
                (c == '*') ||
                (c == '/') ||
                (c == ';') ||
                (c == '"') ||
                (c == eof) ||
                (c == '\'') ||
                (c == '(') ||
                (c == ')') ||
                (c == '}') );
    }

    private boolean isNewline(char c) throws IOException {
        boolean newLineFound = (c == '\n') || (c == '\r');
        if (c == '\r' && lookAheadChar() == '\n') reader.read();
        return newLineFound;
    }

    /** Este metodo recebe uma string e verifica se o token
	 * eh um identificador
	 */
	public boolean isTokenId(String input){
		//regex para identificadores
		Pattern p = Pattern.compile("^[a-z|A-Z ][\\w]*");
		boolean matches = Pattern.matches(p.pattern(), input);
		return matches;
	}
	
	/** Este metodo recebe uma string e verifica se o token
	 * eh um numero
	 */
	public boolean isTokenNumber(String input){
		//regex para numeros
		Pattern p = Pattern.compile("^-?[0-9]*\\.?[0-9]*");
		boolean matches = Pattern.matches(p.pattern(), input);
		return matches;
	}
	
	/** Este metodo recebe uma string e verifica se o token
	 * eh uma cadeia de caractere
	 */
	public boolean isTokenString(String input){
		//regex para cadeia de caracteres
		Pattern p = Pattern.compile("^\"[a-z|A-Z ][a-z|A-Z|\\d| ]*\"");
		boolean matches = Pattern.matches(p.pattern(), input);
		return matches;
	}
	
	/** Este metodo recebe uma string e verifica se o token
	 * eh um comentario
	 */
	public boolean isTokenComment(String input){
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
