package procesamientos;

import static asint.Sintaxis.*;

import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

import java.util.List;
import java.util.Map;
import java.util.Iterator;

/**
    Clase utilidad que contiene todos los métodos requeridos para ejecutar la fase de <b>tipado</b> en el
    procesamiento de nuestro lenguaje Tiny.
    
    @version Java SE 17
    @see Procesamiento

    @author Félix Rodolfo Díaz Lorente
    @author Álvaro Magalló Paz
    @author Alejandro del Río Caballero
*/
public class Tipado extends Procesamiento
{
    private static class EUnificacion extends Throwable
    {
        public EUnificacion(String mensaje) { super(mensaje); }
    }

    private static class ECompatibilidad extends EUnificacion
    {
        private static final String FORMATO_MENSAJE = "Tipos no compatibles: %s/%s";
        
        public ECompatibilidad(Tipo tipo1, Tipo tipo2)
        {
            super(FORMATO_MENSAJE.formatted(tipo1.litTipo(), tipo2.litTipo()));
        }
    }

    private static class ECampoDuplicado extends EUnificacion
    {
        public ECampoDuplicado(String mensaje) { super(mensaje); }
    }

    private static class EArrayNegativo extends EUnificacion
    {
        public EArrayNegativo(String mensaje) { super(mensaje); }
    }

    // Azúcar sintáctico para algunos errores de tipado
    private static final EUnificacion CAMPO_DUPLICADO = new ECampoDuplicado("Campo duplicado");
    private static final EUnificacion ARRAY_NEGATIVO  = new EArrayNegativo("Array de tamaño negativo");

    private static class ETipo extends EProcesamiento
    {
        public ETipo(Strloc strloc, EUnificacion causa) { super(FASE.TIPADO, strloc, causa); }

        public ETipo(Strloc strloc) { super(FASE.TIPADO, strloc); }
    }

    private Tipado() {} // No se permite instanciar esta clase

    public static void tipo(INodo nodo) { procesa(Tipado.class, "tipo", nodo); }

    public static void tipo(List<? extends INodo> lista) { procesa(Tipado.class, "tipo", lista); }

    // Métodos auxiliares

    private static void nodoError(INodo nodo)
    {
        nodo.tipado(ERROR);
        throw new ETipo(nodo.strloc());
    }

    private static void nodoError(INodo nodo, EUnificacion causa)
    {
        nodo.tipado(ERROR);
        throw new ETipo(nodo.strloc(), causa);
    }

    private static final List<Class<? extends Tipo>> NUM = List.of(Int.class, Real.class);
    private static boolean esNum(Tipo tipo) { return NUM.contains(tipo.getClass()); }

    private static final Class<? extends Tipo> CLASE_NULL = NULL.getClass();
    private static boolean esNulo(Tipo tipo) { return tipo.getClass().equals(CLASE_NULL); }

    private static final List<Class<? extends Tipo>> REF = List.of(Puntero.class, CLASE_NULL);
    private static boolean esRef(INodo nodo) { return REF.contains(nodo.getClass()); }

    private static final List<Class<? extends Tipo>> LEGIBLE = List.of(Int.class, Real.class, Str.class);
    private static boolean esLegible(INodo nodo) { return LEGIBLE.contains(nodo.getClass()); }

    private static final List<Class<? extends Tipo>> IMPR = List.of(Int.class, Real.class, Bool.class, Str.class);
    private static boolean esImprimible(INodo nodo) { return IMPR.contains(nodo.getClass()); }

    private static record Par<T>(T prim, T seg)
    {
        @Override
        public boolean equals(Object obj)
        {
            return (obj instanceof Par par)? prim.equals(par.prim()) && seg.equals(par.seg())
                || prim.equals(par.seg()) && seg.equals(par.prim()): false;
        }
    }

