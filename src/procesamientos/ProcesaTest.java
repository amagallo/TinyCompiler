package procesamientos;

import java.util.List;

import asint.Sintaxis.INodo;
import asint.Sintaxis.Nodo;

import static tiny.Ejecutable.MANEJADOR_EXCEPCION;

final class ProcesaTest extends Procesamiento // Para testar los métodos de procesamiento
{
    private ProcesaTest() {}

    private static final String DEFAULT_OPT = "d"; // Comando para ejecutar la implementación por defecto

    // Interfaces y records de prueba
    protected static interface A extends INodo {}
    protected static interface B extends INodo {}
    protected static interface C extends INodo {}
    protected static interface D extends INodo {}

    protected static record E(Nodo nodo) implements A {}
    protected static record F(Nodo nodo) implements B {}
    protected static record G(Nodo nodo) implements C {}
    protected static record H(Nodo nodo) implements D {}

    // Métodos sobrecargados de prueba
    public static void imprime(A a) { System.out.println("A"); }
    public static void imprime(B b) { System.out.println("B"); }
    public static void imprime(C c) { System.out.println("C"); }
    public static void imprime(D d) { System.out.println("D"); }

    public static void main(String[] argv)
    {
        Thread.setDefaultUncaughtExceptionHandler(MANEJADOR_EXCEPCION);

        List<? extends INodo> list = List.of(new E(null), new F(null), new G(null), new H(null),
            new E(null), new F(null), new G(null), new H(null));
        
        long start = 0L;
        if (argv.length == 0)
        {
            start = System.nanoTime();
            procesa(ProcesaTest.class, "imprime", list);
        }
        else if (DEFAULT_OPT.equals(argv[0])) // Código equivalente sin programación reflexiva
        {
            start = System.nanoTime();
            for (INodo elem: list)
            {
                //imprime(elem); // Error de compilación (no existe 'imprime' para instancias de la clase Nodo)
                
                if (elem instanceof A)
                    imprime((A) elem);
                else if (elem instanceof B)
                    imprime((B) elem);
                else if (elem instanceof C)
                    imprime((C) elem);
                else if (elem instanceof D)
                    imprime((D) elem);
                else if (elem instanceof E)
                    imprime((E) elem);
                else if (elem instanceof F)
                    imprime((F) elem);
                else if (elem instanceof G)
                    imprime((G) elem);
                else if (elem instanceof H)
                    imprime((H) elem);
            }
        }

        System.out.println("Tiempo (ms): " + (System.nanoTime() - start) / 1e6);
    }
}