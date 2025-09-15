package tiny;

import asint.parser;
import asint.sym;
import asint.Sintaxis.*;
import java_cup.runtime.Symbol;
import procesamientos.*;
import static procesamientos.GeneracionCodigo.maquina;
import procesamientos.Procesamiento.FASE;
import static procesamientos.Procesamiento.NOMBRE_FASE;
import static procesamientos.Procesamiento.error;

import static tiny.Ejecutable.MANEJADOR_EXCEPCION;
import static tiny.Ejecutable.EXIT_SUCCESS;
import static tiny.Ejecutable.EXIT_FAILURE;
import static tiny.Ejecutable.imprimirExcepcion;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;

public final class Procesador
{
    private static final int NUM_ARGS = 2;

    private static final int NUM_OPT = 1;

    private enum OPCION { LEX, SASC, SDESC, ASC, DESC };

    private static final String DELIM_OPCIONES = "|";

    private static final String EXTENSION = ".tiny";

    private static final String FORMATO_AYUDA = "[PARAMS] <opción de procesamiento (%s)> <archivo de extensión '%s'>";

    private static final String FORMATO_FASE = "Procesador en fase de %s . . .%n";

    private Procesador() {} // No se permite instanciar esta clase

    private static void excepcionParams()
    {
        Object[] optObj =
            Arrays.stream(OPCION.values()).map(opc -> opc.name().toLowerCase()).toArray();

        String[] opt = Arrays.copyOf(optObj, optObj.length, String[].class);
        
        String cadenaOpt = String.join(DELIM_OPCIONES, opt);
        throw new IllegalArgumentException(String.format(FORMATO_AYUDA, cadenaOpt, EXTENSION));
    }

    @SuppressWarnings("deprecation")
    public static void main(String[] args)
    {
        Thread.setDefaultUncaughtExceptionHandler(MANEJADOR_EXCEPCION);

        final int length = args.length;
        if (length < NUM_ARGS || length > NUM_ARGS + NUM_OPT)
            excepcionParams();

        OPCION opcion = null;
        try { opcion = OPCION.valueOf(args[0].toUpperCase()); }
        catch (IllegalArgumentException e) { excepcionParams(); }

        String archivo = args[1];
        if (!archivo.endsWith(EXTENSION))
            excepcionParams();
        
        Reader in = null;
        try { in = new InputStreamReader(new FileInputStream(archivo)); }
        catch (FileNotFoundException e) { imprimirExcepcion(e, true); }

        AnalizadorLexico lexico = null;
        parser sintaxisAsc = null;
        AnalizadorSintactico sintaxisDesc;
        Prog prog = null;

        FASE[] fases = FASE.values();
        for (int i = 0, n = fases.length; i < n; ++i)
        {
            System.out.printf(FORMATO_FASE, NOMBRE_FASE[i].toUpperCase());
            try
            {
                switch (fases[i])
                {
                case LEXICO:
                    switch (opcion)
                    {
                    case LEX:
                        lexico = new AnalizadorLexico(in);

                        Symbol simbolo;
                        for (simbolo = lexico.next_token(); simbolo.sym != sym.EOF; simbolo = lexico.next_token())
                            System.out.println(simbolo);

                        System.exit(EXIT_SUCCESS);
                    case SASC:
                    case ASC:
                        lexico = new AnalizadorLexico(in);
                        sintaxisAsc = new parser(lexico);
                        break;
                    case SDESC:
                    case DESC:
                        break;
                    }
                    break;
                case SINTAXIS:
                    switch (opcion)
                    {
                    case LEX:
                        break;
                    case SASC:
                        sintaxisAsc.parse();
                        System.exit(EXIT_SUCCESS);
                    case ASC:
                        prog = (Prog) sintaxisAsc.parse().value;
                        break;
                    case SDESC:
                        sintaxisDesc = new AnalizadorSintactico(in);
                        sintaxisDesc.Prog();
                        System.exit(EXIT_SUCCESS);
                    case DESC:
                        sintaxisDesc = new AnalizadorSintactico(in);
                        prog = sintaxisDesc.Prog();
                        break;
                    }
                    break;
                case VINCULACION:
                    Vinculacion.vincula(prog);
                    break;
                case TIPADO:
                    Tipado.tipo(prog);
                    break;
                case ASIG_MEMORIA:
                    Asignacion.asignaMemoria(prog);
                    break;
                case ETIQUETADO:
                    Etiquetado.etiqueta(prog);
                    break;
                case GEN_CODIGO:
                    GeneracionCodigo.generaCodigo(prog);
                }
            } catch (Exception e) { imprimirExcepcion(e, true); }

            if (error() > 0)
                System.exit(EXIT_FAILURE);
        }
        
        System.out.println();

        switch (length)
        {
            case NUM_ARGS:
                maquina.imprimeLista();
                System.out.println();
                break;
            default:
                maquina.imprimeLista(args[NUM_ARGS]);
        }
        
        maquina.ejecuta();
        System.exit(EXIT_SUCCESS);
    }
}