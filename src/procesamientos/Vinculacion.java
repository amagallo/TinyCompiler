package procesamientos;

import asint.Sintaxis.*;

import java.util.List;

/**
    Clase utilidad que contiene todos los métodos requeridos para ejecutar la fase de <b>vinculación</b> en el
    procesamiento de nuestro lenguaje Tiny.
    
    @version Java SE 17
    @see Procesamiento

    @author Félix Rodolfo Díaz Lorente
    @author Álvaro Magalló Paz
    @author Alejandro del Río Caballero
*/
public class Vinculacion extends Procesamiento
{
    private static TablaSimbolos ts = new TablaSimbolos();

    private static class EIdentificador extends Throwable
    {
        public EIdentificador(String mensaje) { super(mensaje); }
    }
    
    private static class EIdNoDeclarado extends EIdentificador
    {
        public EIdNoDeclarado(String mensaje) { super(mensaje); }
    }

    private static class EIdDuplicado extends EIdentificador
    {
        public EIdDuplicado(String mensaje) { super(mensaje); }
    }
    
    // Azúcar sintáctico para los errores de vinculación más frecuentes
    private static final EIdentificador ID_NO_DECLARADO = new EIdNoDeclarado("Identificador no declarado");
    private static final EIdentificador ID_DUPLICADO    = new EIdDuplicado("Identificador duplicado");

    private static class EVinculacion extends EProcesamiento
    {
        public EVinculacion(Strloc strloc, EIdentificador causa) { super(FASE.VINCULACION, strloc, causa); }

        public EVinculacion(Strloc strloc) { super(FASE.VINCULACION, strloc); }
    }

    private Vinculacion() {} // No se permite instanciar esta clase

    public static void vincula(INodo nodo)  { procesa(Vinculacion.class, "vincula", nodo); }
    public static void vincula1(INodo nodo) { procesa(Vinculacion.class, "vincula1", nodo); }
    public static void vincula2(INodo nodo) { procesa(Vinculacion.class, "vincula2", nodo); }

    public static void vincula(List<? extends INodo> lista)  { procesa(Vinculacion.class, "vincula", lista); }
    public static void vincula1(List<? extends INodo> lista) { procesa(Vinculacion.class, "vincula1", lista); }
    public static void vincula2(List<? extends INodo> lista) { procesa(Vinculacion.class, "vincula2", lista); }

    // Programa

    public static void vincula(Prog prog)
    {
        vincula1(prog.ldec());
        vincula2(prog.ldec());
        vincula(prog.lins());
    }

    // Declaraciones básicas

    private static void recolecta(Strloc strloc, INodo nodo)
    {
        String id = strloc.str();

        if (ts.contiene(id))
            throw new EVinculacion(strloc, ID_DUPLICADO);
        ts.inserta(id, nodo);
    }

    public static void vincula1(Var var)
    {
        vincula1(var.tipo());
        recolecta(var.strloc(), var);
    }

    public static void vincula2(Var var) { vincula2(var.tipo()); }

    public static void vincula1(Type type)
    {
        vincula1(type.tipo());
        recolecta(type.strloc(), type);
    }

    public static void vincula2(Type type) { vincula2(type.tipo()); }

    public static void vincula1(Proc proc)
    {
        recolecta(proc.strloc(), proc);

        ts.abreNivel();
        vincula1(proc.lparam());
        vincula1(proc.ldec());
        vincula2(proc.lparam());
        vincula2(proc.ldec());
        vincula(proc.lins());
        ts.cierraNivel();
    }

    // Parámetros formales

    public static void vincula1(Param param)
    {
        vincula1(param.tipo());
        recolecta(param.strloc(), param);
    }

    public static void vincula2(Param param) { vincula2(param.tipo()); }

    // Tipos renombrados

    public static void vincula1(Ref ref)
    {
        INodo nodo = ts.valorDe(ref.id());
        if (nodo == null)
            throw new EVinculacion(ref.strloc(), ID_NO_DECLARADO);
        ref.vincular(nodo);
    }

    public static void vincula1(Array array) { vincula1(array.tipo()); }

    public static void vincula2(Array array) { vincula2(array.tipo()); }

    public static void vincula1(Puntero punt)
    { 
        Tipo tipo = punt.tipo();
        
        if (!(tipo instanceof Ref))
            vincula1(tipo);
    }

    public static void vincula2(Puntero punt)
    {
        Tipo tipo = punt.tipo();
        if (tipo instanceof Ref ref)
        {
            INodo nodo = ts.valorDe(ref.id());
            if (nodo == null)
                throw new EVinculacion(punt.strloc(), ID_NO_DECLARADO);
            else
                tipo.vincular(nodo);
        }
        else 
            vincula2(tipo);
    }
    
    // Campos de datos

    public static void vincula1(Reg reg) { vincula1(reg.lcampo()); }

    public static void vincula1(Campo campo) { vincula1(campo.tipo()); }

    public static void vincula2(Reg reg) { vincula2(reg.lcampo()); }

    public static void vincula2(Campo campo) { vincula2(campo.tipo()); }
    
    // Instrucciones básicas

    public static void vincula(Asig asig)
    {
        vincula(asig.ei());
        vincula(asig.ed());
    }

    public static void vincula(Invoc invoc)
    {
        vincula(invoc.eid());
        vincula(invoc.lexp());
    }

    // Instrucciones de bloque
    
    public static void vincula(Seq seq)
    {
        ts.abreNivel();
        vincula1(seq.ldec());
        vincula2(seq.ldec());
        vincula(seq.lins());
        ts.cierraNivel();
    }
    
    public static void vincula(IfThen ifThen)
    {
        vincula(ifThen.exp());
        vincula(ifThen.lins());
    }

    public static void vincula(IfThenElse ifThenElse)
    {
        vincula(ifThenElse.exp());
        vincula(ifThenElse.lins());
        vincula(ifThenElse.lins2());
    }
    
    public static void vincula(While wh)
    {
        vincula(wh.exp());
        vincula(wh.lins());
    }

    // Gestión de la E/S estándar

    public static void vincula(Read read) { vincula(read.exp()); }

    public static void vincula(Write write) { vincula(write.exp()); }

    // Gestión de la memora dinámica

    public static void vincula(InsMemoria mem) { vincula(mem.exp()); }
    
    // Expresiones básicas

    public static void vincula(Ident exp)
    {
        INodo nodo = ts.valorDe(exp.lit());
        if (nodo == null)
            throw new EVinculacion(exp.strloc(), ID_NO_DECLARADO);
        else
            exp.vincular(nodo);
    }

    // Expresiones compuestas

    public static void vincula(ExpUnaria exp) { vincula(exp.op()); }

    public static void vincula(ExpBinaria exp)
    {
        vincula(exp.op1());
        vincula(exp.op2());
    }
    
    // Operadores posfijos de acceso a memoria

    public static void vincula(Indx indx)
    {
        vincula(indx.eid());
        vincula(indx.ei());
    }

    public static void vincula(Acc acc) { vincula(acc.reg()); }

    public static void vincula(Indir indir) { vincula(indir.et()); }
}