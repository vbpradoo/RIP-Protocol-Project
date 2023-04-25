import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class Packet {
    private ByteBuffer content;
    private int index=1;
    static private byte [] password;
    Packet(Tipo t, int numEntradas){
        content = ByteBuffer.allocate(4+20*(numEntradas+1));
       /*-----HEADER----*/
        content.put( (byte) t.v);                      //Command
        content.put( (byte) 2);                        //Version de RIP
        content.put( (byte) 0);                        //Relleno
        content.put( (byte) 0);                        //Relleno
        /*-----AUTH-----*/
        content.put((byte)255);                        //Address Family
        content.put((byte)255);
        content.put((byte)0);
        content.put((byte)2);                         //Authentication type
        content.put(password);                        //Authentication





    }

    Packet(byte[] mensaje){
        if((mensaje.length-4)%20!=0) return; //TODO como transmitirselo a la clase que llama? !=null?
        this.content =ByteBuffer.wrap(mensaje);
    }
    Packet(Packet clone){


        byte[] array = clone.content.array();
        ByteBuffer.wrap(clone.content.array());
        this.index = clone.index;
    }

    public boolean isPassValid(){
        byte [] passToValidate=new byte[16];
        for (int i = 8; i < 24; i++) {
            passToValidate[i-8]=content.get(i);
        }
        return Arrays.equals(passToValidate, password);
    }
    public static void genPassword(String pass){
        while(pass.length()<16) pass+="0";
        if(pass.length()>16) pass=pass.substring(0,16);
        password= pass.getBytes(); //Aquí un array de 16 byts
    }


    ArrayList<Entry> getEntrys(){
        ArrayList<Entry> entrys = new ArrayList<>();
        for (int j = 1; j < (content.limit()-4)/20; j++) {
            if(content.get(j * 20 + 8)==(byte)0) return entrys; //Cuando no haya IPv4 significa que ya no hay más entradas
            entrys.add(new Entry(
                    new byte[]{content.get(j * 20 + 8), content.get(j * 20 + 9), content.get(j * 20 + 10), content.get(j * 20 + 11)}, //Añade la IPv4
                    new byte[]{content.get(j * 20 + 12), content.get(j * 20 + 13), content.get(j * 20 + 14), content.get(j * 20 + 15)}, //Añade la mascara
                    new byte[]{content.get(j * 20 + 16),content.get(j * 20 + 17),content.get(j * 20 + 18),content.get(j * 20 + 19)}, //Añade el Next Hop
                    content.get(j * 20 + 23)));  //Añade la metrica)
        }
        return entrys;
    }

    void setMetric(int index, int metric){
        content.put(20 * (index+1) + 23, (byte) metric);
    }
    void setCommand(Tipo t){
        content.put(0, (byte) t.v);
    }
    void addEntry(Entry e){

        byte[] mask = e.getMask();
        byte[] IPv4 = e.getIPv4();
        byte[] nextHop = e.getNextHopBytes();
        byte metric = e.getMetric();

        /*--Entry table--*/
        content.put(4 + index * 20, (byte) 0);        //Address family
        content.put(5 + index * 20, (byte) 2);        //TODO http://tools.ietf.org/html/rfc1058 dice que 2 (¿?)
        content.put(6 + index * 20, (byte) 0);        //Route tag
        content.put(7 + index * 20, (byte) 0);
        content.put(8 + index * 20, IPv4[0]);        //IPv4 address
        content.put(9 + index * 20, IPv4[1]);
        content.put(10 + index * 20, IPv4[2]);
        content.put(11 + index * 20, IPv4[3]);
        content.put(12 + index * 20, mask[0]);       //Subnet mask
        content.put(13 + index * 20, mask[1]);
        content.put(14 + index * 20, mask[2]);
        content.put(15 + index * 20, mask[3]);
        content.put(16 + index * 20, nextHop[0]);   //Next hop
        content.put(17 + index * 20, nextHop[1]);
        content.put(18 + index * 20, nextHop[2]);
        content.put(19 + index * 20, nextHop[3]);
        content.put(20 + index * 20, (byte) 0);       //Metric
        content.put(21 + index * 20, (byte) 0);
        content.put(22 + index * 20, (byte) 0);
        content.put(23 + index * 20, metric);
        index++;

    }
    DatagramPacket getDatagramPacket(InetAddress addrDestino, int puertoDestino){
        /*if(index>24){
            ArrayList<Entry> Entries = this.getEntrys();
            int numEntradas = Entries.size();
            int numBucles = (int) Math.floor(numEntradas / 24);

            //Añade paquetes de 24 entradas hasta que sea necesario
            ArrayList<Packet> Packages = new ArrayList<>();
            for (int i = 0; i < numBucles ; i++) {
                Packet p = new Packet(Tipo.RESPONSE,24);
                for (int j = 0; j < 24 ; j++) {
                    p.addEntry(Entries.get(j+i*24));
                    Packages.add(p);
                }
            }
            int paquetesRestantes = numEntradas -24*numBucles;
            //Si no es multiplo de 24, añade las entradas restantes al último paquete
            if(paquetesRestantes!=0) {
                Packet p = null;
                for (int i = 0; i < paquetesRestantes; i++) {
                    p = new Packet(Tipo.RESPONSE, paquetesRestantes);
                    p.addEntry(Entries.get(i+24*numBucles));
                }
                Packages.add(p);
            }
            return Packages;
            
            
            for (int i = 0; i < 24 ; i++) {
                Packages.add(new Packet(Tipo.RESPONSE,)
            }
        }
        else{
            
        }
        */
        


        //Split Horizon with Poison Reverse
        Packet p = new Packet(Tipo.RESPONSE,getEntrys().size());
        for(Entry e: this.getEntrys()){
            if(e.getNextHop().equals(addrDestino))
                e.setMetric(16);
            p.addEntry(e);

        }
       return new DatagramPacket(p.content.array(),p.content.limit(), addrDestino, puertoDestino);

    }



}
