import de.labystudio.mcwsproxy.Server;

/**
 * Main class that starts the web socket server
 *
 * @author LabyStudio
 */
public class Start {

    public static void main(String[] args) throws Exception {
        new Server().start();
    }

}
