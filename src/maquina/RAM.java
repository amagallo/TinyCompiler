package maquina;

import java.util.Objects;

/**
    Un gestor de memoria que maneja un <b>array redimensionable</b>, también conocido como <i>vector</i>, a modo
    de simular la organización de una memoria de acceso aleatorio (RAM).
    
    <br>Esta estructura de datos sigue la implementación concurrente de la tabla dinámica de dos niveles descrita en
    el artículo <i>Lock-free Dynamically Resizable Arrays</i> (2006), de Damian Dechev, Peter Pirkelbauer y Bjarne
    Stroustrup, que se abrevia como <b>LDRA</b> en los comentarios del código. Esta tabla costa de un array de
    <b>compartimentos</b> (en inglés, <i>buckets</i>), regiones de almacenamiento relativamente pequeñas que son
    alojadas a demanda.
    
    <br>En el artículo, la elección de esta tabla como estructura subyacente al vector, en vez de un
    array unidimensional, se sustenta en la implementación del vector concurrente de Intel, pues de esta forma
    se evita una excesiva contención en las hebras que accedan a él, ya que así se optimiza en tiempo las
    redimensiones del array.

    <br>Sin embargo, esta memoria RAM no soporta concurrencia, y la elección de este tipo de tablas se ha realizado
    en base a optimizar en memoria esta estructura de datos, cuyo coste en espacio en el caso mejor es logarítimico.

    @implNote Esta implementación emplea algunos algoritmos de manejo de bits descritos en el libro <i>Hacker's
        Delight</i> (segunda edición, 2012), de Henry S. Warren, que se abrevia como <b>HD</b> en los comentarios del
        siguiente código.

    @version Java SE 17
    @see Memoria

    @author Félix Rodolfo Díaz Lorente
    @author Álvaro Magalló Paz
    @author Alejandro del Río Caballero
*/
class RAM implements Memoria
{
    /**
        Tabla de dos niveles que almacena los bloques alojados en esta memoria RAM.
    */
    private Object[][] memoria;

    /**
        Tamaño del primer bloque de memoria.
    */
    private final int primerBloque;

    /**
        Tamaño por defecto del primer bloque de memoria, tomado del que se empleo en los <i>benchmarks</i>
        de prueba que se describen en LDRA. 
    */
    private static final int BLOQUE_POR_DEFECTO = 8;

    /**
        Valor resultante de evaluar {@code Integer.numberOfLeadingZeros(primerBloque)}.
    */
    private final int nlzBloque;

    /**
        Tamaño máximo de la memoria RAM. Para requerimientos de almacenamiento superiores a este límite, se
        lanzará la excepción {@link StackOverflowError}.
    */
    private final int tamMax;

    /**
        Puntero a la siguiente posición al bloque con la dirección más alta. Corresponde al tamaño del vector si el
        almacenamiento del mismo no presenta huecos.
    */
    private int end = 0;
    
    /**
        Mensaje de las excepciones del tipo {@link StackOverflowError} que se lanzan en esta clase.
    */
    private static final String MENSAJE_STACK_OVERFLOW = "Memoria RAM agotada";

    /**
        Calcula la potencia de dos más cercana por arriba de <code>x</code>. Es decir, el resultado <i>r</i> es tal
        que 2<sup>x-1</sup> < r <= 2<sup>x</sup>.

        @param x el argumento de esta función
    
        @return Un entero <i>r</i> es tal que <code>2<sup>x-1</sup> < r <= 2<sup>x</sup></code>.
    */
    private static int clp2(int x)
    {
        // HD figura 3-3
        --x;
        x |= x >> 1;
        x |= x >> 2;
        x |= x >> 4;
        x |= x >> 8;
        x |= x >> 16;
        return x + 1;
    }

