package asint;

import java.util.List;

/**
    Clase que define los contructores de los nodos del AST para nuestra versión del lenguaje Tiny. Se utilizan
    atributos de tipo <code>Record</code> junto con interfaces para reducir el número de líneas de código y así
    aumentar su mantenibilidad.

    @version Java SE 17

    @author Félix Rodolfo Díaz Lorente
    @author Álvaro Magalló Paz
    @author Alejandro del Río Caballero
*/
public class Sintaxis
{
    private Sintaxis() {} // No se permite instanciar esta clase

    // Cadena localizada
    public static record Strloc(String str, int linea, int col)
    {
        private static final String FORMATO_POSICION = "(l:%d; c:%d)";
        private static final String FORMATO_CADENA = "\"%s\" (l:%d; c:%d)";

        @Override
        public String toString()
        {
            return (str == null)? FORMATO_POSICION.formatted(linea, col): FORMATO_CADENA.formatted(str, linea, col);
        }
    }
    
    /**
        Interfaz común a los nodos de sintáxis abstracta.
    */
    public interface INodo
    {
        Nodo nodo();

        default String id() { return null; }

        default Strloc strloc() { return new Strloc(id(), nodo().linea, nodo().col); }

        /* ---------------------------- Representación textual de los nodos AST ---------------------------- */

        // Nombre del nodo AST

        default String nombre() { return getClass().getSimpleName().toLowerCase(); }

        // Representación literal del tipo del nodo AST
        
        default String litTipo() { return tipo().nombre(); }

        /* --------- Azúcar sintáctico para la lectura/escritura de los atributos de los nodos AST --------- */
        
        // Vinculación y tipado

        default INodo vinculo() { return nodo().vinculo; }
        default Tipo tipo()     { return nodo().tipo; }

        default void vincular(INodo vinculo) { nodo().vinculo = vinculo; }
        default void tipado(Tipo tipo)       { nodo().tipo = tipo; }

        // Asignación de memoria

        default int dir()            { return nodo().dir; }
        default int nivel()          { return nodo().nivel; }
        default int espacio()        { return nodo().tam; }
        default int desplazamiento() { return nodo().desp; }

        default void asignaDir(int dir)     { nodo().dir = dir; }
        default void asignaNivel(int nivel) { nodo().nivel = nivel; }
        default void asignaEspacio(int tam) { nodo().tam = tam; }
        default void asignaDesp(int desp)   { nodo().desp = desp; }

        // Etiquetado
        
        default int inicio() { return nodo().inicio; }
        default int sig()    { return nodo().sig; }

        default void etiquetaInicio(int inicio) { nodo().inicio = inicio; }
        default void etiquetaSig(int sig)       { nodo().sig = sig; }
    }

    public static class Nodo
    {
        /**
            Entero que representa un valor sin inicializar.
        */
        public static final int VALOR_NULO = -1;

        /**
            Nivel más bajo en el anidamiento de llamadas a procedimientos y funciones.
        */
        public static final int MIN_NIVEL = 0;

        public final int linea, col;

        public INodo vinculo;             // Vinculación
        public Tipo tipo;                 // Tipado
        public int nivel, dir, tam, desp; // Asignación de espacio
        public int inicio, sig;           // Etiquetado

        public Nodo(int linea, int col, INodo vinculo, Tipo tipo, int nivel, int dir, int tam, int inicio,
            int sig)
        {
            this.linea = linea; this.col = col;

            this.vinculo = vinculo; this.tipo = tipo;
            this.nivel = nivel; this.dir = dir; this.tam = tam;
            this.inicio = inicio; this.sig = sig;
        }

        public Nodo(int linea, int col, INodo vinculo, Tipo tipo, int dir, int tam, int inicio, int sig)
        {
            this(linea, col, vinculo, tipo, MIN_NIVEL, dir, tam, inicio, sig);
        }

        public Nodo(int linea, int col)
        {
            this.linea = linea; this.col = col;

            vinculo = null; tipo = null;
            dir = tam = inicio = sig = VALOR_NULO;
        }

