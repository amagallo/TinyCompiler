package procesamientos;

import asint.Sintaxis.*;

import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.lang.reflect.Method;

import static tiny.Ejecutable.*;

import java.lang.reflect.InvocationTargetException;

/**
    Clase base para todos los procesamientos del lenguaje Tiny, con métodos de utilidad para su implementación.

    @version Java SE 17

    @author Félix Rodolfo Díaz Lorente
    @author Álvaro Magalló Paz
    @author Alejandro del Río Caballero
*/
public class Procesamiento
{
    /**
        Diccionario que almacena los objetos representantes de métodos, direccionable por el nombre del método y
        el objeto que representa la clase de su único parámetro.
        
        @apiNote Este diccionario sirve de estructura de memorización, para optimizar en tiempo la invocación
            de los métodos de procesamiento, pues evita la repetición de llamadas a librerías de programación
            reflexiva (una técnica en la que se fundamenta la programación dinámica).
    */
    private static Map<String, Map<Class<? extends INodo>, Method>> tm = new HashMap<>();

    /**
        Enumerado que incluye todas las fases del procesamiento del lenguaje Tiny.
    */
    public enum FASE { LEXICO, SINTAXIS, VINCULACION, TIPADO, ASIG_MEMORIA, ETIQUETADO, GEN_CODIGO };

    public static final String NOMBRE_FASE[] =
        { "análisis léxico", "análisis sintáctico", "vinculación", "tipado", "asignación de memoria",
            "etiquetado", "generación de código" };

    protected Procesamiento() {} // No se permite instanciar esta clase fuera de su jerarquía de clases

    /**
        Excepción lanzada cuando ocurre un error fatal en una de las fases de procesamiento del lenguaje.
    */
    protected static class EProcesamiento extends RuntimeException
    {
        private static final String FORMATO_MENSAJE = "Error de %s en %s.";

        private static final String FORMATO_MENSAJE_STR = "Error de %s originado por el símbolo %s.";
        
        /**
            Genera el mensaje de error de esta excepción.
        
            @param fase fase de procesamiento en la que ha sucedido el error
            @param strloc cadena localizada con el símbolo que ha originado el error
        
            @return El mensaje de error correspondiente a esta excepción.
        */
        private static String mensaje(FASE fase, Strloc strloc)
        {
            final String nombreFase = NOMBRE_FASE[fase.ordinal()], str = strloc.str();
            final String FORMATO = (str == null)? FORMATO_MENSAJE: FORMATO_MENSAJE_STR;

            return FORMATO.formatted(nombreFase, strloc.toString());
        }

        public EProcesamiento(FASE fase, Strloc strloc, Throwable causa)
        {
            super(mensaje(fase, strloc), causa);
        }

        public EProcesamiento(FASE fase, Strloc strloc)
        {
            super(mensaje(fase, strloc), null);
        }
    }

    private static int ERROR = 0;

    public static final int error() { return ERROR; }

    /**
        Devuelve el diccionario de objetos representantes de métodos asociado al nombre de un método.

        @param nombre nombre del método

        @return el diccionario de objetos representantes de métodos y direccionable por el objeto que representa
            la clase del único parámetro del método llamado <code>nombre</code>.
    */
    private static Map<Class<? extends INodo>, Method> tablaLocal(String nombre)
    {
        Map<Class<? extends INodo>, Method> dic = tm.get(nombre);
        if (dic == null)
        {
            dic = new HashMap<Class<? extends INodo>, Method>();
            tm.put(nombre, dic);
        }

        return dic;
    }

