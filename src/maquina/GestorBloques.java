package maquina;

/**
    Interfaz común a todos los sistemas de bloques, o sistemas de almacenamiento similares, con las operaciones
    básicas para su gestión. Utilizados, por ejemplo, en manejadores de memoria dinámica y en los sistemas de
    ficheros de muchos sistemas operativos, estos gestores implementan un esquema de traducción entre la visión
    lógica del sistema, que es el espacio de direcciones que maneja el usuario para acceder al dispositivo de
    memoria desde su aplicación de más alto nivel, y la visión física, que establece cómo se distribuyen dichos
    bloques de datos en el dispositivo de memoria donde se almacenan (p. ej.: una partición del disco duro).

    @version Java SE 17

    @author Félix Rodolfo Díaz Lorente
    @author Álvaro Magalló Paz
    @author Alejandro del Río Caballero
*/
interface GestorBloques extends Memoria
{
    /**
        Reserva un número dado de bloques consecutivos según la visión lógica de este sistema de bloques.
        Devuelve {@value #NULL} si dicho número no es un entero positivo.

        @param n número de bloques que van a ser alojados en el sistema
    
        @return La dirección de este sistema de almacenamiento al primer bloque reservado, fruto de realizar
            esta operación.
        
        @throws OutOfMemoryError si el número de bloques que van a ser alojados excede la capacidad del sistema
            de bloques.
    */
    int alojar(int n) throws OutOfMemoryError;

    /**
        Libera un número dado de bloques consecutivos en este sistema de almacenamiento, o tantos como sean
        alcanzables desde el primero de ellos, según la visión lógica de este sistema, proporcionando una
        dirección inicial. Por eficiencia, <b>no</b> es obligatorio <b>comprobar</b> dicha condición de
        accesibilidad en el propio método, por lo que es responsabilidad del programador verificar que los
        bloques consecutivos referidos son alcanzables desde el inicial.

        @param dir la dirección del primer bloque
        @param n el número de bloques consecutivos, alojados a partir de la dirección <code>dir</code>, que van
            a ser liberados

        @throws IndexOutOfBoundsException si la dirección <code>dir</code> no es válida.
    */
    void destruir(int dir, int n) throws IndexOutOfBoundsException;
}