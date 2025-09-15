package maquina;

import java.util.Objects;
import java.util.BitSet;

/**
    Implementación de una tabla FAT.

    @version Java SE 17
    @see GestorBloques

    @author Félix Rodolfo Díaz Lorente
    @author Álvaro Magalló Paz
    @author Alejandro del Río Caballero
*/
class FAT implements GestorBloques
{
    /** Bloque de la tabla FAT. */
    private static class Bloque
    {
        public Object valor;
        public int sig;

        public Bloque(Object valor, int sig) { this.valor = valor; this.sig = sig; }

        public Bloque() { this(null, NULL); }
    }

    private Bloque[] tabla; // Tabla FAT

    private BitSet mapa; // Mapa de bits
    private final int espacio; // Número de bits que contiene el mapa

    /**
        Crea una tabla FAT de un cierto tamaño.

        @param espacio tamaño de la tabla FAT
    */
    public FAT(int espacio)
    {
        this.espacio = espacio;

        // Rellenamos la tabla FAT con nodos vacíos
        tabla = new Bloque[espacio];
        for (int i = 0; i < espacio; ++i)
            tabla[i] = new Bloque();

        mapa = new BitSet(espacio);
    }

    public int alojar(int n)
    {
        // Tratamos el caso de valores de "n" no válidos y comprobamos si hay espacio suficiente
        
        if (n <= 0)
            return NULL;
        if (mapa.cardinality() + n > espacio)
            throw new OutOfMemoryError("No hay suficiente espacio de almacenamiento en este sistema.");

        // Alojamos el primer bloque

        final int inicio = mapa.nextClearBit(0); // Buscamos el primero libre empezando por el principio
        mapa.set(inicio);

        // Alojamos el resto
        
        int dir = inicio;
        for (int i = 1; i < n; ++i)
        {
            tabla[dir].sig = mapa.nextClearBit(dir); // Buscamos el siguiente bloque libre
            dir = tabla[dir].sig;

            mapa.set(dir);
        }

        tabla[dir].sig = NULL; // Indicamos que el último bloque alojado no tiene siguiente

        return inicio;
    }

    public Object leerBloque(int dir)
    {
        Objects.checkIndex(dir, tabla.length);
        return tabla[dir].valor;
    }

    public Object[] leer(int dir, int n)
    {
        Objects.checkIndex(dir, tabla.length);

        Object[] lectura = new Object[n];

        for (int i = 0; i < n && dir != NULL; ++i, dir = tabla[dir].sig)
            lectura[i] = tabla[dir].valor;
        
        return lectura;
    }

    public void escribirBloque(int dir, Object val)
    {
        Objects.checkIndex(dir, tabla.length);
        tabla[dir].valor = val;
    }

    public void escribir(int dir, Object[] val, int inicio)
    {
        Objects.checkIndex(dir, tabla.length);

        final int n = val.length;
        for (int i = inicio; i < n && dir != NULL; ++i, dir = tabla[dir].sig)
            tabla[dir].valor = val[i];
    }

    public void mover(int d0, int d1, int n)
    {
        Objects.checkIndex(d0, tabla.length);
        Objects.checkIndex(d1, tabla.length);

        for (int i = 0; i < n && d0 != NULL && d1 != NULL; ++i, d0 = tabla[d0].sig, d1 = tabla[d1].sig)
            tabla[d0].valor = tabla[d1].valor;
    }

    public void destruir(int dir, int n)
    {
        Objects.checkIndex(dir, tabla.length);

        for (int i = 0; i < n && dir != NULL; ++i, dir = tabla[dir].sig)
            mapa.clear(dir);
    }
}