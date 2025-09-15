package maquina;

import static tiny.Ejecutable.MANEJADOR_EXCEPCION;

final class MemoriaTest
{
    private MemoriaTest() {}

    public static void main(String[] args)
    {
        Thread.setDefaultUncaughtExceptionHandler(MANEJADOR_EXCEPCION);

        GestorBloques g = new FAT(100);

        int a = g.alojar(1);
        int b = g.alojar(1);
        int c = g.alojar(1);
        int d = g.alojar(1);

        g.destruir(c, 1);

        c = g.alojar(1);

        g.destruir(b, 1);
        g.destruir(d, 1);
        g.destruir(a, 1);

        int e = g.alojar(10);

        g.destruir(c, 1);
        g.destruir(e, 10);

        int w = g.alojar(100);
        int z = g.alojar(1);

        g.destruir(z, 1);
        g.destruir(w, 100);
    }
}