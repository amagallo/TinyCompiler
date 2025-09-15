package maquina;

import java.util.Deque;
import java.util.ArrayDeque;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;
import java.io.File;
import java.io.IOException;

import static tiny.Ejecutable.*;

import java.lang.reflect.RecordComponent;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.InvocationTargetException;

/**
    Implementación de una máquina virtual que contiene una pila de operaciones y distintos dispositivos de
    memoria, accesibles mediante direcciones virtuales con soporte en esta máquina.

    @version Java SE 17

    @author Félix Rodolfo Díaz Lorente
    @author Álvaro Magalló Paz
    @author Alejandro del Río Caballero
*/
public class MaquinaVirtual
{
    /* -------------------------- Definición de las excepciones de acceso a memoria -------------------------- */

    /**
        Dispositivos de memoria.
    */
    protected enum MEMORIA { PILA_OP, VIRTUAL };

    /**
        Clase excepción para los errores de ejecución de una máquina virtual.
    */
    protected static class EMemoria extends RuntimeException
    {
        private static final String FORMATO_MENSAJE = "Error en %s: %s";
        private static final String NOMBRE_MEMORIA[] = { "la pila de operaciones", "el mapa de memoria" };

        private static final String mensaje(MEMORIA mem, String descripcion)
        {
            return FORMATO_MENSAJE.formatted(NOMBRE_MEMORIA[mem.ordinal()], descripcion);
        }

        public EMemoria(MEMORIA mem, String descripcion, Throwable causa)
        {
            super(mensaje(mem, descripcion), causa);
        }

        public EMemoria(MEMORIA mem, String descripcion) { this(mem, descripcion, null); }
    }

    /* ---------------------------- Excepciones de acceso a memoria más comunes ---------------------------- */

    /**
        Error relacionado con la memoria virtual, o desde el punto de vista de los sistemas operativos, con el
        mapa de memoria.
    */
    protected static class EMapaMemoria extends EMemoria
    {
        private static final String FORMATO_MENSAJE = "Dirección de memoria no válida: %d";

        private static String mensaje(int dir) { return FORMATO_MENSAJE.formatted(dir); }

        public EMapaMemoria(String mensaje, Throwable causa) { super(MEMORIA.VIRTUAL, mensaje, causa); }

        public EMapaMemoria(int dir, Throwable causa) { this(mensaje(dir), causa); }

        public EMapaMemoria(String mensaje) { super(MEMORIA.VIRTUAL, mensaje); }

        public EMapaMemoria(int dir) { this(mensaje(dir)); }
    }

    /**
        Error de lectura/escritura de la pila de operaciones.
    */
    protected static class EPilaOp extends EMemoria
    {
        private static final String FORMATO_MENSAJE = "El tipo del valor a apilar no está permitido: %s";

        private static String mensaje(Class<?> clase) { return FORMATO_MENSAJE.formatted(clase.getSimpleName()); }

        public EPilaOp(String mensaje, Throwable causa) { super(MEMORIA.PILA_OP, mensaje, causa); }

        public EPilaOp(Class<?> clase, Throwable causa) { this(mensaje(clase), causa); }

        public EPilaOp(String mensaje) { super(MEMORIA.PILA_OP, mensaje); }

        public EPilaOp(Class<?> clase) { this(mensaje(clase)); }
    }

    /* -------------------- Definición de la estructuras de memoria de la máquina virtual -------------------- */

    /**
        Clase que implementa la pila de operaciones.
    */
    protected class PilaOp
    {
        private Deque<Object> pila = new ArrayDeque<>();

        public void apila(Object valor) { pila.addLast(valor); }

        public Object cima() { return pila.peekLast(); }

        public boolean esVacia() { return pila.isEmpty(); }

        public Object desapila()
        {
            if (pila.isEmpty())
                throw new EPilaOp("La pila está vacía.");
            else
                return pila.removeLast();
        }

        @Override
        public String toString() { return pila.toString(); }
    }

    /* ---------------------------- Estructuras de memoria de la máquina virtual ---------------------------- */
    
    // Pila de operaciones

    protected PilaOp pila;

    // Memoria estática

    protected RAM estatica;

    /**
        Nivel de anidamiento máximo en las llamadas recursivas de funciones. Extraído del límite por defecto que
        establecen los intérpretes del lenguaje de programación Python.
    */
    protected static final int MAX_RECURSION_POR_DEFECTO = 1000;

    // Memoria dinámica (heap)

    protected static final int TAM_HEAP = 1 << 18;
    
    protected GestorBloques heap;

    // Memoria de instrucciones

    protected InsMaquina[] listaIns;
    protected int pc = 0; // Contador de programa

    // Registros/Pila de activación

