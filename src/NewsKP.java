import net.sharkfw.knowledgeBase.*;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.peer.KEPConnection;
import net.sharkfw.peer.KnowledgePort;
import net.sharkfw.peer.SharkEngine;
import net.sharkfw.system.L;
import net.sharkfw.system.SharkException;
import net.sharkfw.system.SharkSecurityException;

import java.io.IOException;
import java.util.Enumeration;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.Vector;



/**
 * Created by timol on 21.11.2015.
 * KnowledgePort of the Shark Newsreader
 */


public class NewsKP extends KnowledgePort {

    /**
     * remote Peer cause we want to exchange news with all Shark Newsreader Users
     */
    private PeerSemanticTag remotePeer = null;
    /**
     * Listener of the Shark Newsreader
     */
    private NewsListener listener = null;
    /**
     * The Peer of the Knowledgeport is the owner
     */
    private final PeerSemanticTag owner;
    /**
     * Shark Knowledgebase
     */
    SharkKB kb = null;
    /**
     * Source Semantic Tag of News
     */
    TXSemanticTag newsinterest;


    /**
     * Konstruktor of the Knewsknowledgeport
     *
     * @param se    Shark engine which should be used
     * @param owner Owner of the Knowlegeport
     */
    public NewsKP(SharkEngine se, PeerSemanticTag owner) {
        super(se);
        this.owner = owner;
        initKnowledgeBase();
    }

