package maquina;

import asint.Sintaxis.Nodo;

interface Memoria
{
    /**
        Entero que representa una dirección física nula.
    */
    int NULL = Nodo.VALOR_NULO;

    /**
        Devuelve un bloque de datos dada la dirección donde se encuentra alojado.

        @param dir la dirección del bloque que va a ser leído

        @return El bloque de datos alojado en la dirección <code>dir</code>.

        @throws IndexOutOfBoundsException si la dirección proporcionada no es válida.
    */
    default Object leerBloque(int dir) throws IndexOutOfBoundsException
    {
        return leer(dir, 1)[0];
    }

    /**
        Devuelve un número dado de bloques de datos consecutivos, o tantos como sean accesibles desde el primero
        de ellos, según la visión lógica de este sistema de bloques, proporcionando una dirrección inicial.
        Por eficiencia, <b>no</b> es obligatorio <b>comprobar</b> dicha condición de accesibilidad en el propio
        método, por lo que es responsabilidad del programador verificar que los bloques consecutivos referidos
        son alcanzables desde el inicial.

        @param dir la dirección inicial
        @param n el número de bloques consecutivos, alojados a partir de la dirección <code>dir</code>, que van
            a ser leídos

        @return Un array que solo contiene, el orden creciente de direcciones, los <code>n</code> bloques
            consecutivos almacenados en el sistema comenzando en la dirección <code>dir</code>.

        @throws IndexOutOfBoundsException si la dirección <code>dir</code> no es válida.
    */
    Object[] leer(int dir, int n) throws IndexOutOfBoundsException;

    /**
        Escribe un bloque de datos en una dirección dada.

        @param dir la dirección del bloque que va a ser escrito
        @param val el bloque de datos

        @throws IndexOutOfBoundsException si la dirección proporcionada no es válida.
    */
    default void escribirBloque(int dir, Object val) throws IndexOutOfBoundsException
    {
        Object[] bloque = { val };
        escribir(dir, bloque);
    }

    /**
        Escribe múltiples bloques de datos de manera consecutiva según la visión lógica de este sistema de
        bloques, o tantos como sean accesibles desde la dirección de comienzo dada. Dichos bloques se encuentran
        almacenados a partir de un índice de comienzo en un array dado. Por eficiencia, <b>no</b> es obligatorio
        <b>comprobar</b> dicha condición de accesibilidad en el propio método, por lo que es responsabilidad del
        programador verificar que los bloques consecutivos referidos son alcanzables desde el inicial.

        @param dir la dirección de comienzo
        @param val el array que contiene los bloques que va a ser escritos de forma consecutiva
        @param inicio el índice de comienzo a partir del cuál los bloques contenidos en el array van a ser
            escritos en este sistema de memoria

        @throws IndexOutOfBoundsException si la dirección <code>dir</code> no es válida.
    */
    void escribir(int dir, Object[] val, int inicio) throws IndexOutOfBoundsException;

    /**
        Escribe múltiples bloques de datos de manera consecutiva según la visión lógica de este sistema de
        bloques, o tantos como sean accesibles desde la dirección de comienzo dada. Por eficiencia, <b>no</b> es
        obligatorio <b>comprobar</b> dicha condición de accesibilidad en el propio método, por lo que es
        responsabilidad del programador verificar que los bloques consecutivos referidos son alcanzables desde
        el inicial.

        @param dir la dirección de comienzo
        @param val el array que contiene los bloques que va a ser escritos de forma consecutiva

        @throws IndexOutOfBoundsException si la dirección <code>dir</code> no es válida.

        @apiNote Una llamada a este método es equivalente al siguiente código: {@code escribir(dir, val, 0); }
    */
    default void escribir(int dir, Object[] val) throws IndexOutOfBoundsException
    {
        escribir(dir, val, 0);
    }

    /**
        Traslada un número de bloques consecutivos, según la visión lógica de este sistema de bloques, de una
        dirección de comienzo a otra. Dichos bloques deben ser accesibles desde sendas direcciones de inicio;
        sin embargo, por eficiencia, <b>no</b> es obligatorio <b>comprobar</b> dicha condición de accesibilidad
        en el propio método, por lo que es responsabilidad del programador verificar que todos los bloques
        consecutivos referidos son alcanzables desde las respectivas direcciones de comienzo.
        
        Para ilustrar el funcionamiento de este método, se ofrece una implementación por defecto.

        @param dir0 dirección destino a la cual se van a mover los bloques
        @param dir1 dirección origen, la dirección de comienzo de los bloques consecutivos
        @param n número de bloques consecutivos que se van a desplazar
    
        @throws IndexOutOfBoundsException si la dirección <code>dir0</code> y/o <code>dir1</code> no son válidas.
    */
    default void mover(int dir0, int dir1, int n) throws IndexOutOfBoundsException
    {
        Object[] lectura = leer(dir1, n);
        escribir(dir0, lectura);
    }
}