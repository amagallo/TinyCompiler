package procesamientos;

import asint.Sintaxis.INodo;

import java.util.Map;
import java.util.HashMap;

import java.util.Deque;
import java.util.ArrayDeque;

/**
    Tabla de símbolos que sirve para agilizar la fase de vinculación en el procesamiento de nuestro lenguaje
    Tiny.

    @version Java SE 17
    @see Vinculacion

    @author Félix Rodolfo Díaz Lorente
    @author Álvaro Magalló Paz
    @author Alejandro del Río Caballero
*/
class TablaSimbolos
{
    private Deque<Map<String, INodo>> ambitos; // Pila de ámbitos

    /**
        Construye una nueva tabla de símbolos con un único ámbito, o nivel de anidamiento.
    */
    public TablaSimbolos()
    {
        ambitos = new ArrayDeque<Map<String, INodo>>();
        abreNivel();
    }

    /**
        Crea un nuevo nivel de anidamiento, realizando así todas las acciones necesarias para cambiar de ámbito.
    */
    public void abreNivel() { ambitos.addFirst(new HashMap<String, INodo>()); }

    /**
        Inserta un nodo, direccionado por un identificador, en la tabla de símbolos asociada al ámbito, o nivel
        de anidamiento, actual.

        @apiNote Se permite, aunque no se recomienda, que el nodo sea nulo.

        @param id el identificador que indexa el nodo a introducir en la tabla de símbolos
        @param nodo el nodo que debe ser añadido a la tabla de símbolos
    */
    public void inserta(String id, INodo nodo) { ambitos.getFirst().put(id, nodo); }

    /**
        Halla el nodo del nivel más cercano asociado a un identificador en la tabla de símbolos.

        @apiNote Si el nodo asociado al identificador es nulo, si lo hubiera, entonces el mejor coste temporal
            de este método es el peor posible: O(<i>n</i>) amortizado, donde <i>n</i> es el número de niveles de
            anidamiento que presenta la tabla de símbolos. Por ello, no se recomienda que los nodos de la tabla
            sean nulos, ya que en caso contrario, el coste mejor temporal es O(1) amortizado.

        @param id el identificador asociado al nodo que se debe devolver

        @return El nodo indexado por <code>id</code> en la tabla de símbolos.
    */
    public INodo valorDe(String id)
    {
        for (Map<String, INodo> tabla: ambitos)
        {
            INodo valor = tabla.get(id);
            if (valor != null)
                return valor;
        }
        return null;
    }

    /**
        Indica si existe un cierto identificador en la tabla de símbolos.

        @param id el identificador a buscar en la tabla de símbolos

        @return <code>true</code> si solo si la tabla de símbolos contiene un nodo asociado a <code>id</code>.
    */
    public boolean contiene(String id) { return ambitos.getFirst().containsKey(id); }

    /**
        Elimina el nivel actual de anidamiento, restaurando así el anterior e indicando que se regresa al
        ámbito inmediatamente anterior.
    */
    public void cierraNivel()
    {
        if (ambitos.size() > 1)
            ambitos.removeFirst();
        else // No eliminamos el último nivel restante, simplemente vaciamos el diccionario asociado
            ambitos.getFirst().clear();
    }
}