    /**
        Crea una memoria RAM cuya región de almacenamiento fijo (i. e.: una región que siempre se encuentra alojada
        en memoria) ocupa un mínimo número de bloques, alojados en las primeras posiciones, y que tiene un cierto
        tamaño máximo, que no se puede rebasar.

        @param tamFijo tamaño de la región de almacenamiento fijo
        @param tamMax tamaño máximo de la memoria RAM
    */
    public RAM(int tamFijo, int tamMax)
    {
        // Como el primer bloque debe tener un tamaño que sea potencia de dos, aproximamos a la siguiente potencia
        primerBloque = (tamFijo == 0)? 1: clp2(tamFijo);
        
        nlzBloque = Integer.numberOfLeadingZeros(primerBloque);

        this.tamMax = tamMax;

        memoria = new Object[Math.max(1, nlzBloque - Integer.numberOfLeadingZeros(tamMax) + 1)][];
        memoria[0] = new Object[primerBloque];
    }

    /**
        Crea una memoria RAM cuya región de almacenamiento fijo (i. e.: una región que siempre se encuentra alojada
        en memoria), de bloques almacenados en las primeras posiciones, tiene un tamaño dado por defecto,
        {@value #BLOQUE_POR_DEFECTO}, y que tiene un cierto tamaño máximo, que no se puede rebasar.

        @param tamMax tamaño máximo de la memoria RAM
    */
    public RAM(int tamMax) { this(BLOQUE_POR_DEFECTO, tamMax); }

    /**
        Verifica que la dirección dada no es demasido elevada como para que una escritura en dicha dirección no
        incurra en un desbordamiento de la memoria RAM (i. e.: que la escritura no haga que se supere el tamaño
        máximo impuesto en la memoria).

        @param dir la dirección a comprobar

        @throws StackOverflowError si la dirección haría que se desbordara la memoria RAM en caso de una escritura.
    */
    private void comprobarRango(int dir) throws StackOverflowError
    {
	if (dir >= tamMax)
            throw new StackOverflowError(MENSAJE_STACK_OVERFLOW);
    }
    
    /* LDRA algoritmos 3 y 6 */
    public Object leerBloque(int dir)
    {
        // Si la dirección no es válida, no leemos nada

        if (dir < 0 || dir >= end)
            return null;

        // Hallamos la dirección relativa, al compartimento correspondiente, del bloque que va a ser leído

        int pos = dir + primerBloque, nlzPos = Integer.numberOfLeadingZeros(pos);

        Object[] datos = memoria[nlzBloque - nlzPos]; // Hallamos el compartimento donde se encuentra alojado el bloque

        // Si está vacío, no leemos nada. Si no, leemos el bloque requerido

        return (datos == null)? null: datos[pos & (Integer.MAX_VALUE >>> nlzPos)];
    }

    public Object[] leer(int dir, int n)
    {
        Object[] lectura = new Object[n];

        if (dir < 0 || dir >= end) // Si la dirección dada no es válida, no hay que copiar nada
            return lectura;

        // Hallamos el índice del compartimento donde se encuentra el primer bloque a leer y su
        // dirección relativa en este

        int pos = dir + primerBloque, nlzPos = Integer.numberOfLeadingZeros(pos);
        int bloque = nlzBloque - nlzPos, offset = pos & (Integer.MAX_VALUE >>> nlzPos);

        // Copiamos al array de lectura el trozo del primer compartimento que debe ser leído

        Object[] datos = memoria[bloque++];

        int i; // Indice del array de lectura donde se va escribir el siguiente bloque leído

        // Tenemos el cuenta, con la función min, que solo leemos n bloques en total
        System.arraycopy(datos, offset, lectura, 0, i = Math.min(n, datos.length - offset));
        
        if (i != n) // Si no se han copiado todos los bloques requeridos
        {
            // Actualizamos los índices para que apunten al último bloque que debe ser leído

            pos += n;
            nlzPos = Integer.numberOfLeadingZeros(pos);

            // Escribimos en el array de lectura los compartimentos centrales, que se deben copiar completos

            for (int bl = nlzBloque - nlzPos; bloque < bl; i += datos.length)
            {
                datos = memoria[bloque++];
                System.arraycopy(datos, 0, lectura, i, datos.length);
            }

            // Se copian los bloques finales del último compartimento, si es necesario

            offset = pos & (Integer.MAX_VALUE >>> nlzPos);
            if (offset > 0)
            {
                datos = memoria[bloque];
                System.arraycopy(datos, 0, lectura, i, offset);
            }
        }

        return lectura;
    }