        private Nodo() { this(VALOR_NULO, VALOR_NULO); }
    }

    private static final Nodo NODO_VACIO = new Nodo();

    // Programa

    public static record Prog(List<? extends Dec> ldec, List<? extends Ins> lins, Nodo nodo) implements INodo {}

    // Declaraciones básicas

    public interface Dec extends INodo {}

    public static record Type(String id, Tipo tipo, Nodo nodo) implements Dec {}

    public static record Var(String id, Tipo tipo, Nodo nodo) implements Dec {}

    public static record Proc(String id, List<? extends Param> lparam, List<? extends Dec> ldec,
        List<? extends Ins> lins, Nodo nodo) implements Dec {}

    // Parámetros formales

    public interface Param extends Dec
    {
        Tipo tipo();
    }

    public static record Pval(String id, Tipo tipo, Nodo nodo) implements Param {}

    public static record Pvar(String id, Tipo tipo, Nodo nodo) implements Param {}

    // Tipos

    public interface Tipo extends INodo
    {
        default Tipo tipo() { return this; }
    }

    // Tipos básicos

    public interface TipoBasico extends Tipo {}

    public static record Int(Nodo nodo) implements TipoBasico
    {
        @Override
        public String toString() { return litTipo(); }
    }

    public static record Bool(Nodo nodo) implements TipoBasico
    {
        @Override
        public String toString() { return litTipo(); }
    }

    public static record Real(Nodo nodo) implements TipoBasico
    {
        @Override
        public String toString() { return litTipo(); }
    }

    public static record Str(Nodo nodo) implements TipoBasico
    {
        @Override
        public String toString() { return litTipo(); }
    }

    private static record Null(Nodo nodo) implements TipoBasico {}
    private static record Error(Nodo nodo) implements TipoBasico {}
    private static record Ok(Nodo nodo) implements TipoBasico {}

    // Singleton de tipos elementales

    public static final Tipo INT    = new Int(NODO_VACIO);
    public static final Tipo BOOL   = new Bool(NODO_VACIO);
    public static final Tipo REAL   = new Real(NODO_VACIO);
    public static final Tipo STRING = new Str(NODO_VACIO);

    public static final Tipo NULL  = new Null(NODO_VACIO);
    public static final Tipo OK    = new Ok(NODO_VACIO);
    public static final Tipo ERROR = new Error(NODO_VACIO);

    // Otros tipos
    
    private static final String TIPO_COMPUESTO = "%s<%s>", SEPARADOR = ", ";

    public interface TipoCompuesto extends Tipo
    {
        default List<String> litTipos() { return List.of(tipo().nombre()); }

        @Override
        default String litTipo() { return TIPO_COMPUESTO.formatted(nombre(), String.join(SEPARADOR, litTipos())); }
    }
    
    // Tipos renombrados

    public static record Ref(String id, Nodo nodo) implements TipoCompuesto {}

    // Tipos compuestos

    public static record Array(Tipo tipo, String tam, Nodo nodo) implements TipoCompuesto {}
    
    public static record Reg(List<Campo> lcampo, Nodo nodo) implements TipoCompuesto
    {
        @Override
        public List<String> litTipos() { return lcampo.stream().map(c -> c.litTipo()).toList(); }
    }

    public static record Puntero(Tipo tipo, Nodo nodo) implements TipoCompuesto {}
        
    // Campos de datos

    public static record Campo(String id, Tipo tipo, Nodo nodo) implements INodo {}

    // Instrucciones básicas

    public interface Ins extends INodo {}

    public static record Asig(Exp ei, Exp ed, Nodo nodo) implements Ins {}
    
    public static record Invoc(Exp eid, List<? extends Exp> lexp, Nodo nodo) implements Ins {}

    // Instrucciones de bloque

    public static record Seq(List<? extends Dec> ldec, List<? extends Ins> lins, Nodo nodo) implements Ins {}

    public static record IfThen(Exp exp, List<? extends Ins> lins, Nodo nodo) implements Ins {}