    private static boolean tiposCompatibles(Tipo t1, Tipo t2)
    {
        Map<Par<String>, Boolean> tc = new HashMap<>();

        Par<String> parRef = null;
        Boolean res = null;

        while (res == null)
        {
            if (t1 instanceof Ref && t2 instanceof Ref)
            {
                String id1, id2;
                do
                {
                    id1 = t1.id(); id2 = t2.id();
                    if (id1.equals(id2))
                        return true;

                    t1 = t1.vinculo().tipo(); t2 = t2.vinculo().tipo();
                } while (t1 instanceof Ref && t2 instanceof Ref);

                parRef = new Par<>(id1, id2);
                res = tc.get(parRef);
                if (res != null)
                    return res;
            }

            t1 = refFact(t1); t2 = refFact(t2);
            
            if (esNum(t1) && esNum(t2))
                res = true;
            else if (t1 instanceof Bool && t2 instanceof Bool)
                res = true;
            else if (t1 instanceof Str && t2 instanceof Str)
                res = true;
            else if (t1 instanceof Array a1 && t2 instanceof Array a2)
            {
                t1 = a1.tipo(); t2 = a2.tipo();
            }
            else if (t1 instanceof Puntero && esNulo(t2))
                res = true;
            else if (t1 instanceof Puntero p1 && t2 instanceof Puntero p2)
            {
                t1 = p1.tipo(); t2 = p2.tipo();
            }
            else if (t1 instanceof Reg reg1 && t2 instanceof Reg reg2)
            {
                List<Campo> lc1 = reg1.lcampo(), lc2 = reg2.lcampo();
                if (lc1.size() == lc2.size())
                {
                    // Comprueba campos
                    Iterator<Campo> it1 = lc1.iterator(), it2 = lc2.iterator();
                    while (it1.hasNext() && it2.hasNext())
                    {
                        Campo c1 = it1.next(), c2 = it2.next();
                        if (!tiposCompatibles(c1.tipo(), c2.tipo()))
                            return false;
                    }
                    return false;
                }
                else
                    res = false;
            }
            else
                res = false;
        }

        tc.put(parRef, res);
        return res;
    }
    
    // Programa

    public static void tipo(Prog prog)
    {
        final int prevError = error();

        tipo(prog.ldec());
        tipo(prog.lins());

        prog.tipado((error() == prevError)? OK: ERROR);
    }
    
    // Declaraciones básicas
    
    public static void tipo(Proc proc)
    {
        final int prevError = error();

        tipo(proc.lparam());
        tipo(proc.ldec());
        tipo(proc.lins());

        proc.tipado((error() == prevError)? OK: ERROR);
    }

    // Tipos renombrados

    public static void tipo(Ref ref)
    {
        if (!(ref.vinculo() instanceof Type type))
            nodoError(ref);
    }

    // Tipos compuestos
    
    public static void tipo(Array array)
    {
        if (array.tam().charAt(0) == '-') // El tamaño es negativo
            nodoError(array, ARRAY_NEGATIVO);
    }

    // Campos de datos

    public static void tipo(Reg reg)
    {
        Set<String> tc = new HashSet<>();

        for (Campo c: reg.lcampo())
        {
            if (tc.contains(c.id()))
                nodoError(c, CAMPO_DUPLICADO);

            Tipo t = c.tipo();
            tipo(t);
            if (t.tipo() == ERROR)
                reg.tipado(ERROR);
            
            tc.add(c.id());
        }
    }

    // Instrucciones básicas

    public static void tipo(Asig asig)
    {
        Exp ei = asig.ei(), ed = asig.ed();
        
        tipo(ei);
        tipo(ed);

        if (ei.tipo() == ERROR || ed.tipo() == ERROR)
            asig.tipado(ERROR);
        else if (!tiposCompatibles(ei.tipo(), ed.tipo()))
            nodoError(asig, new ECompatibilidad(ei.tipo(), ed.tipo()));
        else if (!esDesig(ei))
            nodoError(asig);
        else
            asig.tipado(OK);
    }

