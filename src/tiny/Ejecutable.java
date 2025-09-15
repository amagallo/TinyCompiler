package tiny;

import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;

public class Ejecutable
{
    // Nombres extraídos de las dos macros del lenguaje C que indican el estado de terminación del programa
    public static final int EXIT_SUCCESS = 0, EXIT_FAILURE = 1;

    public static class ManejadorExcepcion implements UncaughtExceptionHandler
    {
        private PrintStream err;

        public ManejadorExcepcion(PrintStream err) { this.err = err; }

        private ManejadorExcepcion() { this(System.err); }

        public void uncaughtException(Thread t, Throwable e) { imprimirExcepcion(e, err, false); }
    }

    public static final UncaughtExceptionHandler MANEJADOR_EXCEPCION = new ManejadorExcepcion();

    protected Ejecutable() {} // No se permite instanciar esta clase de forma externa

    /**
        Determina si el programa en ejecución está en modo depuración (en inglés, <i>debug</i>).

        @return <code>true</code> si solo si el programa en ejecución está siendo depurado.
    */
    private static boolean modoDepuracion()
    {
        List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg: args)
        {
            // Comprobamos si está establecido el protocolo "Java Debug Wire Protocol" (JDWP)
            if (arg.startsWith("-agentlib:jdwp"))
                return true;
        }
        return false;
    }

    private static void establecerTraza(Throwable e)
    {
        Object[] traza = modoDepuracion()? Arrays.stream(e.getStackTrace())
            .filter(t -> !t.getClassName().matches("(java|jdk)\\..*")).toArray(): new StackTraceElement[0];
        
        e.setStackTrace(Arrays.copyOf(traza, traza.length, StackTraceElement[].class));
    }

    public static void imprimirExcepcion(Throwable e, PrintStream err, boolean salir)
    {
        final Throwable causa = e.getCause();

        establecerTraza(e);
        if (causa != null)
            establecerTraza(causa);
        e.printStackTrace(err);
        
        if (salir)
            System.exit(EXIT_FAILURE);
    }

    public static void imprimirExcepcion(Throwable e, boolean salir) { imprimirExcepcion(e, System.err, salir); }
}