package maquina;

import static maquina.MaquinaP.*;
import static tiny.Ejecutable.MANEJADOR_EXCEPCION;

final class MaquinaTest
{
    public static final int numRegistros = 5, tamHeap = 10, tamActivacion = 10, numDisplays = 2, numIns = 16;
    
    public static void main(String[] args)
    {
        Thread.setDefaultUncaughtExceptionHandler(MANEJADOR_EXCEPCION);

        MaquinaVirtual maquina = new MaquinaP(numRegistros, tamHeap, tamActivacion, numDisplays, numIns);

        maquina.lineaCodigo(new Activa(1, 1, 8, maquina));
        maquina.lineaCodigo(new Dup(maquina));
        maquina.lineaCodigo(new Apila(0, maquina));
        maquina.lineaCodigo(new OpBinaria<>(SUMA_ENT, maquina));
        maquina.lineaCodigo(new Apila(5, maquina));
        maquina.lineaCodigo(new DesapilaInd(maquina));
        maquina.lineaCodigo(new Desapilad(1, maquina));
        maquina.lineaCodigo(new Ira(9, maquina));
        maquina.lineaCodigo(new Stop(maquina));
        maquina.lineaCodigo(new Apila(0, maquina));
        maquina.lineaCodigo(new Apilad(1, maquina));
        maquina.lineaCodigo(new Apila(0, maquina));
        maquina.lineaCodigo(new OpBinaria<>(SUMA_ENT, maquina));
        maquina.lineaCodigo(new Mueve(1, maquina));
        maquina.lineaCodigo(new Desactiva(1, 1, maquina));
        maquina.lineaCodigo(new Irind(maquina));

        maquina.ejecuta();
    }
}