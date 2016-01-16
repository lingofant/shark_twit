
import java.io.IOException;
import net.sharkfw.kep.SharkProtocolNotSupportedException;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.system.SharkException;

/**
 *
 * @author thsc
 */
public class Alice extends NewsPeerTCP {

    public Alice(String peerName, String peerSI, String address, int port) throws SharkProtocolNotSupportedException, IOException {
        super(peerName, peerSI, address, port);
    }

    @Override
    public void messageReceived() throws SharkException, IOException {
        System.out.println("news received:");
        newsKP.printNews();

    }

    public static void main(String[] args) throws SharkException, IOException {
        // settup alice peer
//        L.setLogLevel(L.LOGLEVEL_ALL);


        Alice alice = new Alice("Alice",
                "http://www.sharksystem.net/alice.html",
                "tcp://localhost:7070",
                7070
        );

        alice.newsKP.addNewsTopic("Sport", "www.sport.de");
        alice.newsKP.addNewsTopic("Nachrichten", "www.tagesthemen.de");
        alice.newsKP.addNewsTopic("Fussball", "www.bundlesliga.de", alice.newsKP.getTopicasSemanticTag("Sport"), 2);

        alice.newsKP.addNews("Bayern nervt", alice.newsKP.getTopicasSemanticTag("Fussball"));
        alice.newsKP.addNews("Sth about IS", alice.newsKP.getTopicasSemanticTag("Nachrichten"));
        alice.newsKP.addNews("Sport sucks", alice.newsKP.getTopicasSemanticTag("Sport"));

        alice.newsKP.addNews("Die Welt geht unter");

        System.out.println("Alice is running - start Bob now");

        alice.newsKP.printNews();

/*        try {
            TimeUnit.SECONDS.sleep(30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

*/
 //       alice.newsKP.addNews("This is a new Newsfeed from Alice");

        PeerSemanticTag bob = InMemoSharkKB.createInMemoPeerSemanticTag("Bob",
                "http://www.sharksystem.net/bob.html",
                "tcp://localhost:7071");








    }
}