    /**
     * Initialise the KnowledgeBase
     */
    private void initKnowledgeBase() {
        kb = new InMemoSharkKB();
        kb.setOwner(this.owner);
        try {
            initTaxonomy();
        } catch (SharkException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialises the Taxonomy of interests and the Source of the Taxonomy called "News" which is used to add subtopics
     *
     * @throws SharkException
     */
    private void initTaxonomy() throws SharkException {
        Taxonomy tx = kb.getTopicsAsTaxonomy();
        // Describe the source of all Interests as News
        newsinterest = tx.createTXSemanticTag("News", "www.shark-news.de");

        Interest newInterest = kb.createInterest(kb.createContextCoordinates(
                newsinterest,
                null, // originator is owner of engine
                null, // peer is owner as well
                null, // talking to anybody
                null, // time
                null,   //place irrelevant
                SharkCS.DIRECTION_INOUT // Exchange News (in an out)
        ));
        kb.addInterest(newInterest);
    }


    /**
     * Adds the Listener to the Knowlegeport
     *
     * @param listener Object of the Listener which should be used
     */
    public void addListener(NewsListener listener) {
        this.listener = listener;
    }

    /**
     * The Method returns a Vector of all News which are in the KB
     * @return Vector
     * @throws SharkKBException
     */
    public Vector getNews() throws SharkKBException {
        Vector news = new Vector();
        Enumeration<ContextPoint> cpEnum = kb.getAllContextPoints();
        if (cpEnum != null) {
            do {
                ContextPoint cpout = cpEnum.nextElement();
                Iterator<Information> infoIter = cpout.getInformation();
                if (infoIter != null) {
                    String messageout;
                    messageout = infoIter.next().getContentAsString();
                    news.add(messageout);
                }
            } while (cpEnum.hasMoreElements());
        }
        return news;
    }

    /**
     * adds a Newsfeed to the Database, the Topic is News
     *
     * @param message The News to be included
     * @throws SharkException
     * @throws IOException
     */
    public void addNews(String message) throws SharkException, IOException {
        addNews(message, newsinterest);
    }

    /**
     * adds a Newsfeed to the Databased to the specified Topic "newsTag"
     *
     * @param message The Newsfeed
     * @param newsTag TXSemanticTag of the Topic
     * @throws SharkException
     * @throws IOException
     */
    public void addNews(String message, TXSemanticTag newsTag) throws SharkException, IOException {
        Interest inter = getTopicAsInterst(newsTag.getName());
        ContextCoordinates cc = kb.createContextCoordinates(
                newsTag, // its a newsfeed
                this.owner, // originator is owner of engine
                this.owner, // peer is owner as well
                null, // talking to anybody
                InMemoSharkKB.createInMemoTimeSemanticTag(System.currentTimeMillis(), 15), // time
                null,   //place irrelevant
                //SharkCS.DIRECTION_INOUT
                inter.getDirection() // Exchange News (in an out)
        );

        // all metadata set - lets create a context point
        ContextPoint cp = kb.createContextPoint(cc);
        // add message
        cp.addInformation(message);
        // create knowledge
        Knowledge k = kb.createKnowledge();
        // add context point
        k.addContextPoint(cp);
    }

    /**
     * News got received add the News to the Knowledgebase
     *
     * @param k The Knowledge which contains the news
     * @param kepConnection The Connection to the Sender
     */
    @Override
    protected void doInsert(Knowledge k, KEPConnection kepConnection) {


        Enumeration<ContextPoint> cpEnum = k.contextPoints();

        if (cpEnum != null) {
            ContextPoint cp = cpEnum.nextElement();

            ContextCoordinates cc = cp.getContextCoordinates();

            SemanticTag topic = cc.getTopic();
       //     if (!SharkCSAlgebra.identical(topic, this.getnewsTag())) {    //orginal getChatTopic
        //        // this knowledge is not a Newsfeed
         //       return;
            //  }


            // add News feed to Knowledgebase
            // all metadata set - lets create a context point

            try {
                if (!isInKB(cc)) {
                    addKnowledge(cp);
                    if (this.listener != null) {
                        this.listener.messageReceived();
                    }
                }
            } catch (SharkException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    /**
     *
     * Validates if there`s already a ContextPoint with the same Owner and Time in the Knowledge Base
     * We assume that if Owner and Time is identical the information Stored is also identical
     * @param cc ContectCoordinates of the ContextPoint
     * @return true if it`s in KB
     * @throws SharkException
     * @throws IOException
     */
    private boolean isInKB(ContextCoordinates cc) throws SharkException, IOException {
        boolean isInKB = false;
        Enumeration<ContextPoint> cpEnum = kb.getAllContextPoints();
        if (cpEnum != null) {
            do {
                ContextPoint cpout = cpEnum.nextElement();
                ContextCoordinates ckb = cpout.getContextCoordinates();
                if (SharkCSAlgebra.identical(cc.getTime(), ckb.getTime())) {
                    if (SharkCSAlgebra.identical(cc.getTopic(), ckb.getTopic())) {
                        isInKB = true;
                        break;
                    }
                }
            } while (cpEnum.hasMoreElements());
        }
        if (isInKB) System.out.println("Already in KB");
        return isInKB;
    }


    /**
     * Exchange all News with a Remote Peer
     *
     * @param remotePeer
     * @throws SharkException
     * @throws IOException
     */

    public void sendAllNews(PeerSemanticTag remotePeer) throws SharkException, IOException {

        Enumeration<ContextPoint> cpEnum = kb.getAllContextPoints();
        if (cpEnum != null) {
            while (cpEnum.hasMoreElements()) {
                Knowledge k = InMemoSharkKB.createInMemoKnowledge();
                ContextPoint cpout = cpEnum.nextElement();
                k.addContextPoint(cpout);
                this.sendKnowledge(k, remotePeer);
            }
        }

    }

    /**
     * Print all news with System.out.println
     * Only for evaluation
     *
     * @throws SharkException
     * @throws IOException
     */
    public void printNews() throws SharkException, IOException {

        Enumeration<ContextPoint> cpEnum = kb.getAllContextPoints();
        if (cpEnum != null) {
            do {
                ContextPoint cpout = cpEnum.nextElement();
                Iterator<Information> infoIter = cpout.getInformation();
                if (infoIter != null) {
                    String messageout;
                    try {
                        messageout = infoIter.next().getContentAsString();
                        System.out.println(messageout);
                        // notify listener about new message
                    } catch (Exception ex) {
                        // something went wrong - should be handled in a final version
                    }
                }
            } while (cpEnum.hasMoreElements());
        }

    }

    /**
     * If a News got received we call the Method to inform the GUI
     *
     * @param message
     * @param sender
     * @throws SharkException
     * @throws IOException
     */
    private void messageReceived(String message, PeerSemanticTag sender) throws SharkException, IOException {
        if (this.listener != null) {
            this.listener.messageReceived();
        }
    }

    /**
     * Extract the Newsstring out of a context point and add it to the Knowledgebase
     *
     * @param cp
     * @throws SharkException
     * @throws IOException
     */

    private void addKnowledge(ContextPoint cp) throws SharkException, IOException {

        ContextCoordinates ccnew = kb.createContextCoordinates(
                cp.getContextCoordinates().getTopic(), // its a newsfeed
                cp.getContextCoordinates().getOriginator(), // originator is owner of engine
                cp.getContextCoordinates().getPeer(), // peer is owner as well
                null, // talking to anybody
                cp.getContextCoordinates().getTime(), // time
                null, //place is irrelevant
                cp.getContextCoordinates().getDirection() // Exchange News (in an out)
        );

        // all metadata set - lets create a context point
        ContextPoint cpnew = kb.createContextPoint(ccnew);


        Iterator<Information> infoIter = cp.getInformation();
        if (infoIter != null) {
            String messageout;
            try {
                messageout = infoIter.next().getContentAsString();
                cpnew.addInformation(messageout);

                // notify listener about new message
            } catch (Exception ex) {
                // something went wrong - should be handled in a final version
            }
            // create knowledge
            Knowledge k = kb.createKnowledge();

            // add context point
            k.addContextPoint(cpnew);

        }
    }

    /**
     * Delete all News older than the Duration of that News
     *
     * @throws SharkException
     * @throws IOException
     */
    public void deleteold() throws SharkException, IOException {

        TimeSemanticTag time = InMemoSharkKB.createInMemoTimeSemanticTag(System.currentTimeMillis(), 24 * 60 * 60 * 1000);
        Enumeration<ContextPoint> cpEnum = kb.getAllContextPoints();
        if (cpEnum != null) {
            do {
                ContextPoint cpout = cpEnum.nextElement();
                TimeSemanticTag timecp = cpout.getContextCoordinates().getTime();
                if (timecp.getFrom() + timecp.getDuration() < time.getFrom()) {
                    kb.removeContextPoint(cpout.getContextCoordinates());
                }

            } while (cpEnum.hasMoreElements());


        }
    }

    /**
     * Get's called if a Peer sends interests. Evaluates if the interest is in the KB and calls Methods
     * to respond with the News which are related to the Topic (all Subtags of the Interest) if the Direction is out
     *
     * @param interest
     * @param kepConnection
     */
    @Override
    protected void doExpose(SharkCS interest, KEPConnection kepConnection) {
        PeerSemanticTag destination = interest.getOriginator();
        FragmentationParameter[] fps = new FragmentationParameter[SharkCS.MAXDIMENSIONS];
        fps[SharkCS.DIM_TOPIC] = new FragmentationParameter( true, // don’t follow super tags
                                                true, // follow sub tags
                                                SharkCS.MAXDIMENSIONS); //depth 1

        try {
            Iterator<SharkCS> interestIter = kb.interests();
            while (interestIter.hasNext()) {
                SharkCS storedInterest = interestIter.next();

                Interest mutualInterest = null;
                try {
                    mutualInterest = SharkCSAlgebra.contextualize(storedInterest, interest, fps);
                } catch (SharkKBException e) {
                    e.printStackTrace();
                }
                System.out.println("Alice calculates: Bob / Alice:\n " + L.contextSpace2String(mutualInterest));
                if(mutualInterest!=null){
                    try {
                        sendKnowledge(mutualInterest, destination);
                    } catch (SharkSecurityException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SharkException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (SharkKBException e) {
            e.printStackTrace();
        }
    }

    /**
     * sends Intersts to the Remote Peer, but only Subtopics of News not the Root "News" itself.
     *
     * @param remotePeer
     * @throws SharkKBException
     * @throws SharkSecurityException
     * @throws IOException
     */

    public void sendInteresst(PeerSemanticTag remotePeer) throws SharkKBException, SharkSecurityException, IOException {
        Iterator<SharkCS> InterestIterator = kb.interests();
        for (; InterestIterator.hasNext(); ) {
            Interest inter = (Interest) InterestIterator.next();
            if(!inter.getTopics().tags().nextElement().getName().equals("News")) {
                if (inter.getDirection() == 2 || inter.getDirection() == 0) {
                    inter.setOriginator(owner);
                    sendInterest(inter, remotePeer);
                }
            }
        }
    }

    /**
     * Sends the Root Interest "News" to the Remotepeer to get all News
     *
     * @param remotePeer
     * @throws SharkKBException
     * @throws SharkSecurityException
     * @throws IOException
     */

    public void sendAllIntersesst(PeerSemanticTag remotePeer)throws SharkKBException, SharkSecurityException, IOException {
        Iterator<SharkCS> InterestIterator =  kb.interests();
        for(;InterestIterator.hasNext();){
            Interest inter = (Interest) InterestIterator.next();
            if(inter.getTopics().tags().nextElement().getName().equals("News")) {
                inter.setOriginator(owner);
                sendInterest(inter, remotePeer);
            }
        }

    }

    /**
     * Add a new Topic to the Newstopics. The Topic is placed under the Root "News"
     *The Direction is 2
     *
     * @param topic      The Topic
     * @param desciption The URI
     * @throws SharkKBException
     */
    public void addNewsTopic(String topic, String desciption) throws SharkKBException {
        addNewsTopic(topic, desciption, newsinterest, 2);
    }

    /**
     * Add a new Topic to the Newstopics. The Topic gets placed under the defined Topic "supertopic" with the direction defined.
     * @param topic Name of the Topic
     * @param description URI of the Topic
     * @param supertopic TXSemanticTag of the Supertopic
     * @param direction direction of the Topic
     * @throws SharkKBException
     * @throws IllegalFormatException
     */


    public void addNewsTopic(String topic, String description, TXSemanticTag supertopic, int direction) throws SharkKBException, IllegalFormatException {
        if(direction < 0 || direction > 3){
            throw new IllegalArgumentException("direction in a wrong format");
        }
        Taxonomy tx = kb.getTopicsAsTaxonomy();
        TXSemanticTag newtopic = tx.createTXSemanticTag(topic, description);
        newtopic.move(supertopic);
        Interest newInterest = kb.createInterest(kb.createContextCoordinates(
                newtopic,
                null, // originator is owner of engine
                null, // peer is owner as well
                null, // talking to anybody
                //    InMemoSharkKB.createInMemoTimeSemanticTag(System.currentTimeMillis(), 24 * 60 * 60 * 1000), // time
                null, // time
                null,   //place irrelevant
                direction // Exchange News (in an out)
        ));
        kb.addInterest(newInterest);
        System.out.println(L.stSet2String(tx));
    }

    /**
     * Returns a TXSemanticTag which hast the specified topic
     * @param topic topic to be searched for
     * @return TXSemanticTag
     * @throws SharkKBException
     */
    public TXSemanticTag getTopicasSemanticTag (String topic) throws SharkKBException {
        TXSemanticTag supertopic = null;
        Taxonomy txs = kb.getTopicsAsTaxonomy();
        Enumeration<SemanticTag> enumRoot = txs.tags();
        if(enumRoot != null){
            do{
                SemanticTag tag = enumRoot.nextElement();
                if(topic.equals(tag.getName())){
                    supertopic = (TXSemanticTag) tag;
                }
            }while ( enumRoot.hasMoreElements());
        }
        return supertopic;
    }

    /**
     * Returns the Interest with the specified Topic
     * @param topic
     * @return
     * @throws SharkKBException
     */
    public Interest getTopicAsInterst(String topic) throws SharkKBException {
        Iterator<SharkCS> interenum = kb.interests();
        for(;interenum.hasNext();){
            Interest inter = (Interest)interenum.next();
            STSet topics = inter.getTopics();
            Enumeration<SemanticTag> tagenum = topics.tags();
            for(;tagenum.hasMoreElements();){
                if(tagenum.nextElement().getName().equals(topic)){
                    return inter;
                }
            }
        }
        return null;

    }

    /**
     * Returns the ContextPoint of a TXSemanticTag
     * @param topic
     * @return
     * @throws SharkKBException
     */
    private ContextPoint getCPfromTopic(TXSemanticTag topic) throws SharkKBException {
        Enumeration<ContextPoint> cpEnum = kb.getAllContextPoints();
        for (; cpEnum.hasMoreElements(); ) {
            ContextPoint stkb = cpEnum.nextElement();
            if (stkb.getContextCoordinates().getTopic().getName().equals(topic.getName())) {
                return stkb;
            }
        }
        return null;
    }

    /**
     * Sends all the News which are related to a Topic including all the subtags
     *
     * @param supertag
     * @param destination
     * @throws SharkException
     * @throws IOException
     */
    private void sendSubTags(TXSemanticTag supertag, PeerSemanticTag destination) throws SharkException, IOException {
        Enumeration<TXSemanticTag> subtagenum =supertag.getSubTags();
        if(subtagenum == null) return;
        while(subtagenum.hasMoreElements()){
            TXSemanticTag txtag = subtagenum.nextElement();
            ContextPoint stkb = getCPfromTopic(txtag);
            if (stkb != null && (stkb.getContextCoordinates().getDirection() == 1 || stkb.getContextCoordinates().getDirection() == 2))
                sendCP(stkb, destination);
            sendSubTags(txtag, destination);
        }
    }


    /**
     * Sends Knowledge to a destination
     * @param mutualInterest
     * @param destination
     * @throws SharkException
     * @throws IOException
     */
    private void sendKnowledge(Interest mutualInterest, PeerSemanticTag destination) throws SharkException, IOException {
        Taxonomy tx = kb.getTopicsAsTaxonomy();
        Enumeration<SemanticTag> txenum = tx.tags();
        Enumeration<SemanticTag> interestenum = mutualInterest.getTopics().tags();
        TXSemanticTag txtag = null;
        outerloop:
        for (; interestenum.hasMoreElements(); ) {
            String interesttopic = interestenum.nextElement().getName();
            for (; txenum.hasMoreElements(); ) {
                TXSemanticTag txtag_buf = (TXSemanticTag) txenum.nextElement();
                String txtopic = txtag_buf.getName();
                if (interesttopic.equals(txtopic)) {
                    txtag = txtag_buf;
                    break outerloop;
                }
            }
        }
        if (txtag != null) {
            ContextPoint stkb = getCPfromTopic(txtag);
            if (stkb != null  && (stkb.getContextCoordinates().getDirection() == 1 || stkb.getContextCoordinates().getDirection() == 2)) {
                sendCP(stkb, destination);
            }
            sendSubTags(txtag, destination);
        }
    }

    /**
     * Sends a ContextPoint to the destination
     * @param cp
     * @param destination
     * @throws SharkException
     * @throws IOException
     */
    private void sendCP(ContextPoint cp, PeerSemanticTag destination) throws SharkException, IOException {
        Knowledge k = InMemoSharkKB.createInMemoKnowledge();
        k.addContextPoint(cp);
        sendKnowledge(k, destination);
    }
}