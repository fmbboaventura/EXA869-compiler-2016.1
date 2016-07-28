package br.ecomp.compiler.lexer;

/**
 * @author Filipe Boaventura
 * @since 27/07/2016.
 */
public class Token {
    protected enum TokenType {
        KEYWORD("palavra_reservada"),
        IDENTIFIER("id"),
        NUMBER("num"),
        OPERATOR("operador"),
        DELIMITER("delimitador"),
        INVALID("token_invalido");

        private final String name;

        TokenType (String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
    private int line;
    private String lexeme;
    private TokenType type;

    protected Token(int line, String lexeme) {
        this.line = line;
        this.lexeme = lexeme;
        this.type = TokenType.INVALID;
    }

    public Token(int line, String lexeme, TokenType type) {
        this(line, lexeme);
        this.type = type;
    }

    protected void setType(TokenType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("%02d %s %s", line, lexeme, type.toString());
    }
}
