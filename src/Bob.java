
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import net.sharkfw.kep.SharkProtocolNotSupportedException;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.system.L;
import net.sharkfw.system.SharkException;

/**
 *
 * @author thsc
 */
public class Bob extends NewsPeerTCP {

    public Bob(String peerName, String peerSI, String address, int port) throws SharkProtocolNotSupportedException, IOException {
        super(peerName, peerSI, address, port);
    }

    public static void main(String[] args) throws SharkProtocolNotSupportedException, IOException, SharkException, InterruptedException {
        // settup alice peer
//        L.setLogLevel(L.LOGLEVEL_ALL);

        System.out.println("Alice must run first");

        Bob bob = new Bob("Bob",
                "http://www.sharksystem.net/bob.html",
                "tcp://localhost:7071",
                7071
        );
        System.out.println("Bob started - is going to send news to Alice");

        bob.newsKP.addNews("New Newsfeed available");
        bob.newsKP.addNews("Now i have one more news");
        bob.newsKP.printNews();

        PeerSemanticTag alice = InMemoSharkKB.createInMemoPeerSemanticTag("Alice",
                "http://www.sharksystem.net/alice.html",
                "tcp://localhost:7070");

        bob.newsKP.sendNews(alice);
        try {
            TimeUnit.SECONDS.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        bob.newsKP.deleteold();







    }

    @Override
    public void messageReceived() throws SharkException, IOException {
            System.out.println("News recived");
            newsKP.printNews();
        }

    }
