

import java.io.IOException;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.system.SharkException;

/**
 * @author thsc
 */
public interface NewsListener {
    public void messageReceived() throws SharkException, IOException;
}
