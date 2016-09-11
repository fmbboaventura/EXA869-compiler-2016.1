package br.ecomp.compiler.lexer;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Filipe Boaventura
 * @since 20/07/2016.
 */
public class Lexer {

    private BufferedReader reader;
    private int lineCount, colCount;
    private final char eof;
    private final ArrayList<String> keywords, logOp;

    public Lexer() {
        lineCount = 1;
        colCount = 1;
        eof = (char) -1;
        keywords = new ArrayList<>(Arrays.asList("programa",
                "const", "var", "funcao", "inicio", "fim",
                "se", "entao", "enquanto", "faca", "leia",
                "escreva", "inteiro", "real", "booleano",
                "verdadeiro", "falso", "cadeia", "caractere"));
        logOp = new ArrayList<>(Arrays.asList("nao", "e", "ou"));
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
        Token t;
        while ((c = lookAheadChar()) != eof) {

            if (Character.isWhitespace(c)) {
                nextChar();
            } else if (c == '>' || c == '<' || c == '=') {
                tokenList.add(buildRelopToken());
            } else if (c == '{') {
                t = buildCommentLexeme();
                if (isTokenComment(t.getLexeme())) {
                    t.setType(Token.TokenType.COMMENT);
                    tokenList.add(t);
                } else {
                    t.setType(Token.TokenType.INVALID_COMMENT);
                    faultyTokenList.add(t);
                }
            } else if (c == '"') {
                t = buildStringLexeme();
                if (isTokenString(t.getLexeme())) {
                    t.setType(Token.TokenType.CHAR_STRING);
                    tokenList.add(t);
                } else {
                    t.setType(Token.TokenType.INVALID_CHAR_STRING);
                    faultyTokenList.add(t);
                }
            } else if (c == '\'') {
                t = buildCharacterLexeme();
                if (isTokenChar(t.getLexeme())) {
                    t.setType(Token.TokenType.CHARACTER);
                    tokenList.add(t);
                } else {
                    t.setType(Token.TokenType.INVALID_CHARACTER);
                    faultyTokenList.add(t);
                }
            } else if (c == '-' || Character.isDigit(c)) {
                t = buildNumberLexeme();
                if (!t.getType().equals(Token.TokenType.OPERATOR)) {
                    if (isTokenNumber(t.getLexeme())) {
                        t.setType(Token.TokenType.NUMBER);
                        tokenList.add(t);
                    } else {
                        t.setType(Token.TokenType.INVALID_NUMBER);
                        faultyTokenList.add(t);
                    }
                } else tokenList.add(t);
            } else if (Character.isLetter(c)) {
                t = buildIdLexeme();

                if (logOp.contains(t.getLexeme())){
                    t.setType(Token.TokenType.OPERATOR);
                    tokenList.add(t);
                } else if (keywords.contains(t.getLexeme())){
                    t.setType(Token.TokenType.KEYWORD);
                    tokenList.add(t);
                } else if (isTokenId(t.getLexeme())) {
                    t.setType(Token.TokenType.IDENTIFIER);
                    tokenList.add(t);
                } else {
                    t.setType(Token.TokenType.INVALID_IDENTIFIER);
                    faultyTokenList.add(t);
                }
            } else if (isOperator(c)) {
                tokenList.add(new Token(lineCount,
                        Character.toString(nextChar()),
                        Token.TokenType.OPERATOR));
            } else if (isLexDelimiter(c)) {
                tokenList.add(new Token(lineCount,
                        Character.toString(nextChar()),
                        Token.TokenType.DELIMITER));
            } else {
                faultyTokenList.add(buildFaultyTokenBecauseWhyNot());
            }
        }

        tokenList.addAll(faultyTokenList);
        tokenList.forEach(System.out::println);
        writeOutput(input.getName(), tokenList);

        reset();
        reader.close();
    }

    private void writeOutput(String fileName, List<Token> tokenList) throws IOException {
        BufferedWriter writer = new BufferedWriter(
                new FileWriter(new File("output" + File.separator + fileName)));

        for (Token t : tokenList) {
            writer.write(t.toString());
            writer.newLine();
        }
        writer.close();
    }

    private Token buildFaultyTokenBecauseWhyNot() throws IOException {
        int line = lineCount;
        String lexeme = Character.toString(nextChar());

        //enquanto o proximo nao for delimitador
        while (!isLexDelimiter(lookAheadChar())) {
            lexeme += nextChar();
        }
        return new Token(line, lexeme, Token.TokenType.INVALID_SYMBOL);
    }

    private Token buildIdLexeme() throws IOException {
        int line = lineCount;
        String lexeme = Character.toString(nextChar());

        //enquanto o proximo nao for delimitador
        while (!isLexDelimiter(lookAheadChar())) {
            lexeme += nextChar();
        }
        return new Token(line, lexeme);
    }

