package procesamientos;

import asint.Sintaxis.*;

import java.util.List;

/**
    Clase utilidad que contiene todos los métodos requeridos para ejecutar la fase de <b>asignación de memoria</b>
    en el procesamiento de nuestro lenguaje Tiny.
    
    @version Java SE 17
    @see Procesamiento

    @author Félix Rodolfo Díaz Lorente
    @author Álvaro Magalló Paz
    @author Alejandro del Río Caballero
*/
public class Asignacion extends Procesamiento
{
    private Asignacion() {} // No se permite instanciar esta clase

    public static void asignaMemoria(INodo nodo)  { procesa(Asignacion.class, "asignaMemoria", nodo); }
    public static void asignaMemoria1(INodo nodo) { procesa(Asignacion.class, "asignaMemoria1", nodo); }
    public static void asignaMemoria2(INodo nodo) { procesa(Asignacion.class, "asignaMemoria2", nodo); }

    public static void asignaMemoria(List<? extends INodo> lista)  { procesa(Asignacion.class, "asignaMemoria", lista); }
    public static void asignaMemoria1(List<? extends INodo> lista) { procesa(Asignacion.class, "asignaMemoria1", lista); }
    public static void asignaMemoria2(List<? extends INodo> lista) { procesa(Asignacion.class, "asignaMemoria2", lista); }

    private static int dir = 0; // Contador de direcciones
    
    private static int registros = 0; // Número de registros (variables globales)

    private static int nivel = 0, maxNivel = 0; // Nivel de anidamiento
    private static int local = 0, maxLocal = 0; // Tamaño de las variables locales

    static int maxNivel()     { return maxNivel; }
    static int numRegistros() { return registros; }
    static int espacioLocal() { return maxLocal; }

    // Programa

    public static void asignaMemoria(Prog prog)
    {
        asignaMemoria(prog.ldec());
        asignaMemoria(prog.lins());
    }    

    // Declaraciones básicas

    public static void asignaMemoria(Var var)
    {
        var.asignaDir(dir);
        var.asignaNivel(nivel);

        Tipo tipo = var.tipo();
        asignaMemoria(tipo);

        int tam = tipo.espacio();
        if (nivel == 0)
            registros += tam;
        else
            local += tam;
        
        dir += tam;
    }

    public static void asignaMemoria(Type type) { asignaMemoria(type.tipo()); }

    public static void asignaMemoria(Proc proc)
    {
        proc.asignaDir(dir);
        final int prevDir = dir;

        proc.asignaNivel(++nivel);
        if (nivel > maxNivel)
            maxNivel = nivel;
        
        dir = 0;
        asignaMemoria(proc.lparam());
        asignaMemoria(proc.ldec());
        asignaMemoria(proc.lins());

        if (local > maxLocal)
            maxLocal = local;
        
        proc.asignaEspacio(dir);
        
        --nivel;
        local -= dir;
        dir = prevDir;
    }

    // Parámetros formales

    public static void asignaMemoria(Pvar pvar)
    { 
        pvar.asignaDir(dir);
        pvar.asignaNivel(nivel);
        
        asignaMemoria(pvar.tipo());
        local += 1; dir += 1;
    }
    
    public static void asignaMemoria(Pval pval)
    {
        pval.asignaDir(dir);
        pval.asignaNivel(nivel);
        
        Tipo tipo = pval.tipo();
        asignaMemoria(tipo);

        int espacio = tipo.espacio();
        local = espacio; dir += espacio;
    }

    // Tipos

    public static void asignaMemoria(Tipo tipo)
    {
        if (tipo.espacio() == Nodo.VALOR_NULO)
        {
            asignaMemoria1(tipo);
            asignaMemoria2(tipo);
        }
    }

    // Tipos básicos 
    
    public static void asignaMemoria1(TipoBasico basico) { basico.asignaEspacio(1); }

    // Tipos renombrados

    public static void asignaMemoria1(Ref ref)
    {
        INodo vinculo = ref.vinculo();
        Tipo tipo = vinculo.tipo();

        asignaMemoria(tipo);
        ref.asignaEspacio(tipo.espacio());
    }

    // Tipos compuestos

    public static void asignaMemoria1(Array array)
    {
        asignaMemoria(array.tipo());
        array.asignaEspacio(Integer.valueOf(array.tam()) * array.tipo().espacio());
    }

    public static void asignaMemoria1(Puntero punt)
    {
        punt.asignaEspacio(1);

        Tipo tipo = punt.tipo();
        if (!(tipo instanceof Ref))
            asignaMemoria1(tipo);
    }

    public static void asignaMemoria2(Puntero punt)
    {
        Tipo tipo = punt.tipo();
        if (tipo instanceof Ref)
            asignaMemoria(tipo);
        else
            asignaMemoria2(tipo);
    }
    
    public static void asignaMemoria1(Reg reg)
    {
        int tam = 0; // Variable acumuladora para el tamaño de registro, que optimiza su cálculo
        
        List<Campo> lcampo = reg.lcampo();
        for (Campo c: lcampo)
        {
            c.asignaDesp(tam);

            Tipo tipo = c.tipo();
            asignaMemoria1(tipo);
            tam += tipo.espacio();
        }

        reg.asignaEspacio(tam);
    }

    public static void asignaMemoria2(Reg reg)
    {
        List<Campo> lcampo = reg.lcampo();
        for (Campo c: lcampo)
            asignaMemoria2(c.tipo());
    }

    // Instrucciones de bloque
    
    public static void asignaMemoria(Seq seq)
    {
        asignaMemoria(seq.ldec());
        asignaMemoria(seq.lins());
    }

    public static void asignaMemoria(IfThen ifThen) { asignaMemoria(ifThen.lins()); }

    public static void asignaMemoria(IfThenElse ifThenElse)
    {
        asignaMemoria(ifThenElse.lins());
        asignaMemoria(ifThenElse.lins2());
    }
    
    public static void asignaMemoria(While wh) { asignaMemoria(wh.lins()); }
}