    /* LDRA algoritmos 1, 4 y 6 */
    public void escribirBloque(int dir, Object val) throws IndexOutOfBoundsException
    {
        // Comprobamos que no tratamos de escribir en direcciones no permitidas de esta memoria RAM

        comprobarRango(dir);

        // Si la dirección es negativa, lanzamos la excepción correspondiente
        
        Objects.checkIndex(dir, tamMax);

        // Calculamos el índice del compartimento donde hay que escribir el bloque

        int pos = dir + primerBloque, nlzPos = Integer.numberOfLeadingZeros(pos);
        int bloque = nlzBloque - nlzPos;

        // Alojamos el compartimento que debe ser escrito, si es necesario

        Object[] datos = memoria[bloque];
        if (datos == null)
            datos = memoria[bloque] = new Object[primerBloque << bloque];
        
        // Escribimos el bloque

        datos[pos & (Integer.MAX_VALUE >>> nlzPos)] = val; 

        // Actualizamos la dirección del final del vector, si es necesario

        end = Math.max(end, dir + 1);
    }

    public void escribir(int dir, Object[] val, int inicio) throws IndexOutOfBoundsException
    {
        final int n = val.length;

        // Comprobamos que no tratamos de escribir en direcciones no permitidas de esta memoria RAM

        comprobarRango(dir + n - 1);

        // Si la dirección es negativa, lanzamos la excepción correspondiente

        Objects.checkIndex(dir, tamMax);

        // Calculamos el índice del compartimento y la posición en el mismo del bloque donde escribir el dato

        int pos = dir + primerBloque, nlzPos = Integer.numberOfLeadingZeros(pos);
        int bloque = nlzBloque - nlzPos, offset = pos & (Integer.MAX_VALUE >>> nlzPos);

        // Alojamos el compartimento que debe ser escrito, si es necesario

        Object[] datos = memoria[bloque];
        if (datos == null)
            datos = memoria[bloque] = new Object[primerBloque << bloque];
        
        int i; // Indice del array de lectura donde se va escribir el siguiente bloque leído
        int m; // Número de bloques copiados en la primera parte de la escritura

        // Tenemos el cuenta, con la función min, que solo escribimos n bloques en total
        System.arraycopy(val, i = inicio, datos, offset, m = Math.min(n, datos.length - offset));
        
        if (m != n) // Si hay más compartimentos por escribir
        {
            // Actualizamos el índice del compartimento y la dirección relativa para esta segunda parte de la escritura

            i += m;
            ++bloque;

            // Actualizamos los índices para que apunten al último bloque que debe ser escrito

            pos += n;
            nlzPos = Integer.numberOfLeadingZeros(pos);
            
            // Rellenamos compartimentos completos, con los bloques de datos a escribir

            for (int bl = nlzBloque - nlzPos; bloque < bl; i += datos.length, ++bloque)
            {
                // Alojamos la memoria del compartimento actual, si es necesario

                datos = memoria[bloque];
                if (datos == null)
                    datos = memoria[bloque] = new Object[primerBloque << bloque];
                
                // Escribimos los bloques correspondientes al compartimento actual

                System.arraycopy(val, i, datos, 0, datos.length);
            }

            // Escribimos el último compartimento con los bloques finales, si es necesario

            offset = pos & (Integer.MAX_VALUE >>> nlzPos);
            if (offset > 0)
            {
                // Alojamos la memoria este compartimento, si es necesario

                datos = memoria[bloque];
                if (datos == null)
                    datos = memoria[bloque] = new Object[primerBloque << bloque];

                System.arraycopy(val, i, datos, 0, offset);
            }
        }

        // Actualizamos la dirección del final del vector, si es necesario

        end = Math.max(end, dir + n);
    }