    /**
     * Constrói os tokens dos operadores relacionais.
     *
     * @return O tokens construido
     * @throws IOException - caso ocorra algum erro na leitura do arquivo de entrada
     */
    private Token buildRelopToken() throws IOException {
        String lexeme = "";
        int state = 0;
        int line = lineCount;
        char c;
        Token.TokenType type = Token.TokenType.OPERATOR;

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
                    else if (c == '<') state = 5;
                    else state = 4;
                    break;
                case 2: // Estado 2: leu >
                    c = lookAheadChar(); // olha um caractere a frente
                    if (c == '=') state = 3;
                    else if (c == '>') state = 6;
                    else state = 4;
                    break;
                case 3: // Estado 3: leu < ou > e outro caractere que forma lexema com eles
                    lexeme += nextChar(); // É sabido que o proximo char deve ser concatenado
                    return new Token(line, lexeme, type);
                case 4: // > ou < sozinhos ou =
                    return new Token(line, lexeme, type);
                case 5: // Estado 5: leu <<
                    lexeme += nextChar();
                    c = lookAheadChar();
                    if (c == '<') {
                        type = Token.TokenType.VEC_DELIM;
                        state = 3; // estado 3 concatena o terceiro <
                    }
                    else {
                        type = Token.TokenType.ATRIB;
                        state = 4; // retorna o << caso leia qualquer coisa que não seja um <
                    }
                    break;
                case 6: // Estado 6: leu >>
                    if (lookAheadChar(2)[1] == '>') { // sabe-se que o primeiro caractere do array é o segundo >
                        type = Token.TokenType.VEC_DELIM;
                        lexeme += nextChar();
                        state = 3; // estado 3 concatena o terceiro >
                    }
                    else state = 4; // retorna o < caso leia qualquer coisa que não seja um <
                    break;
            }
        }
    }

    /**
     * Constroi os {@link Token}s de numeros ou o token do sinal de subtração.
     *
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
                    if (isLexDelimiter(lookAheadChar())) return new Token(line, lexeme);
                    lexeme += nextChar();
                    state = 2;
                    break;
            }
        }
    }

    /**
     * Constrói um lexema que começa com (") e o armazena num {@link Token}.
     * Os lexemas devem ser validados pare que o token possa receber um
     * {@link Token.TokenType} válido.
     *
     * @return O Token construído. O lexema consiste de uma string que termina
     * no próximo (") ou numa quebra de linha.
     * @throws IOException caso ocorra algum erro de leitura no arquivo
     */
    private Token buildStringLexeme() throws IOException {
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
                    else if (isNewline(c) || c == eof) return new Token(line, lexeme);
                    else lexeme += nextChar();
                    break;
                case 2:
                    lexeme += nextChar();
                    return new Token(line, lexeme);
            }
        }
    }

    /**
     * Constrói um lexema que começa com (') e o armazena num {@link Token}.
     * Os lexemas devem ser validados pare que o token possa receber um
     * {@link Token.TokenType} válido.
     *
     * @return O Token construído. O lexema consiste de uma string que termina
     * no próximo (') ou numa quebra de linha.
     * @throws IOException caso ocorra algum erro de leitura no arquivo
     */
    private Token buildCharacterLexeme() throws IOException {
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
                case 1: // Estado 1: leu o primeiro '
                    c = lookAheadChar();
                    if (c == '\'') state = 2;
                    else if (Character.isWhitespace(c) || c == eof) return new Token(line, lexeme);
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
     *
     * @return O um {@link Token} contendo o lexema construído e com o
     * {@link Token.TokenType} a sr confimado. O lexema é uma string
     * que termina no próximo(}) ou no final do arquivo.
     * @throws IOException caso ocorra algum erro de leitura no arquivo
     */
    private Token buildCommentLexeme() throws IOException {
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
        return isOperator(c) ||
                (Character.isWhitespace(c) ||
                        (c == '=') ||
                        (c == '<') ||
                        (c == '>') ||
                        (c == ';') ||
                        (c == '"') ||
                        (c == eof) ||
                        (c == '\'') ||
                        (c == '(') ||
                        (c == ')') ||
                        (c == '}') ||
                        (c == ','));
    }

    private boolean isOperator(char c) {
        return (c == '+') ||
                (c == '-') ||
                (c == '*') ||
                (c == '/');
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
	 * Esse metodo recebe uma string e verifica se o lexema 
	 * eh um char valido
	 * @param input
	 * @return retorna verdadeiro caso o token seja valido
	 * ou falso caso invalido
	 */
	public boolean isTokenChar(String input){
		//regex para cadeia de caracteres
		Pattern p = Pattern.compile("^'[a-z|A-Z|\\d]'");
		boolean matches = Pattern.matches(p.pattern(), input);
		return matches;
	}

    /**
     * Retorna o proximo caractere do stream.
     *
     * @throws IOException
     */
    private char nextChar() throws IOException {
        char c = (char) reader.read();

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
     *
     * @return o caractere lido
     * @throws IOException
     */
    private char lookAheadChar() throws IOException {
        return lookAheadChar(1)[0];
    }


    /**
     * Le os proximos n caracteres sem mover o ponteiro de leitura.
     * Usa {@link Reader#mark(int)} para marcar a posição atual
     * do ponteiro e {@link Reader#reset()} para retornar para
     * a posição marcada, uma vez que a leitura foi realizada.
     *
     * @return um array contendo os proximos n caracteres
     * @throws IOException
     */
    private char[] lookAheadChar(int n) throws IOException {
        if (n <= 0) throw new RuntimeException("n deve ser maior que zero");
        char c[] = new char[n];
        reader.mark(n);
        for (int i = 0; i < n; i++) {
            c[i] = (char) reader.read();
        }
        reader.reset();
        return c;
    }

    private void reset() throws IOException {
        reader.close();
        lineCount = 1;
        colCount = 1;
    }
}