    public static void tipo(Invoc invoc)
    {
        INodo id = invoc.eid();
        if (id instanceof Ident)
        {
            INodo vinculo = id.vinculo();
            if (vinculo instanceof Proc proc)
            {
                final List<? extends Param> lparam = proc.lparam();
                final List<? extends Exp> lexp = invoc.lexp();

                tipo(lexp);

                if (lparam.size() != lexp.size())
                    nodoError(invoc);

                Iterator<? extends Param> itf = lparam.iterator();
                Iterator<? extends Exp> itr = lexp.iterator();
                while (itf.hasNext() && itr.hasNext())
                {
                    Param pf = itf.next();
                    Exp pr = itr.next();

                    tipo(pf);
                    tipo(pr);

                    if (pf instanceof Pvar && !esDesig(pr))
                        nodoError(invoc);
                    else if (!tiposCompatibles(pf.tipo(), pr.tipo()))
                        nodoError(invoc, new ECompatibilidad(pf.tipo(), pr.tipo()));
                }

                invoc.tipado(OK);
            }
        }
        else
            nodoError(invoc);
    }

    // Instrucciones de bloques

    public static void tipo(Seq seq)
    {
        final int prevError = error();

        tipo(seq.ldec());
        tipo(seq.lins());

        seq.tipado((error() == prevError)? OK: ERROR);
    }

    public static void tipo(IfThen ifThen)
    {
        INodo exp = ifThen.exp();
        tipo(exp);

        if (exp.tipo() == ERROR)
            ifThen.tipado(ERROR);
        else if (refFact(exp.tipo()) instanceof Bool)
        {
            final int prevError = error();

            tipo(ifThen.lins());
            ifThen.tipado((error() == prevError)? OK: ERROR);
        }
        else
            nodoError(ifThen);
    }

    public static void tipo(IfThenElse ifThenElse)
    {
        INodo exp = ifThenElse.exp();
        tipo(exp);

        if (exp.tipo() == ERROR)
            ifThenElse.tipado(ERROR);
        else if (refFact(exp.tipo()) instanceof Bool)
        {
            final int prevError = error();

            tipo(ifThenElse.lins());
            tipo(ifThenElse.lins2());

            ifThenElse.tipado((error() == prevError)? OK: ERROR);
        }
        else
            nodoError(ifThenElse);
    }

    public static void tipo(While wh)
    {
        INodo exp = wh.exp();
        tipo(exp);

        if (exp.tipo() == ERROR)
            wh.tipado(ERROR);
        else if (refFact(exp.tipo()) instanceof Bool)
        {
            final int prevError = error();

            tipo(wh.lins());
            wh.tipado((error() == prevError)? OK: ERROR);
        }
        else
            nodoError(wh);
    }
    
    // Gestión de la E/S estándar
    
    public static void tipo(Read read)
    {
        INodo exp = read.exp();
        if (!esDesig(exp))
            nodoError(read);

        tipo(exp);

        if (exp.tipo() == ERROR)
            read.tipado(ERROR);
        else if (esLegible(refFact(exp.tipo())))
            read.tipado(OK);
        else
            nodoError(read);
    }

    public static void tipo(Write write)
    {
        INodo exp = write.exp();
        tipo(exp);

        if (exp.tipo() == ERROR)
            write.tipado(ERROR);
        else if (esImprimible(refFact(exp.tipo())))
            write.tipado(OK);
        else
            nodoError(write);
    }

    public static void tipo(NewLine nl) { nl.tipado(OK); }

    // Gestión de la memoria dinámica

    public static void tipo(InsMemoria insMem)
    {
        INodo exp = insMem.exp();
        tipo(exp);

        if (exp.tipo() == ERROR)
            insMem.tipado(ERROR);
        else if (refFact(exp.tipo()) instanceof Puntero)
            insMem.tipado(OK);
        else
            nodoError(insMem);
    }

    // Expresiones básicas

    public static void tipo(Entero exp)  { exp.tipado(INT); }
    public static void tipo(Decimal exp) { exp.tipado(REAL); }
    public static void tipo(Cadena exp)  { exp.tipado(STRING); }

    public static void tipo(Ident exp)
    {
        INodo vinculo = exp.vinculo();
        if (vinculo instanceof Var || vinculo instanceof Param)
            exp.tipado(vinculo.tipo());
        else
            nodoError(exp);
    }

    // Expresiones constantes

    public static void tipo(True t)  { t.tipado(BOOL); }
    public static void tipo(False f) { f.tipado(BOOL); }
    public static void tipo(Nulo n)  { n.tipado(NULL); }

    // Operadores de igualdad

