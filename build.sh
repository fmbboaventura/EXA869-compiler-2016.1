if [ -d out ]
then
    rm -r out
fi
mkdir out
javac -d out src/br/ecomp/compiler/Main.java src/br/ecomp/compiler/lexer/Token.java src/br/ecomp/compiler/lexer/Lexer.java