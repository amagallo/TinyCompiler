package procesamientos;

import static asint.Sintaxis.*;

import java.util.List;
import java.util.Iterator;

/**
    Clase utilidad que contiene todos los métodos requeridos para ejecutar la fase de <b>etiquetado</b> en el
    procesamiento de nuestro lenguaje Tiny.
    
    @version Java SE 17
    @see Procesamiento

    @author Félix Rodolfo Díaz Lorente
    @author Álvaro Magalló Paz
    @author Alejandro del Río Caballero
*/
public class Etiquetado extends Procesamiento
{
    private Etiquetado() {} // No se permite instanciar esta clase

    public static void etiqueta(INodo nodo) { procesa(Etiquetado.class, "etiqueta", nodo); }

    public static void etiqueta(List<? extends INodo> lista) { procesa(Etiquetado.class, "etiqueta", lista); }

    // Contador de instrucciones

    private static int tag = 0;

    static int numInstrucciones() { return tag; }

    // Métodos auxiliares

    private static int bool2int(boolean valor) { return valor? 1: 0; }

    // Programa

    public static void etiqueta(Prog prog)
    {
        prog.etiquetaInicio(tag);

        etiqueta(prog.lins());
        tag += 1;

        List<Proc> procs = recolectaProcs(prog);
        procs.forEach(proc -> etiqueta(proc));
        
        prog.etiquetaSig(tag);
    }

    // Declaraciones

    public static void etiqueta(Proc proc)
    {
        proc.etiquetaInicio(tag);
        etiqueta(proc.lins());
        tag += 2;
        proc.etiquetaSig(tag);
    }

    // Instrucciones básicas

    public static void etiqueta(Asig asig)
    {
        final Exp ei = asig.ei(), ed = asig.ed();

        asig.etiquetaInicio(tag);
        etiqueta(ei);
        etiqueta(ed);
        tag += (ei.tipo() instanceof Real && ed.tipo() instanceof Int)? bool2int(esDesig(ed)) + 2: 1;
        asig.etiquetaSig(tag);
    }

    public static void etiqueta(Invoc invoc)
    {
        final Proc proc = (Proc) invoc.eid().vinculo();
        List<? extends Param> lparam = proc.lparam();

        invoc.etiquetaInicio(tag);
        tag += 1;

        Iterator<? extends Param> itp = lparam.iterator();
        Iterator<? extends Exp> ite = invoc.lexp().iterator();
        while (itp.hasNext() && ite.hasNext())
        {
            Param pf = itp.next();
            Exp pr = ite.next();

            tag += 3;
            etiqueta(pr);
            if (pf instanceof Pval)
                tag += (pf.tipo() instanceof Real && pr.tipo() instanceof Int)? bool2int(esDesig(pr)) + 2: 1;
            else
                tag += 1;
        }

        tag += 2;
        invoc.etiquetaSig(tag);
    }

    // Instrucciones de bloque 

    public static void etiqueta(Seq seq)
    {
        seq.etiquetaInicio(tag);
        etiqueta(seq.lins());
        seq.etiquetaSig(tag);
    }

    public static void etiqueta(IfThen ifThen)
    {
        final Exp exp = ifThen.exp();

        ifThen.etiquetaInicio(tag);
        etiqueta(exp);
        tag += bool2int(esDesig(exp)) + 1;
        etiqueta(ifThen.lins());
        ifThen.etiquetaSig(tag);
    }

    public static void etiqueta(IfThenElse ifThenElse)
    {
        final Exp exp = ifThenElse.exp();

        ifThenElse.etiquetaInicio(tag);
        etiqueta(exp);
        tag += bool2int(esDesig(exp)) + 1;
        etiqueta(ifThenElse.lins());
        tag += 1;
        etiqueta(ifThenElse.lins2());
        ifThenElse.etiquetaSig(tag);
    }

    public static void etiqueta(While wh)
    {
        final Exp exp = wh.exp();

        wh.etiquetaInicio(tag);
        etiqueta(exp);
        tag += bool2int(esDesig(exp)) + 1;
        etiqueta(wh.lins());
        tag += 1;
        wh.etiquetaSig(tag);
    }
    
    // Gestión de la E/S estándar

    public static void etiqueta(Read read)
    {
        read.etiquetaInicio(tag);
        etiqueta(read.exp());
        tag += 2;
        read.etiquetaSig(tag);
    }
    
    public static void etiqueta(Write write)
    {   
        final Exp exp = write.exp();

        write.etiquetaInicio(tag);
        etiqueta(exp);
        tag += bool2int(esDesig(exp)) + 1;
        write.etiquetaSig(tag);
    }

    public static void etiqueta(NewLine newLine)
    {
        newLine.etiquetaInicio(tag);
        tag += 1;
        newLine.etiquetaSig(tag);
    }
    
    // Gestión de la memoria dinámica 

    public static void etiqueta(New n)
    {
        n.etiquetaInicio(tag);
        etiqueta(n.exp());
        tag += 2;
        n.etiquetaSig(tag);
    }

    public static void etiqueta(Delete d)
    {
        d.etiquetaInicio(tag);
        etiqueta(d.exp());
        tag += 7;
        d.etiquetaSig(tag);
    }

    // Expresiones básicas

    public static void etiqueta(ExpLiteral basica)
    {
        basica.etiquetaInicio(tag);
        tag += 1;
        basica.etiquetaSig(tag);
    }

    public static void etiqueta(Ident ident)
    {
        final INodo vinculo = ident.vinculo();

        ident.etiquetaInicio(tag);
        tag += (vinculo.nivel() == 0)? 1: bool2int(vinculo instanceof Pvar) + 3;
        ident.etiquetaSig(tag);
    }

    // Expresiones constantes

    public static void etiqueta(ExpCte cte)
    {
        cte.etiquetaInicio(tag);
        tag += 1;
        cte.etiquetaSig(tag);
    }
    
    // Expresiones compuestas
    
    public static void etiqueta(ExpBinaria exp)
    {
        final Exp op1 = exp.op1(), op2 = exp.op2();
        Tipo t = refFact(exp.tipo());

        exp.etiquetaInicio(tag);
        etiqueta(op1);
        tag += bool2int(esDesig(op1)) + bool2int(t instanceof Real && refFact(op1.tipo()) instanceof Int);
        etiqueta(op2);
        tag += bool2int(esDesig(op2)) + bool2int(t instanceof Real && refFact(op2.tipo()) instanceof Int) + 1;
        exp.etiquetaSig(tag);
    }

    public static void etiqueta(ExpUnaria exp)
    {
        final Exp op1 = exp.op();

        exp.etiquetaInicio(tag);
        etiqueta(op1);
        tag += bool2int(esDesig(op1)) + 1;
        exp.etiquetaSig(tag);
    }

    // Operadores de acceso a memoria

    public static void etiqueta(Indx indx)
    {
        final Exp ei = indx.ei();

        indx.etiquetaInicio(tag);
        etiqueta(indx.eid());
        etiqueta(ei);
        tag += bool2int(esDesig(ei)) + 3;
        indx.etiquetaSig(tag);
    }

    public static void etiqueta(Acc acc)
    {
        acc.etiquetaInicio(tag);
        etiqueta(acc.reg());
        tag += 2;
        acc.etiquetaSig(tag);
    }
    
    public static void etiqueta(Indir indir)
    {
        indir.etiquetaInicio(tag);
        etiqueta(indir.et());
        tag += 6;
        indir.etiquetaSig(tag);
    }
}