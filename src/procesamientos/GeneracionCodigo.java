package procesamientos;

import static asint.Sintaxis.*;
import static asint.Sintaxis.Nodo.VALOR_NULO;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static maquina.MaquinaVirtual.REG_CONTROL;
import static maquina.MaquinaP.*;

import maquina.MaquinaP;
import maquina.MaquinaVirtual;

/**
    Clase utilidad que contiene todos los métodos requeridos para ejecutar la fase de <b>generación de código</b>
    en el procesamiento de nuestro lenguaje Tiny.
    
    @version Java SE 17
    @see Procesamiento

    @author Félix Rodolfo Díaz Lorente
    @author Álvaro Magalló Paz
    @author Alejandro del Río Caballero
*/
public class GeneracionCodigo extends Procesamiento
{
    private GeneracionCodigo() {} // No se permite instanciar esta clase

    public static void generaCodigo(INodo nodo) { procesa(GeneracionCodigo.class, "generaCodigo", nodo); }

    public static void generaCodigo(List<? extends INodo> lista) { procesa(GeneracionCodigo.class, "generaCodigo", lista); }

    public static final MaquinaVirtual maquina = new MaquinaP(Asignacion.numRegistros(),
        Asignacion.espacioLocal() + REG_CONTROL * Asignacion.maxNivel(), Asignacion.maxNivel(),
        Etiquetado.numInstrucciones(), StandardCharsets.UTF_8);
    
    // Funciones auxiliares

    private static int tamBase(INodo nodo) { return refFact(nodo.tipo()).tipo().espacio(); }

    private static void compruebaNulo(int sig)
    {
        maquina.lineaCodigo(new Dup(maquina));
        maquina.lineaCodigo(new Apila(VALOR_NULO, maquina));
        maquina.lineaCodigo(new OpBinaria<>(EQ, maquina));
        maquina.lineaCodigo(new Irf(sig, maquina));
        maquina.lineaCodigo(new Stop("[ERROR] Intento de acceso a través de null", maquina));
    }

    // Programa

    public static void generaCodigo(Prog prog)
    {
        generaCodigo(prog.lins());
        maquina.lineaCodigo(new Stop(maquina));
        
        List<Proc> procs = recolectaProcs(prog);
        procs.forEach(proc -> generaCodigo(proc));
    }

    // Declaraciones

    public static void generaCodigo(Proc proc)
    {
        generaCodigo(proc.lins());

        maquina.lineaCodigo(new Desactiva(proc.nivel(), proc.espacio(), maquina));
        maquina.lineaCodigo(new Irind(maquina));
    }

    // Instrucciones básicas
    
    public static void generaCodigo(Asig asig)
    {
        final Exp ei = asig.ei(), ed = asig.ed();

        generaCodigo(ei);
        generaCodigo(ed);
        
        if (ei.tipo() instanceof Real && ed.tipo() instanceof Int)
        {
            if (esDesig(ed))
                maquina.lineaCodigo(new ApilaInd(maquina));
            maquina.lineaCodigo(new PromReal(maquina));
            maquina.lineaCodigo(new DesapilaInd(maquina));
        }
        else if (esDesig(ed))
            maquina.lineaCodigo(new Mueve(ei.tipo().espacio(), maquina));
        else 
            maquina.lineaCodigo(new DesapilaInd(maquina));
    }

    public static void generaCodigo(Invoc invoc)
    {
        final Proc proc = (Proc) invoc.eid().vinculo();
        List<? extends Param> lparam = proc.lparam();

        maquina.lineaCodigo(new Activa(proc.nivel(), proc.espacio(), invoc.sig(), maquina));

        Iterator<? extends Param> itp = lparam.iterator();
        Iterator<? extends Exp> ite = invoc.lexp().iterator();
        while (itp.hasNext() && ite.hasNext())
        {
            // Paso de parámetros

            Param pf = itp.next();
            Exp pr = ite.next();

            maquina.lineaCodigo(new Dup(maquina));
            maquina.lineaCodigo(new Apila(pf.dir(), maquina));
            maquina.lineaCodigo(new OpBinaria<>(SUMA_ENT, maquina));

            generaCodigo(pr);
            
            if (pf instanceof Pval)
            {
                if (pf.tipo() instanceof Real && pr.tipo() instanceof Int)
                {
                    if (esDesig(pr))
                        maquina.lineaCodigo(new ApilaInd(maquina));
                    maquina.lineaCodigo(new PromReal(maquina));
                    maquina.lineaCodigo(new DesapilaInd(maquina));
                }
                else if (esDesig(pr))
                    maquina.lineaCodigo(new Mueve(pf.tipo().espacio(), maquina));
                else
                    maquina.lineaCodigo(new DesapilaInd(maquina));
            }
            else
                maquina.lineaCodigo(new DesapilaInd(maquina));
        }

        maquina.lineaCodigo(new Desapilad(proc.nivel(), maquina));
        maquina.lineaCodigo(new Ira(proc.inicio(), maquina));
    }