    public static void tipo(ExpRel exp)
    {   
        Exp op1 = exp.op1(), op2 = exp.op2();
        tipo(op1);
        tipo(op2);

        Tipo t1 = refFact(op1.tipo()), t2 = refFact(op2.tipo());
        if (t1.tipo() == ERROR || t2.tipo() == ERROR)
            exp.tipado(ERROR);
        else if (esNum(t1) && esNum(t2) || t1 instanceof Bool && t2 instanceof Bool
            || t1 instanceof Str && t2 instanceof Str
            || exp instanceof ExpIgualdad && esRef(t1) && esRef(t2))
            exp.tipado(BOOL);
        else
            nodoError(exp);
    }

    // Operadores aritméticos

    public static void tipo(ExpAritReal exp)
    {
        Exp op1 = exp.op1(), op2 = exp.op2();
        tipo(op1);
        tipo(op2);

        Tipo t1 = refFact(op1.tipo()), t2 = refFact(op2.tipo());
        if (t1.tipo() == ERROR || t2.tipo() == ERROR)
            exp.tipado(ERROR);
        else if (t1 instanceof Int && t2 instanceof Int)
            exp.tipado(INT);
        else if (esNum(t1) && esNum(t2))
            exp.tipado(REAL);
        else
            nodoError(exp);
    }

    public static void tipo(Neg exp)
    {
        Exp op = exp.op();
        tipo(op);

        Tipo t = refFact(op.tipo());
        if (t.tipo() == ERROR)
            exp.tipado(ERROR);
        else if (t instanceof Int)
            exp.tipado(INT);
        else if (esNum(t))
            exp.tipado(REAL);
        else
            nodoError(exp);
    }

    // Operador de aritmética discreta

    public static void tipo(ExpDiscreta exp)
    {
        final boolean esEntera = exp instanceof ExpAritEnt;
        final Class<? extends Tipo> clase = esEntera? Int.class: Bool.class;

        Exp op1 = exp.op1(), op2 = exp.op2();
        tipo(op1);
        tipo(op2);

        Tipo t1 = refFact(op1.tipo()), t2 = refFact(op2.tipo());
        if (t1.tipo() == ERROR || t2.tipo() == ERROR)
            exp.tipado(ERROR);
        else if (clase.equals(t1.getClass()) && clase.equals(t2.getClass()))
            exp.tipado(esEntera? INT: BOOL);
        else
            nodoError(exp);
    }

    public static void tipo(Not exp)
    {
        Exp op = exp.op();
        tipo(op);

        Tipo t = refFact(op.tipo());
        if (t.tipo() == ERROR)
            exp.tipado(ERROR);
        else if (t instanceof Bool)
            exp.tipado(BOOL);
        else 
            nodoError(exp);
    }

    // Operadores posfijos de acceso a memoria
    
    public static void tipo(Indx indx)
    {
        Exp eid = indx.eid(), ei = indx.ei();
        tipo(eid);
        tipo(ei);

        Tipo tid = refFact(eid.tipo()), ti = refFact(ei.tipo());
        if (tid.tipo() == ERROR || ti.tipo() == ERROR)
            indx.tipado(ERROR);
        else if (tid instanceof Array array && ti instanceof Int)
            indx.tipado(array.tipo());
        else
            nodoError(indx);
    }

    public static void tipo(Acc acc)
    {
        Exp er = acc.reg();
        tipo(er);

        Tipo t = refFact(er.tipo());
        if (acc.tipo() == ERROR || er.tipo() == ERROR)
            acc.tipado(ERROR);
        else if (t instanceof Reg reg)
        {
            // Comprueba campos
            final String cid = acc.id();

            List<Campo> campos = reg.lcampo();
            for (Campo c: campos)
            {
                if (cid.equals(c.id()))
                {
                    acc.tipado(c.tipo());
                    return;
                }
            }
        }

        nodoError(acc);
    }

    public static void tipo(Indir indir)
    {
        Exp er = indir.et();
        tipo(er);

        Tipo t = refFact(er.tipo());
        if (indir.tipo() == ERROR || er.tipo() == ERROR)
            indir.tipado(ERROR);
        else if (t instanceof Puntero punt)
            indir.tipado(punt.tipo());
        else
            nodoError(indir);
    }
}
