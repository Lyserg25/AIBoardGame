import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Wayne on 14.04.2017.
 */
public class ClientStarter {
    private static final String HOST_NAME = null;
    private static final String[] TEAM_NAME = {"Lyserg25", "Grillhaehnsche", "MrKitty",};
    private static final String[] LOGO_PATH = {"src/resources/logo.png", "src/resources/patrick.png", "src/resources/mushroom.png"};

    /* start server:
     * cd "C:\Users\Wayne\IdeaProjects\BoardGame\server"
     * java -Djava.library.path=lib/native -jar bogapr.jar
     */
    public static void main(String[] args) {
        BufferedImage logo;
        List<MyClient> clients = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 3; i++) {
            logo = new BufferedImage(256, 256, BufferedImage.TYPE_4BYTE_ABGR);
            try {
                logo = ImageIO.read(new File(LOGO_PATH[i]));
            } catch (IOException e) {
                e.printStackTrace();
            }
            clients.add(new MyClient(HOST_NAME, TEAM_NAME[i], logo));
        }
        try {
            executor.invokeAll(clients);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
