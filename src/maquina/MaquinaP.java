package maquina;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import asint.Sintaxis.Tipo;
import asint.Sintaxis.TipoBasico;
import asint.Sintaxis.Int;
import asint.Sintaxis.Real;
import asint.Sintaxis.Bool;

/**
    Repertorio de instrucciones de la <b>máquina P</b>, la máquina virtual sobre la que se ejecután los
    programas generados por nuestro procesador del lenguaje Tiny.

    @version Java SE 17
    @see MaquinaVirtual

    @author Félix Rodolfo Díaz Lorente
    @author Álvaro Magalló Paz
    @author Alejandro del Río Caballero
*/
public class MaquinaP extends MaquinaVirtual
{
    public MaquinaP(int numRegistros, int tamActivacion, int recursionMax, int numDisplays, int numIns,
        InputStream in, PrintStream out, PrintStream err, Charset codificacion)
    {
        super(numRegistros, tamActivacion, recursionMax, numDisplays, numIns, in, out, err, codificacion);
    }

    public MaquinaP(int numRegistros, int tamActivacion, int recursionMax, int numDisplays, int numIns,
        Charset codificacion)
    {
        super(numRegistros, tamActivacion, recursionMax, numDisplays, numIns, codificacion);
    }

    public MaquinaP(int numRegistros, int tamActivacion, int numDisplays, int numIns, Charset codificacion)
    {
        super(numRegistros, tamActivacion, numDisplays, numIns, codificacion);
    }

    public MaquinaP(int numRegistros, int tamActivacion, int tamHeap, int numDisplays, int numIns)
    {
        super(numRegistros, tamActivacion, numDisplays, numIns);
    }

    private static final List<Class<?>> CLASES_PERMITIDAS =
        List.of(Integer.class, Double.class, Boolean.class, String.class, Character.class);
    private static boolean estaPermitida(Class<?> clase) { return CLASES_PERMITIDAS.contains(clase); }

    /**
        Mensaje de error para direcciones de memorias inválidas, extraídas principalmente de la pila de
        operaciones.
    */
    private static final String DIR_INVALIDA = "La cima no es una dirección válida";

    /**
        Mensaje de error para tipos inválidos de elementos extraídos o introducidos en la pila de operaciones
        durante la ejecución de una instrucción.
    */
    private static final String OP_INVALIDO = "El tipo de uno de los operandos no es válido.";