    /**
        Reserva tanto espacio como sea necesario para que la memoria RAM pueda albergar, sin alojar memoria, un
        número de bloques dado, desde el comienzo de la memoria RAM. Si dicho número es menor o igual a la
        mayor dirección de un bloque alojado, este procedimiento no tiene ningún efecto.

        @param capacidad la nueva capacidad de la memoria RAM
    */
    public void reservar(int capacidad) /* LDRA algoritmo 5 */
    {
        // Comprobamos que no tratamos de alojar bloques en direcciones no permitidas de esta memoria RAM

        comprobarRango(capacidad - 1);

        // Si ya están alojados los suficientes compartimentos como para que la capacidad sea la requerida, no
        // hacemos nada

        if (end >= capacidad)
            return;
        
        // Calculamos el índice del compartimento donde se aloja el último bloque del vector, y su dirección
        // relativa

        int bloque = nlzBloque - Integer.numberOfLeadingZeros(end + primerBloque - 1);
        bloque &= ~(bloque >> 31); // if (bloque < 0) bloque = 0; [ver función doz(x) en HD sección 2-19]

        int tamBloque = memoria[bloque].length; // Hallamos el tamaño de este bloque

        // Calculamos el índice del compartimento donde se debe alojar el bloque que haga que el vector tenga la
        // capacidad requerida, y su dirección relativa

        final int bl = nlzBloque - Integer.numberOfLeadingZeros(capacidad + primerBloque - 1);
        while (bloque < bl)
        {
            tamBloque <<= 1; // El compartimento actual debe tener un tamaño el doble que el anterior
            memoria[++bloque] = new Object[tamBloque];
        }

        // Actualizamos la dirección del final del vector, para indicar el aumento de la capacidad del mismo

        end = capacidad;
    }

    /**
        Añade un bloque a continuación del que tiene la mayor dirección en esta memoria RAM.

        @param val el bloque a escribir 
    */
    public void pushBack(Object val) { escribirBloque(end, val); }

    /**
        Elimina, o al menos considera borrados, todos los bloques cuya dirección es mayor o igual a una dada.

        @param dir la dirección a partir de la cual todos los bloques alojados se consideran eliminados
    */
    public void borrar(int dir)
    {
        // Si la dirección de comienzo sobrepasa el final del vector, no eliminamos nada, y por lo tanto, salimos

        if (dir >= end)
            return;

        // Calculamos el índice del compartimento donde se aloja el primer bloque a eliminar, y su dirección relativa

        dir &= ~(dir >> 31); // if (bloque < 0) bloque = 0; [ver función doz(x) en HD sección 2-19]

        int pos = dir + primerBloque, nlzPos = nlzBloque - Integer.numberOfLeadingZeros(pos);
        int bloque = nlzBloque - nlzPos, offset = pos & (Integer.MAX_VALUE >>> nlzPos);

        // Si hay bloques que no se deben eliminar en el primer compartimento, no lo liberamos y pasamos al siguiente

        if (offset > 0)
            ++bloque;

        // Calculamos el índice del compartimento donde se aloja el último bloque a eliminar, y su dirección relativa

        int bl = nlzBloque - Integer.numberOfLeadingZeros(end + primerBloque - 1);
        bl &= ~(bl >> 31); // if (i < 0) i = 0; [ver función doz(x) en HD sección 2-19]

        // Eliminamos todos los bloques requeridos

        while (bloque < bl)
            memoria[bloque++] = null;

        // Actualizamos la dirección del final del vector, para indicar la disminución de la capacidad del mismo
        
        end = dir;
    }

    /**
        Elimina el bloque que tiene la mayor dirección en esta memoria RAM.
    */
    public void popBack() /* LDRA algoritmo 2 */
    {
        // Calculamos el índice del compartimento donde se aloja el último bloque, y su dirección relativa

        int pos = --end + primerBloque, nlzPos = Integer.numberOfLeadingZeros(pos);
        int bloque = nlzBloque - nlzPos;

        if (end > 0 && (end & (end - 1)) == 0) // Comprobamos si 'end' es potencia de dos (HD sección 2-1)
            memoria[bloque] = null; // Si el último compartimento se ha quedado vacío, lo liberamos
        else
            memoria[bloque][pos & (Integer.MAX_VALUE >>> nlzPos)] = null; // Si no, solo liberamos el bloque
    }
}
