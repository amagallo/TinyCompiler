package maquina;

class RAMTest
{
    public static void main(String[] args)
    {
        RAM ram = new RAM(1, 8);

        ram.reservar(100);
        ram.escribirBloque(99, "a");
        System.out.println(ram.leerBloque(99));
        ram.pushBack("b");
        System.out.println(ram.leerBloque(100));

        ram.escribirBloque(63, "c");
        ram.borrar(64);
        System.out.println(ram.leerBloque(63));

        long time = System.nanoTime();
        ram.escribirBloque(65, "f");
        ram.escribirBloque(35, "g");
        ram.escribirBloque(76, "c");
        System.out.println("Escritura RAM 1: " + (System.nanoTime() - time) / 1E6);

        time = System.nanoTime();
        System.out.println(ram.leerBloque(65));
        System.out.println(ram.leerBloque(35));
        System.out.println(ram.leerBloque(76));
        System.out.println("Lectura RAM 1: " + (System.nanoTime() - time) / 1E6);

        Object[] ram2 = new Object[128];

        time = System.nanoTime();
        ram2[65] = "a";
        ram2[35] = "b";
        ram2[76] = "c";
        System.out.println("Escritura RAM 2: " + (System.nanoTime() - time) / 1E6);

        time = System.nanoTime();
        System.out.println(ram2[65]);
        System.out.println(ram2[35]);
        System.out.println(ram2[76]);
        System.out.println("Lectura RAM 2: " + (System.nanoTime() - time) / 1E6);

        System.out.println();
        String[] str = { "d", "e", "f" };

        time = System.nanoTime();
        ram.escribir(32, str);
        System.out.println("Escritura RAM 1: " + (System.nanoTime() - time) / 1E6);

        Object[] lectura = new String[str.length];

        time = System.nanoTime();
        lectura = ram.leer(32, str.length);
        System.out.println("Lectura RAM 1: " + (System.nanoTime() - time) / 1E6);

        for (Object obj: lectura)
            System.out.println(obj);

        time = System.nanoTime();
        for (int i = 0; i < str.length; ++i)
            ram2[32 + i] = str[i];
        System.out.println("Escritura RAM 2: " + (System.nanoTime() - time) / 1E6);

        time = System.nanoTime();
        for (int i = 0; i < str.length; ++i)
            lectura[i] = ram2[32 + i];
        System.out.println("Lectura RAM 2: " + (System.nanoTime() - time) / 1E6);
        
        for (Object obj: lectura)
            System.out.println(obj);
    }
}