package br.ecomp.compiler.lexer;

/**
 * @author Filipe Boaventura
 * @since 27/07/2016.
 */
public class Token {

    protected enum TokenType {
        KEYWORD("Palavra Reservada"),
        CHARACTER("Caractere"),
        INVALID_CHARACTER("Caractere Mal formado"),
        IDENTIFIER("Identificador"),
        INVALID_IDENTIFIER("Identificador Mal Formado"),
        NUMBER("Numero"),
        INVALID_NUMBER("Numero Mal Formado"),
        OPERATOR("Operador"),
        DELIMITER("Delimitador"),
        INVALID_CHAR_STRING("Cadeia de Caracteres Mal Formada"),
        CHAR_STRING("Cadeia de Caracteres"),
        COMMENT("Comentario"),
        INVALID_COMMENT("Comentario Mal Formado"),
        INVALID_SYMBOL("Simbolo Invalido"),
        INVALID("Token Invalido");

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

    protected TokenType getType() {
        return type;
    }

    protected String getLexeme() {
        return lexeme;
    }

    @Override
    public String toString() {
        return String.format("%02d %s %s", line, getLexeme(), type.toString());
    }
}