    public static record IfThenElse(Exp exp, List<? extends Ins> lins, List<? extends Ins> lins2, Nodo nodo) implements Ins {}

    public static record While(Exp exp, List<? extends Ins> lins, Nodo nodo) implements Ins {}

    // Gestión de la E/S estándar

    public static record Read(Exp exp, Nodo nodo) implements Ins {}
    
    public static record Write(Exp exp, Nodo nodo) implements Ins {}

    public static record NewLine(Nodo nodo) implements Ins {}

    // Gestión de la memora dinámica

    public interface InsMemoria extends Ins
    {
        Exp exp();
    }

    public static record New(Exp exp, Nodo nodo) implements InsMemoria {}

    public static record Delete(Exp exp, Nodo nodo) implements InsMemoria {}

    // Expresiones

    public interface Exp extends INodo {}

    // Expresiones básicas

    public interface ExpLiteral extends Exp
    {
        String lit();

        default String id() { return lit(); }
    }

    public static record Entero(String lit, Nodo nodo) implements ExpLiteral {}

    public static record Decimal(String lit, Nodo nodo) implements ExpLiteral {}

    public static record Cadena(String lit, Nodo nodo) implements ExpLiteral {}

    public static record Ident(String lit, Nodo nodo) implements ExpLiteral {}

    // Expresiones constantes

    public interface ExpCte extends Exp {}

    public static record True(Nodo nodo) implements ExpCte {}

    public static record False(Nodo nodo) implements ExpCte {}

    public static record Nulo(Nodo nodo) implements ExpCte {}

    // Expresiones unarias

    public interface ExpUnaria extends Exp
    {
        Exp op();
    }
    
    // Expresiones binarias

    public interface ExpBinaria extends Exp
    {
        Exp op1();
        Exp op2();
    }

    public interface ExpIgualdad extends ExpBinaria {} // Expresión de igualdad
    
    public interface ExpRel extends ExpIgualdad {} // Operadores relacionales

    public interface ExpAritReal extends ExpBinaria {} // Expresión aritmética real

    public interface ExpDiscreta extends ExpBinaria {} // Expresión aritmética discreta

    public interface ExpLogica extends ExpDiscreta {} // Expresión lógica binaria

    public interface ExpAritEnt extends ExpDiscreta {} // Expresión aritmética entera

    // Operadores relacionales

    public static record Eq(Exp op1, Exp op2, Nodo nodo) implements ExpRel {}

    public static record Ne(Exp op1, Exp op2, Nodo nodo) implements ExpRel {}

    public static record Le(Exp op1, Exp op2, Nodo nodo) implements ExpRel {}

    public static record Ge(Exp op1, Exp op2, Nodo nodo) implements ExpRel {}

    public static record Lt(Exp op1, Exp op2, Nodo nodo) implements ExpRel {}
    
    public static record Gt(Exp op1, Exp op2, Nodo nodo) implements ExpRel {}

    // Operadores lógicos

    public static record And(Exp op1, Exp op2, Nodo nodo) implements ExpLogica {}

    public static record Or(Exp op1, Exp op2, Nodo nodo) implements ExpLogica {}
    
    public static record Not(Exp op, Nodo nodo) implements ExpUnaria {}

    // Operadores aritméticos

    public static record Suma(Exp op1, Exp op2, Nodo nodo) implements ExpAritReal {}

    public static record Resta(Exp op1, Exp op2, Nodo nodo) implements ExpAritReal {}

    public static record Neg(Exp op, Nodo nodo) implements ExpUnaria {}

    public static record Mul(Exp op1, Exp op2, Nodo nodo) implements ExpAritReal {}

    public static record Div(Exp op1, Exp op2, Nodo nodo) implements ExpAritReal {}
    
    public static record Mod(Exp op1, Exp op2, Nodo nodo) implements ExpAritEnt {}

    // Acceso a memoria

    public static record Indx(Exp eid, Exp ei, Nodo nodo) implements Exp {}

    public static record Acc(Exp reg, String id, Nodo nodo) implements Exp {}

    public static record Indir(Exp et, Nodo nodo) implements Exp {}
}