    private static void procesa(Class<? extends Procesamiento> clase, Map<Class<? extends INodo>, Method> tml,
        String nombre, INodo nodo)
    {
        final Class<? extends INodo> claseRaiz = nodo.getClass();

        Class<?> claseNodo = claseRaiz;
        while (claseNodo != INodo.class)
        {
            Method metodo = tml.get(claseNodo); // Tratamos de obtener el método almacenado en el diccionario
            if (metodo == null) // Si no está el método o bien es vacío
            {
                // Si el método es vacío, no hacemos nada más que añadirlo al diccionario si no estaba
                if (tml.containsKey(claseNodo))
                {
                    if (claseNodo != claseRaiz)
                        tml.put(claseRaiz, null);
                    return;
                }

                try {
                    // Obtenemos el método por programación reflexiva
                    metodo = clase.getDeclaredMethod(nombre, claseNodo);
                } catch (NoSuchMethodException e) {
                    // Subimos en la jerarquía de interfaces
                    Class<?> interfaces[] = claseNodo.getInterfaces();
                    if (interfaces.length == 0) // Si no hay más interfaces que consultar, se produce un error
                        break;

                    assert interfaces.length == 1; // Cada récord no puede implementar más de una interfaz

                    // Consideramos siempre la primera interfaz, puesto que por el diseño de la sintaxis
                    // abstracta, no permitimos que un récord implemente dos o más interfaces
                    claseNodo = interfaces[0]; 
                    continue;
                }

                // Tratamos de suprimir el control de accesos en cada invocación como medida de optimización
                metodo.setAccessible(true);
                // Añadimos el método al diccionario, direccionado por la clase original
                tml.put(claseRaiz, metodo);
            }

            try {
                metodo.invoke(null, nodo); // Invocamos al método estático
            }
            catch (InvocationTargetException e)
            {
                imprimirExcepcion(e.getCause(), false);
                ++ERROR;
            }
            catch (IllegalAccessException | SecurityException e) { imprimirExcepcion(e, true); }

            return;
        }
    }

    /**
        Intenta invocar un método <b>estático</b> que procesa un nodo dado. Si no existe ese método, este
        procedimiento no tiene ningún efecto externo. 

        @apiNote La implementación de este método hace uso de técnicas de la programación reflexiva para
            maximizar la escabilidad y mantenibilidad del código, aunque quizás a costa de un coste en tiempo
            de ejecución mayor.

        @param clase objeto que representa la clase donde se encuentra el método a invocar
        @param nombre nombre del método a invocar, en cadena de caracteres
        @param nodo nodo a procesar por el método dado
    */
    protected static void procesa(Class<? extends Procesamiento> clase, String nombre, INodo nodo)
    {
        procesa(clase, tablaLocal(nombre), nombre, nodo);
    }

    /**
        Intenta invocar un método <b>estático</b> que procesa un nodo dado. Si no existe ese método, este
        procedimiento no tiene ningún efecto externo.

        @apiNote La implementación de este método hace uso de técnicas de la programación reflexiva para
            maximizar la escabilidad y mantenibilidad del código, aunque quizás a costa de un coste en tiempo
            de ejecución mayor.

        @param clase objeto que representa la clase donde se encuentra el método a invocar
        @param nombre nombre del método a invocar, en cadena de caracteres
        @param lista lista cuyos elementos hay que procesar mediante el método dado
    */
    protected static void procesa(Class<? extends Procesamiento> clase, String nombre,
        List<? extends INodo> lista)
    {
        Map<Class<? extends INodo>, Method> tml = tablaLocal(nombre);
        lista.forEach(elem -> procesa(clase, tml, nombre, elem));
    }

    /* --------------------------------- Métodos auxiliares sobre nodos AST --------------------------------- */

    private static final List<Class<? extends INodo>> DESIG =
        List.of(Ident.class, Indx.class, Acc.class, Indir.class);

    /**
        Indica si un nodo dado es un <b>designador</b>.
    
        @param nodo nodo del árbol de sintáxis abstracta (AST)
    
        @return <code>true</code> si solo si dicho nodo debe ser considerado como designador según la
            especificación de nuestro lenguaje Tiny.
    */
    protected static boolean esDesig(INodo nodo) { return DESIG.contains(nodo.getClass()); }

    protected static Tipo refFact(Tipo t)
    {
        while (t instanceof Ref)
            t = t.vinculo().tipo();
        return t;
    }

    private static List<Proc> procs = null;

    private static void introducirProcs(List<? extends Dec> ldec)
    {
        for (Dec dec: ldec)
        {
            if (dec instanceof Proc proc)
                procs.add(proc);
        }
    }

    protected static List<Proc> recolectaProcs(Prog prog)
    {
        if (procs == null)
        {
            procs = new ArrayList<>();
            int primero = 0;

            List<? extends Dec> ldec = prog.ldec();
            introducirProcs(ldec);

            while (primero != procs.size())
            {
                ldec = procs.get(primero++).ldec();
                introducirProcs(ldec);
            }
        }

        return Collections.unmodifiableList(procs);
    }
}