    public static final int REG_CONTROL = 2; // Número de registros de control
    protected int sp; // Puntero de pila

    // Displays

    protected int[] displays;

    // E/S estándar

    private static final boolean autoFlush = false;
    private final Charset codificacion;

    protected InputStream in;
    protected PrintStream out;
    protected PrintStream err;
    protected Scanner sc;

    // Regiones del mapa de memoria

    protected final int PAGINA_ESTATICA;
    protected final boolean esEstatica(int dir, int n) { return dir >= PAGINA_ESTATICA && dir + n <= PAGINA_HEAP; }

    protected final int PAGINA_HEAP;
    protected final boolean esHeap(int dir, int n) { return dir >= PAGINA_HEAP && dir + n <= TAM_MEMORIA_VIRTUAL; } 

    protected final int TAM_MEMORIA_VIRTUAL; // Tamaño de la memoria virtual

    /**
        Clase excepción para los errores de ejecución de una instrucción máquina.
    */
    protected static class EInstruccion extends RuntimeException
    {
        private static final String FORMATO_MENSAJE = "Dirección de instrucción no válida: %d";

        private static String mensaje(int dir) { return FORMATO_MENSAJE.formatted(dir); }

        public EInstruccion(String mensaje, Throwable causa) { super(mensaje, causa); }

        public EInstruccion(int dir, Throwable causa) { this(mensaje(dir), causa); }

        public EInstruccion(String mensaje) { this(mensaje, null); }

        public EInstruccion(int dir) { this(mensaje(dir), null); }
    }

    /**
        Instrucción del repertorio de instrucciones de esta máquina virtual, que proprciona métodos accesores a
        las estructuras de memoria que vienen implementadas por defecto en dicha máquina.
        
        @apiNote El repertorio debe estar sujeto a su ISA, la arquitectura del repertorio de instrucciones.
    */
    public interface InsMaquina
    {
        /**
            Proporciona la máquina virtual que va a lanzar a ejecución esta instrucción.
        
            @return una referencia a la máquina virtual sobre la que se va a ejecutar esta instrucción máquina.
        */
        MaquinaVirtual maquina();
        
        /**
            Lanza a ejecución esta instrucción máquina.

            @throws EInstruccion si se ha producido algún error durante la ejecución de la instrucción.
        */
        void ejecuta() throws EInstruccion;

        /* -------------- Métodos accesores a las estructuras de memoria de la máquina virtual -------------- */

        // Pila de operaciones

        default PilaOp pila() { return maquina().pila; }

        // Memoria estática

        default RAM estatica() { return maquina().estatica; }

        // Memoria dinámica (heap)

        default GestorBloques heap() { return maquina().heap; }

        // Regiones del mapa de memoria

        default int paginaEstatica() { return maquina().PAGINA_ESTATICA; }

        default boolean esEstatica(int dir) { return maquina().esEstatica(dir, 1); }
        default boolean esEstatica(int dir, int n) { return maquina().esEstatica(dir, n); }

        default int paginaHeap() { return maquina().PAGINA_HEAP; }

        default boolean esHeap(int dir) { return maquina().esHeap(dir, 1); }
        default boolean esHeap(int dir, int n) { return maquina().esHeap(dir, n); }

        default int espacioVirtual() { return maquina().TAM_MEMORIA_VIRTUAL; }

        // Instrucciones máquina

        default int numIns() { return maquina().listaIns.length; }
        
        // Contador de programa

        default void nuevoPC(int pc) { maquina().pc = pc; }
        default void incrPC() { ++maquina().pc; }
        default int pc() { return maquina().pc; }

        // Puntero de pila

        default void incrSP(int t) { maquina().sp += t; }
        default void decrSP(int t) { maquina().sp -= t; }

        // Displays

        default int display(int n) { return maquina().displays[n]; }
        default void display(int n, int dir) { maquina().displays[n] = dir; }

        // E/S estándar

        default Scanner sc() { return maquina().sc; }
        default PrintStream out() { return maquina().out; }
        default PrintStream err() { return maquina().err; }
    }

    protected MaquinaVirtual(int numRegistros, int tamActivacion, int recursionMax, int numDisplays,
        int numIns, InputStream in, PrintStream out, PrintStream err, Charset codificacion)
    {
        final int tamEstatica = numRegistros + tamActivacion * recursionMax;
        final int tamHeap = TAM_HEAP;

        pila = new PilaOp();
        heap = new FAT(tamHeap);

        estatica = new RAM(numRegistros, tamEstatica);
        estatica.reservar(numRegistros);

        sp = numRegistros;
        displays = new int[numDisplays];

        listaIns = new InsMaquina[numIns];
        
        PAGINA_ESTATICA     = 0;
        PAGINA_HEAP         = PAGINA_ESTATICA + tamEstatica;
        TAM_MEMORIA_VIRTUAL = PAGINA_HEAP + tamHeap;

        this.codificacion = codificacion;

        this.in  = in;
        this.out = new PrintStream(out, autoFlush, codificacion);
        this.err = new PrintStream(err, autoFlush, codificacion);
        
        this.sc = new Scanner(in);
    }

