IF EXIST out (
rmdir /s /q out
)
mkdir out
javac -d out src/br/ecomp/compiler/Main.java src/br/ecomp/compiler/lexer/Token.java src/br/ecomp/compiler/lexer/Lexer.java