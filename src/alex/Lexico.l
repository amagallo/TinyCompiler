package procesamientos;

import java_cup.runtime.Symbol;
import asint.Sintaxis.Strloc;
import asint.sym;

%%

%public %class AnalizadorLexico %extends Procesamiento

%unicode
%caseless

%cup
%line
%column

%warn all

%{
    private static class ETokenInvalido extends Throwable
    {
        public ETokenInvalido(String mensaje) { super(mensaje); }
    }

    private static class ELexico extends EProcesamiento
    {
        public ELexico(Strloc strloc, ETokenInvalido causa)
        {
            super(FASE.LEXICO, strloc, causa);
        }

        public ELexico(Strloc strloc)
        {
            super(FASE.LEXICO, strloc);
        }
    }

    private static final ETokenInvalido CADENA_INVALIDA = new ETokenInvalido("Cadena de caracteres no válida");

    private static class Lexloc extends Symbol
    {
        public Lexloc(int fila, int col, int codigo, String lexema)
        {
            super(codigo, null);
            value = new Strloc(lexema, fila, col);
        }

        @Override
        public String toString() { return value.toString(); }
    }

    private Lexloc lexloc(int codigo, String str) { return new Lexloc(yyline + 1, yycolumn + 1, codigo, str); }
%}

letra = [a-z] | [A-Z]
digito_positivo = [1-9]
digito = "0" | {digito_positivo}

identificador = {letra} ({letra} | {digito} | "_")*
entero = ("-" | "+")? ("0" | {digito_positivo} {digito}*)
real = {entero} ({decimal} | {exponencial} | {decimal} {exponencial})?

decimal = "." ("0" | {digito}* {digito_positivo})
exponencial = ("e" | "E") {entero}  

delim_cadena = "'"
char_impr = [^'\b\r\n]

cadena = {delim_cadena} {char_impr}* {delim_cadena}

salto_linea = "\n"
separador = "\b" | "\r" | {salto_linea}
blanco = " "

comentario = "@" {caracter_linea}* {salto_linea}?
caracter_linea = [^\n]

parentesis_apertura = "("
parentesis_cierre = ")"

corchete_apertura = "["
corchete_cierre = "]"

punto_y_coma = ";"
coma = ","
dos_puntos = ":"
punto = "."

var = "var"
tipo = "type"
proc = "proc"

tipo_basico = "int" | "bool" | "real" | "string"

of = "of"
array = "array"
registro = "record"
puntero = "^"

asignacion = "="
if = "if"
then = "then"
else = "else"

while = "while"
do = "do"

read = "read"
write = "write"
newline = "nl"

new = "new"
delete = "delete"

seq = "seq"
begin = "begin"
end = "end"

booleano = "true" | "false"
nulo = "null"

op_relacional = "==" | "!=" | "<=" | ">=" | "<" | ">"

suma = "+"
resta = "-"
mul = "*"
div = "/"
mod = "%"

and = "and"
or = "or"
not = "not"

%%

{separador}             {}
{blanco}                {}
{comentario}            {}

{var}                   { return lexloc(sym.VAR, yytext()); }
{tipo}                  { return lexloc(sym.TIPO, yytext()); }
{proc}                  { return lexloc(sym.PROC, yytext()); }

{tipo_basico}           { return lexloc(sym.TIPO_BASICO, yytext()); }

{of}                    { return lexloc(sym.OF, yytext()); }
{array}                 { return lexloc(sym.ARRAY, yytext()); }
{registro}              { return lexloc(sym.REGISTRO, yytext()); }
{puntero}               { return lexloc(sym.PUNTERO, yytext()); }

{asignacion}            { return lexloc(sym.ASIG, yytext()); }

{if}                    { return lexloc(sym.IF, yytext()); }
{then}                  { return lexloc(sym.THEN, yytext()); }
{else}                  { return lexloc(sym.ELSE, yytext()); }

{while}                 { return lexloc(sym.WHILE, yytext()); }
{do}                    { return lexloc(sym.DO, yytext()); }

{read}                  { return lexloc(sym.READ, yytext()); }
{write}                 { return lexloc(sym.WRITE, yytext()); }
{newline}               { return lexloc(sym.NL, yytext()); }

{new}                   { return lexloc(sym.NEW, yytext()); }
{delete}                { return lexloc(sym.DELETE, yytext()); }

{seq}                   { return lexloc(sym.SEQ, yytext()); }
{begin}                 { return lexloc(sym.BEGIN, yytext()); }
{end}                   { return lexloc(sym.END, yytext()); }

{booleano}              { return lexloc(sym.BOOLEANO, yytext()); }
{nulo}                  { return lexloc(sym.NULO, yytext()); }

{and}                   { return lexloc(sym.AND, yytext()); }
{or}                    { return lexloc(sym.OR, yytext()); }
{not}                   { return lexloc(sym.NOT, yytext()); }

{identificador}         { return lexloc(sym.ID, yytext()); }
{cadena}                { return lexloc(sym.CADENA, yytext()); }
{entero}                { return lexloc(sym.ENTERO, yytext()); }
{real}                  { return lexloc(sym.REAL, yytext()); }

{op_relacional}         { return lexloc(sym.OP_RELACIONAL, yytext()); }

{suma}                  { return lexloc(sym.SUMA, yytext()); }
{resta}                 { return lexloc(sym.RESTA, yytext()); }
{mul}                   { return lexloc(sym.MUL, yytext()); }
{div}                   { return lexloc(sym.DIV, yytext()); }
{mod}                   { return lexloc(sym.MOD, yytext()); }

{parentesis_apertura}   { return lexloc(sym.PAPERTURA, yytext()); }
{parentesis_cierre}     { return lexloc(sym.PCIERRE, yytext()); }
{corchete_apertura}     { return lexloc(sym.CAPERTURA, yytext()); }
{corchete_cierre}       { return lexloc(sym.CCIERRE, yytext()); }

{punto_y_coma}          { return lexloc(sym.PUNTO_COMA, yytext()); }
{coma}                  { return lexloc(sym.COMA, yytext()); }
{dos_puntos}            { return lexloc(sym.DOS_PUNTOS, yytext()); }
{punto}                 { return lexloc(sym.PUNTO, yytext()); }

[^]                     { throw new ELexico(new Strloc(yytext(), yyline + 1, yycolumn + 1), CADENA_INVALIDA); }