    // Instrucciones de bloque 

    public static void generaCodigo(Seq seq) { generaCodigo(seq.lins()); }

    public static void generaCodigo(IfThen ifThen)
    {
        final Exp exp = ifThen.exp();
        generaCodigo(exp);

        if (esDesig(exp))
            maquina.lineaCodigo(new ApilaInd(maquina));

        maquina.lineaCodigo(new Irf(ifThen.sig(), maquina));
        generaCodigo(ifThen.lins());
    }
    
    public static void generaCodigo(IfThenElse ifThenElse)
    {
        final Exp exp = ifThenElse.exp();
        generaCodigo(exp);
        
        if (esDesig(exp))
            maquina.lineaCodigo(new ApilaInd(maquina));

        List<? extends Ins> lins2 = ifThenElse.lins2();
        maquina.lineaCodigo(new Irf(lins2.get(0).inicio(), maquina));
        generaCodigo(ifThenElse.lins());
        maquina.lineaCodigo(new Ira(ifThenElse.sig(), maquina));
        generaCodigo(lins2);
    }
    
    public static void generaCodigo(While wh)
    {
        final Exp exp = wh.exp();
        generaCodigo(exp);

        if (esDesig(exp))
            maquina.lineaCodigo(new ApilaInd(maquina));
        
        maquina.lineaCodigo(new Irf(wh.sig(), maquina));
        generaCodigo(wh.lins());
        maquina.lineaCodigo(new Ira(wh.inicio(), maquina));
    }

    // Gestión de la E/S estándar
    
    public static void generaCodigo(Read read)
    {
        final Exp exp = read.exp();

        generaCodigo(exp);
        maquina.lineaCodigo(new Escanea(exp.tipo(), maquina));
        maquina.lineaCodigo(new DesapilaInd(maquina));
    }

    public static void generaCodigo(Write write)
    {
        final Exp exp = write.exp();

        generaCodigo(exp);
        if (esDesig(exp))
            maquina.lineaCodigo(new ApilaInd(maquina));
        maquina.lineaCodigo(new Imprime(maquina));
    }

    public static void generaCodigo(NewLine nl) { maquina.lineaCodigo(new Endl(maquina)); }
    
    // Gestión de la memoria dinámica

    public static void generaCodigo(New n)
    {
        final Exp exp = n.exp();

        generaCodigo(exp);
        maquina.lineaCodigo(new Alloc(tamBase(exp), maquina)); 
        maquina.lineaCodigo(new DesapilaInd(maquina));
    }

    public static void generaCodigo(Delete d)
    {
        final Exp exp = d.exp();

        generaCodigo(exp);
        maquina.lineaCodigo(new ApilaInd(maquina));
        compruebaNulo(d.sig() - 1);
        maquina.lineaCodigo(new Dealloc(tamBase(exp), maquina));
    }

    // Expresiones básicas

    public static void generaCodigo(Entero ent)
    {
        maquina.lineaCodigo(new Apila(Integer.valueOf(ent.lit()), maquina));
    } 

    public static void generaCodigo(Decimal dec)
    {
        maquina.lineaCodigo(new Apila(Double.valueOf(dec.lit()), maquina));
    }
    
    public static void generaCodigo(Cadena cad)
    {
        maquina.lineaCodigo(new Apila(cad.lit(), maquina));
    }

    public static void generaCodigo(Ident ident)
    {
        final INodo vinculo = ident.vinculo();

        if (vinculo.nivel() == 0)
            maquina.lineaCodigo(new Apila(vinculo.dir(), maquina));
        else
        {
            maquina.lineaCodigo(new Apilad(vinculo.nivel(), maquina));
            maquina.lineaCodigo(new Apila(vinculo.dir(), maquina));
            maquina.lineaCodigo(new OpBinaria<>(SUMA_ENT, maquina));

            if (vinculo instanceof Pvar)
                maquina.lineaCodigo(new ApilaInd(maquina));
        }
    }

    // Expresiones constantes

    public static void generaCodigo(True t)  { maquina.lineaCodigo(new Apila(true, maquina)); }