    protected MaquinaVirtual(int numRegistros, int tamActivacion, int recursionMax, int numDisplays,
        int numIns, Charset codificacion)
    {
        this(numRegistros, tamActivacion, recursionMax, numDisplays, numIns, System.in, System.out, System.err,
            codificacion);
    }

    protected MaquinaVirtual(int numRegistros, int tamActivacion, int numDisplays,
        int numIns, Charset codificacion)
    {
        this(numRegistros, tamActivacion, MAX_RECURSION_POR_DEFECTO, numDisplays, numIns, System.in, System.out,
            System.err, codificacion);
    }

    protected MaquinaVirtual(int numRegistros, int tamActivacion, int numDisplays, int numIns)
    {
        this(numRegistros, tamActivacion, numDisplays, numIns, StandardCharsets.UTF_16);
    }

    /**
        Introduce una nueva instrucción máquina al programa almacenado en esta máquina virtual, sustituyendo
        aquella apuntada por el contador de programa (PC).
        
        @apiNote Para añadir la instrucción al programa preservando el orden de ejecución, se debe cumplir que
        la expresión booleana {@code listaIns[pc] == null && (pc == 0 || listaIns[pc - 1] != null)} evalúe a
        <code>true</code>. Por eficiencia, derivamos la responsabilidad de verificar esta condición en el
        programador.
    
        @param ins la nueva instrucción de dicho programa
    */
    public final void lineaCodigo(InsMaquina ins) { listaIns[pc++] = ins; }

    /* ---------------------------------- Ejecución de la máquina virtual ---------------------------------- */

    /**
        Ejecuta el programa almacenado en la máquina virtual comenzando desde una instrucción dada por su índice
        asociado.
        
        @param comienzo índice de la instrucción del programa que inicia la ejecución

        @apiNote Antes de proceder a ejecutar el programa, el contador de programa (PC) toma el valor de
            <code>comienzo</code>, o cero si {@code comienzo < 0}.
    */
    public final void ejecutaDesde(int comienzo)
    {
        final int l = listaIns.length;

        pc = comienzo & ~(comienzo >> 31); // pc = (comienzo < 0)? 0: comienzo;
        while (pc < l)
            listaIns[pc].ejecuta();
    }

    /**
        Ejecuta el programa almacenado en esta máquina virtual desde el principio.

        @apiNote Este método es equivalente al siguiente código: {@code ejecutaDesde(0)}
    */
    public final void ejecuta() { ejecutaDesde(0); }

    /* ------------------------------- Impresión de la lista de instrucciones ------------------------------- */

    /**
        Imprime, en orden, las instrucciones del programa almacenado en la máquina virtual, empleando un flujo
        de salida dado.

        @param salida flujo de salida
    */
    public final void imprimeLista(PrintStream salida)
    {
        for (InsMaquina ins: listaIns)
        {
            Class<? extends InsMaquina> rec = ins.getClass();

            salida.print(rec.getSimpleName().toLowerCase());
            RecordComponent args[] = rec.getRecordComponents();

            final int n = args.length - 1;
            for (int i = 0; i < n; ++i)
            {
                salida.print(" ");
                try { salida.print(args[i].getAccessor().invoke(ins)); }
                catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e)
                {
                    imprimirExcepcion(e, true);
                }
            }

            // El último parámetro de cualquier instrucción debe ser una referencia a esta máquina virtual
            try { assert args[n].getAccessor().invoke(ins) == this; }
            catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e)
            {
                imprimirExcepcion(e, true); // Imprimimos la excepción surgida de este aserto
            }

            salida.print(System.lineSeparator()); // Imprime un salto de línea
        }
    }

    /**
        Escribe, en orden y en un fichero referido por su nombre, las instrucciones del programa almacenado en
        la máquina virtual.

        @param nombre nombre del fichero donde se escribirán las instrucciones del programa
    */
    public final void imprimeLista(String nombre)
    {
        try
        {
            File archivo = new File(nombre);
            archivo.createNewFile(); // Creamos el archivo si no lo encontramos

            imprimeLista(new PrintStream(nombre, codificacion));
        } catch (IOException e) { imprimirExcepcion(e, true); }
    }

    /**
        Imprime, en orden, las instrucciones del programa almacenado en la máquina virtual, utilizando su flujo
        de impresión.
    */
    public final void imprimeLista() { imprimeLista(out); }
}