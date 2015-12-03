
import java.io.IOException;
import net.sharkfw.kep.SharkProtocolNotSupportedException;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.peer.J2SEAndroidSharkEngine;
import net.sharkfw.peer.SharkEngine;

/**
 *
 * @author thsc
 */
public abstract class NewsPeerTCP implements NewsListener {
    private SharkEngine se = null;
    protected final NewsKP newsKP;

    // set up new chat partner
    public NewsPeerTCP(String peerName, String peerSI, String address, int port) throws SharkProtocolNotSupportedException, IOException {
        this.se = new J2SEAndroidSharkEngine();

        // create PeerSemanticTag describing peer itself
        PeerSemanticTag peer = InMemoSharkKB.createInMemoPeerSemanticTag(peerName, peerSI, address);

        // create chatkp
        this.newsKP = new NewsKP(this.se, peer);

        // subscribe to news
        this.newsKP.addListener(this);

        // start listening at address - can throw exceptions
        this.se.startTCP(port);
    }

    public void stop() {
        this.se.stop();
    }
}