    public static record Apila(Object valor, MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            final Class<?> clase = valor.getClass();

            if (estaPermitida(clase))
            {
                pila().apila(valor);
                incrPC();
            }
            else
                throw new EPilaOp(clase);
        }
    }

    public static record ApilaInd(MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            Object cima = pila().desapila();
            if (cima instanceof Integer dir)
            {
                try { Objects.checkIndex(dir, espacioVirtual()); }
                catch (IndexOutOfBoundsException e) { throw new EMapaMemoria(dir); }

                Object valor =
                    esEstatica(dir)? estatica().leerBloque(dir - paginaEstatica()):
                    /* esHeap(dir)? */ heap().leerBloque(dir - paginaHeap());

                pila().apila(valor);
                incrPC();
            }
            else
                throw new EPilaOp(DIR_INVALIDA);
        }
    }

    public static record DesapilaInd(MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            Object valor = pila().desapila();
            Object dir = pila().desapila();

            if (dir instanceof Integer d)
            {
                try { Objects.checkIndex(d, espacioVirtual()); }
                catch (IndexOutOfBoundsException e) { throw new EMapaMemoria(d); }

                if (esEstatica(d))
                    estatica().escribirBloque(d - paginaEstatica(), valor);
                else // esHeap(d)
                    heap().escribirBloque(d - paginaHeap(), valor);
                
                incrPC();
            }
            else
                throw new EPilaOp(DIR_INVALIDA);
        }
    }

    public static record Mueve(int n, MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            Object d1 = pila().desapila();
            Object d0 = pila().desapila();

            if (d0 instanceof Integer id0 && d1 instanceof Integer id1)
            {
                final int tam = espacioVirtual();

                try { Objects.checkIndex(id0, tam); }
                catch (IndexOutOfBoundsException e) { throw new EMapaMemoria(id0); }
                
                try { Objects.checkIndex(id1, tam); }
                catch (IndexOutOfBoundsException e) { throw new EMapaMemoria(id1); }

                final int pagEstatica = paginaEstatica();
                RAM estatica = maquina().estatica;

                final int pagHeap = paginaHeap();
                GestorBloques heap = maquina().heap;

                final boolean esEstatica0 = esEstatica(id0, n), esEstatica1 = esEstatica(id1, n);
                final boolean esHeap0 = esHeap(id0, n), esHeap1 = esHeap(id1, n);

                if (esEstatica0 && esEstatica1)
                    estatica.mover(id0 - pagEstatica, id1 - pagEstatica, n);
                else if (esEstatica0 && esHeap1)
                {
                    Object[] bloques = heap.leer(id1 - pagHeap, n);
                    estatica.escribir(id0 - pagEstatica, bloques);
                }
                else if (esHeap0 && esHeap1)
                    heap.mover(id0 - pagHeap, id1 - pagHeap, n);
                else if (esHeap0 && esEstatica1)
                {
                    Object[] bloques = estatica.leer(id1 - pagEstatica, n);
                    heap.escribir(id0 - pagHeap, bloques);
                }
                else
                    throw new EMapaMemoria("Se ha traspasado la frontera entre dos regiones de memoria");

                incrPC();
            }
            else
                throw new EPilaOp(DIR_INVALIDA);
        }
    }

    public static record Ira(int d, MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            try { Objects.checkIndex(d, numIns()); }
            catch (IndexOutOfBoundsException e) { throw new EInstruccion(d); }

            nuevoPC(d);
        }
    }

    public static record Irf(int d, MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            try { Objects.checkIndex(d, numIns()); }
            catch (IndexOutOfBoundsException e) { throw new EInstruccion(d); }

            boolean cond = (boolean) pila().desapila();
            nuevoPC(cond? pc() + 1: d);
        }
    }

    public static record Irv(int d, MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            try { Objects.checkIndex(d, numIns()); }
            catch (IndexOutOfBoundsException e) { throw new EInstruccion(d); }

            boolean cond = (boolean) pila().desapila();
            nuevoPC(cond? d: pc() + 1);
        }
    }

    public static record Irind(MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            Object cima = pila().desapila();

            if (cima instanceof Integer dir)
            {
                try { Objects.checkIndex(dir, numIns()); }
                catch (IndexOutOfBoundsException e) { throw new EInstruccion(dir); }
                
                nuevoPC(dir);
            }
            else
                throw new EPilaOp(DIR_INVALIDA);
        }
    }

    public static record Alloc(int n, MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            pila().apila(heap().alojar(n) + paginaHeap());
            incrPC();
        }
    }

    public static record Dealloc(int n, MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            Object cima = pila().desapila();
            if (cima instanceof Integer dir)
            {
                heap().destruir(dir - paginaHeap(), n);
                incrPC();
            }
        }
    }

    public static record Activa(int n, int t, int d, MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            final RAM estatica = maquina().estatica;

            estatica.pushBack(d);
            estatica.pushBack((int) display(n - 1));

            incrSP(REG_CONTROL);
            pila().apila(maquina().sp);

            incrSP(t);
            estatica.reservar(maquina().sp);

            incrPC();
        }
    }

    public static record Apilad(int n, MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            pila().apila(display(n - 1));
            incrPC();
        }
    }

    public static record Desapilad(int n, MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            Object cima = pila().desapila();

            if (cima instanceof Integer dir)
            {
                display(n - 1, dir);
                incrPC();
            }
            else
                throw new EPilaOp(DIR_INVALIDA);
        }
    }

    public static record Desactiva(int n, int t, MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            decrSP(t + REG_CONTROL);
            
            final int sp = maquina().sp;
            final RAM estatica = maquina().estatica;

            pila().apila(estatica.leerBloque(sp));
            display(n - 1, (int) estatica.leerBloque(sp + 1));
            estatica.borrar(sp);

            incrPC();
        }
    }

    public static record Dup(MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            if (!pila().esVacia())
                pila().apila(pila().cima());
            incrPC();
        }
    }

    public static record Stop(String error, MaquinaVirtual maquina) implements InsMaquina
    {
        private static final String FORMATO_ERROR = "Ejecución abortada: [ERROR] %s";
        
        public Stop(MaquinaVirtual maquina) { this("", maquina); }

        public void ejecuta()
        {
            nuevoPC(numIns());
            if (!"".equals(error))
                throw new EInstruccion(FORMATO_ERROR.formatted(error));
        }
    }

    public static record InfijoBinario<T, U, R>(BiFunction<T, U, R> func, String rep)
    {
        @Override
        public String toString() { return rep; }
    }

    public static record InfijoUnario<T, R>(Function<T, R> func, String simbolo)
    {
        @Override
        public String toString() { return simbolo; }
    }

    // Comparadores

    public static final InfijoBinario<Object, Object, Boolean> EQ =
        new InfijoBinario<>((x, y) -> x.equals(y), "==");
    
    public static final InfijoBinario<Object, Object, Boolean> NE =
        new InfijoBinario<>((x, y) -> !x.equals(y), "!=");

    public static final InfijoBinario<? extends Comparable<Object>, ? extends Comparable<Object>, Boolean> LE =
        new InfijoBinario<>((x, y) -> x.compareTo(y) <= 0, "<=");

    public static final InfijoBinario<? extends Comparable<Object>, ? extends Comparable<Object>, Boolean> GE =
        new InfijoBinario<>((x, y) -> x.compareTo(y) >= 0, ">=");
    
    public static final InfijoBinario<? extends Comparable<Object>, ? extends Comparable<Object>, Boolean> LT =
        new InfijoBinario<>((x, y) -> x.compareTo(y) < 0, "<");
    
    public static final InfijoBinario<? extends Comparable<Object>, ? extends Comparable<Object>, Boolean> GT =
        new InfijoBinario<>((x, y) -> x.compareTo(y) > 0, ">");

    // Operadores booleanos

    public static final InfijoBinario<Boolean, Boolean, Boolean> AND = new InfijoBinario<>((x, y) -> x && y, "and");
    public static final InfijoBinario<Boolean, Boolean, Boolean> OR  = new InfijoBinario<>((x, y) -> x || y, "or");

    public static final InfijoUnario<Boolean, Boolean> NOT = new InfijoUnario<>(x -> !x, "not");

    // Operadores aritméticos

    public static final InfijoBinario<Integer, Integer, Integer> SUMA_ENT =
        new InfijoBinario<>((x, y) -> x + y, "+");
    public static final InfijoBinario<Double, Double, Double> SUMA_REAL =
        new InfijoBinario<>((x, y) -> x + y, "+");
    
    public static final InfijoBinario<Integer, Integer, Integer> RESTA_ENT =
        new InfijoBinario<>((x, y) -> x - y, "-");
    public static final InfijoBinario<Double, Double, Double> RESTA_REAL =
        new InfijoBinario<>((x, y) -> x - y, "-");
    
    public static final InfijoBinario<Integer, Integer, Integer> MUL_ENT =
        new InfijoBinario<>((x, y) -> x * y, "*");
    public static final InfijoBinario<Double, Double, Double> MUL_REAL =
        new InfijoBinario<>((x, y) -> x * y, "*");
    
    public static final InfijoBinario<Integer, Integer, Integer> DIV_ENT =
        new InfijoBinario<>((x, y) -> x / y, "/");
    public static final InfijoBinario<Double, Double, Double> DIV_REAL =
        new InfijoBinario<>((x, y) -> x / y, "/");

    public static final InfijoBinario<Integer, Integer, Integer> MOD = new InfijoBinario<>((x, y) -> x % y, "%");

    public static final InfijoUnario<Integer, Integer> NEG_ENT = new InfijoUnario<>(x -> -x, "-");
    public static final InfijoUnario<Double, Double> NEG_REAL  = new InfijoUnario<>(x -> -x, "-");

    public static record OpUnaria<T, R>(InfijoUnario<T, R> op, MaquinaVirtual maquina) implements InsMaquina
    {
        @SuppressWarnings("unchecked")
        public void ejecuta()
        {
            try
            {
                T valor = (T) pila().desapila();
                R res = op.func().apply(valor);

                final Class<?> clase = res.getClass();
                if (estaPermitida(clase))
                {
                    pila().apila(res);
                    incrPC();
                }
                else
                    throw new EPilaOp(clase);
            } catch (ClassCastException e) { throw new EPilaOp(OP_INVALIDO); }
        }
    }

    public static record OpBinaria<T, U, R>(InfijoBinario<T, U, R> op, MaquinaVirtual maquina) implements InsMaquina
    {
        @SuppressWarnings("unchecked")
        public void ejecuta() 
        {
            try
            {
                U valor1 = (U) pila().desapila();
                T valor0 = (T) pila().desapila();
                R res = op.func().apply(valor0, valor1);

                final Class<?> clase = res.getClass();
                if (estaPermitida(clase))
                {
                    pila().apila(res);
                    incrPC();
                }
                else
                    throw new EPilaOp(clase);
            } catch (ClassCastException e) { throw new EPilaOp(OP_INVALIDO); }
        }
    }

    public static record PromReal(MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            Object valor = pila().cima();

            if (valor instanceof Integer)
                pila().apila(Double.valueOf((int) pila().desapila()));
            else if (!(valor instanceof Number))
                throw new EInstruccion("La cima no es un valor numérico");
            
            incrPC();
        }
    }

    public static record Escanea(Tipo tipo, MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            if (tipo instanceof TipoBasico basico)
            {
                String linea = sc().nextLine();

                Object valor = (basico instanceof Int)? Integer.valueOf(linea):
                    (basico instanceof Real)? Double.valueOf(linea):
                    (basico instanceof Bool)? Boolean.valueOf(linea):
                    /* (tipo instanceof Cadena)? */ linea;

                pila().apila(valor);
                incrPC();
            }
            else
                throw new EInstruccion(OP_INVALIDO);
        }
    }

    public static record Imprime(MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            Object valor = pila().desapila();
            out().print(valor);
            incrPC();
        }
    }

    public static record Endl(MaquinaVirtual maquina) implements InsMaquina
    {
        public void ejecuta()
        {
            out().print(System.lineSeparator());
            out().flush();
            incrPC();
        }
    }
}