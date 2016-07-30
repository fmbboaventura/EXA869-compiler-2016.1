javac -d out src/br/ecomp/compiler/Main.java src/br/ecomp/compiler/lexer/Token.java src/br/ecomp/compiler/lexer/Lexer.java
java -cp out br.ecomp.compiler.Main %*