    public static void generaCodigo(False f) { maquina.lineaCodigo(new Apila(false, maquina)); }

    public static void generaCodigo(Nulo n)  { maquina.lineaCodigo(new Apila(VALOR_NULO, maquina)); }

    // Operadores relacionales

    private static void opBinaria(ExpBinaria exp, InfijoBinario<?, ?, ?> op)
    {
        final Exp op1 = exp.op1(), op2 = exp.op2();
        Tipo t = refFact(exp.tipo());

        generaCodigo(op1);
        if (esDesig(op1))
            maquina.lineaCodigo(new ApilaInd(maquina));
        if (t instanceof Real && refFact(op1.tipo()) instanceof Int)
            maquina.lineaCodigo(new PromReal(maquina));

        generaCodigo(op2);
        if (esDesig(op2))
            maquina.lineaCodigo(new ApilaInd(maquina));
        if (t instanceof Real && refFact(op2.tipo()) instanceof Int)
            maquina.lineaCodigo(new PromReal(maquina));
        
        maquina.lineaCodigo(new OpBinaria<>(op, maquina));
    }

    private static void opUnaria(ExpUnaria exp, InfijoUnario<?, ?> op)
    {
        final Exp op1 = exp.op();

        generaCodigo(op1);
        if (esDesig(op1))
            maquina.lineaCodigo(new ApilaInd(maquina));
        
        maquina.lineaCodigo(new OpUnaria<>(op, maquina));
    }

    public static void generaCodigo(Eq eq) { opBinaria(eq, EQ); }
    
    public static void generaCodigo(Ne ne) { opBinaria(ne, NE); }
    
    public static void generaCodigo(Le le) { opBinaria(le, LE); }
    
    public static void generaCodigo(Lt lt) { opBinaria(lt, LT); }
    
    public static void generaCodigo(Ge ge) { opBinaria(ge, GE); }
    
    public static void generaCodigo(Gt gt) { opBinaria(gt, GT); } 
    
    // Operadores lógicos

    public static void generaCodigo(And and) { opBinaria(and, AND); }

    public static void generaCodigo(Or or)   { opBinaria(or, OR); }
    
    public static void generaCodigo(Not not) { opUnaria(not, NOT); }
    
    // Operadores aritméticos 

    public static void generaCodigo(Suma suma) { opBinaria(suma, (suma.tipo() instanceof Real)? SUMA_REAL: SUMA_ENT); }
    
    public static void generaCodigo(Resta resta) { opBinaria(resta, (resta.tipo() instanceof Real)? RESTA_REAL: RESTA_ENT); }
    
    public static void generaCodigo(Mul mul) { opBinaria(mul, (mul.tipo() instanceof Real)? MUL_REAL: MUL_ENT); }

    public static void generaCodigo(Div div) { opBinaria(div, (div.tipo() instanceof Real)? DIV_REAL: DIV_ENT); }
    
    public static void generaCodigo(Neg neg) { opUnaria(neg, (neg.tipo() instanceof Real)? NEG_REAL: NEG_ENT); }

    public static void generaCodigo(Mod mod) { opBinaria(mod, MOD); }

    // Operadores de acceso a memoria 

    public static void generaCodigo(Indx indx)
    {
        final Exp eid = indx.eid(), ei = indx.ei();

        generaCodigo(eid);
        generaCodigo(ei);

        if (esDesig(ei))
            maquina.lineaCodigo(new ApilaInd(maquina));

        maquina.lineaCodigo(new Apila(tamBase(eid), maquina));
        maquina.lineaCodigo(new OpBinaria<>(MUL_ENT, maquina));
        maquina.lineaCodigo(new OpBinaria<>(SUMA_ENT, maquina));
    }

    public static void generaCodigo(Acc acc)
    {
        final Exp reg = acc.reg();
        String id = acc.id();

        generaCodigo(reg);

        // Hallamos el desplazamiento del campo
        List<Campo> lcampo = ((Reg) refFact(reg.tipo())).lcampo();
        for (Campo c: lcampo)
        {
            if (id.equals(c.id()))
            {
                maquina.lineaCodigo(new Apila(c.desplazamiento(), maquina));
                maquina.lineaCodigo(new OpBinaria<>(SUMA_ENT, maquina));
                return;
            }
        }
    }

    public static void generaCodigo(Indir indir)
    {
        generaCodigo(indir.et());
        compruebaNulo(indir.sig() - 1);
        maquina.lineaCodigo(new ApilaInd(maquina));
    }
}