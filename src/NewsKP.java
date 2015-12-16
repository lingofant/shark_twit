import net.sharkfw.knowledgeBase.*;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.peer.KEPConnection;
import net.sharkfw.peer.KnowledgePort;
import net.sharkfw.peer.SharkEngine;
import net.sharkfw.system.L;
import net.sharkfw.system.SharkException;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

//ToDo: Interests should be with direction, the tree of interests should be changeable - move a topic under a subtopic

/**
 * Created by timol on 21.11.2015.
 * KnowledgePort of the Shark Newsreader
 */


public class NewsKP extends KnowledgePort {

    /**
     * URI of the Topic Chat
     */
    //ToDo: Change to Topic as Taxanomie
    public static final String CHAT_TOPIC_SI = "http://www.sharksystem.net/examples/chat/chatinterest.html";
    /**
     * Topic of allnews
     */
    public static final String newsTag_ST = "news";   //Markieren der Messages als News - muss noch geändert werden nach URI
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
     * Fragmentation Parameter for the do Expose Method
     */
    FragmentationParameter fp;



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
     * Initialises the Taxonomy to discribe a Source Interest News and subtopics (for example Sports)
     *
     * @throws SharkException
     */
    private void initTaxonomy() throws SharkException {
        Taxonomy tx = kb.getTopicsAsTaxonomy();
        // Describe the source of all Interests as News
        newsinterest = tx.createTXSemanticTag("News", "www.shark-news.de");

    }

    /**
     * Add a new Topic to the Newstopics for example Sport
     *
     * @param topic      Topic of the News
     * @param desciption the Newsstring itself
     * @throws SharkKBException
     */

    public void addNewsTopic(String topic, String desciption) throws SharkKBException {
        Taxonomy tx = kb.getTopicsAsTaxonomy();
        /**Create new Topic as Taxanomie*/
        TXSemanticTag newtopic = tx.createTXSemanticTag(topic, desciption);
        /**Move new topic under the source Topic (News)*/
        newtopic.move(newsinterest);
        System.out.println(L.stSet2String(tx));
    }

    public Vector getNews() throws SharkKBException {


        Vector news = new Vector();
        Enumeration<ContextPoint> cpEnum = kb.getAllContextPoints();
        if (cpEnum != null) {
            do {
                ContextPoint cpout = cpEnum.nextElement();
                Iterator<Information> infoIter = cpout.getInformation();
                if (infoIter != null) {
                    String messageout;
                    try {
                        messageout = infoIter.next().getContentAsString();
                        news.add(messageout);

                    } catch (Exception ex) {
                        // something went wrong - should be handled in a final version
                    }
                }
            } while (cpEnum.hasMoreElements());
        }
        return news;

    }
    public TXSemanticTag getNewsTopic(){

        return newsinterest;
    }

    public void sendInteresst(PeerSemanticTag remotePeer){
     //   this.sendInterest(newsinterest, remotePeer);

    }

    public void setFP(boolean superTags, boolean subTags, int depth) {
        fp = new FragmentationParameter(superTags, subTags, depth);

    }


    public FragmentationParameter getFP(){
        return fp;
    }

    /**
     * adds a Newsfeed to the Database
     *
     * @param message The News to be included
     * @throws SharkException
     * @throws IOException
     */

    public void addNews(String message) throws SharkException, IOException {
        // create context point for new message

        // first create coordinates

        SemanticTag newsTag = kb.createSemanticTag("NewsTopic", newsTag_ST); //why deprecated, every time?
        ContextCoordinates cc = kb.createContextCoordinates(
                newsTag, // its a newsfeed
                this.owner, // originator is owner of engine
                this.owner, // peer is owner as well
                null, // talking to anybody
                //    InMemoSharkKB.createInMemoTimeSemanticTag(System.currentTimeMillis(), 24 * 60 * 60 * 1000), // time
                InMemoSharkKB.createInMemoTimeSemanticTag(System.currentTimeMillis(), 15), // time
                null,   //place irrelevant
                SharkCS.DIRECTION_INOUT // Exchange News (in an out)
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


//ToDo: Topics of News have to be integrated

    /**
     * News got received add the News to the Knowledgebase
     *
     * @param k
     * @param kepConnection
     */
    @Override
    protected void doInsert(Knowledge k, KEPConnection kepConnection) {


        Enumeration<ContextPoint> cpEnum = k.contextPoints();

        if (cpEnum != null) {
            ContextPoint cp = cpEnum.nextElement();

            ContextCoordinates cc = cp.getContextCoordinates();

            SemanticTag topic = cc.getTopic();
            if (!SharkCSAlgebra.identical(topic, this.getnewsTag())) {    //orginal getChatTopic
                // this knowledge is not a Newsfeed
                return;
            }


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
     * Validates if there`s already a ContextPoint with the same Owner and Time in the Knowledge Base
     * We assume that if Owner and Time is identical the information Stored is also identical
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

    // creates an object any time - could be made better...
    private SemanticTag getnewsTag() {
        SemanticTag newsTag = InMemoSharkKB.createInMemoSemanticTag("NewsTopic", NewsKP.newsTag_ST);

        return newsTag;
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
     * Exchange all News with a Remote Peer
     *
     * @param remotePeer
     * @throws SharkException
     * @throws IOException
     */

    public void sendNews(PeerSemanticTag remotePeer) throws SharkException, IOException {

        SemanticTag plST = InMemoSharkKB.createInMemoSemanticTag("NewsTopic", newsTag_ST);
        ContextCoordinates cout = InMemoSharkKB.createInMemoContextCoordinates(plST, null, null, null, null, null, SharkCS.DIRECTION_INOUT);
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

    //ToDo: a getter Method for the News to be printed out

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

        SemanticTag newsTag = kb.createSemanticTag("NewsTopic", newsTag_ST); //why deprecated, every time?

        ContextCoordinates ccnew = kb.createContextCoordinates(
                newsTag, // its a newsfeed
                cp.getContextCoordinates().getOriginator(), // originator is owner of engine
                cp.getContextCoordinates().getPeer(), // peer is owner as well
                null, // talking to anybody
                cp.getContextCoordinates().getTime(), // time
                null, //place is irrelevant
                SharkCS.DIRECTION_INOUT // Exchange News (in an out)
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

    //ToDo: Add the exchange of Information

    /**
     * does nothing - we handle anything by exchanging knowledge
     * could be integrated when we use categories for Newsfeeds
     *
     * @param interest
     * @param kepConnection
     */
    @Override
    protected void doExpose(SharkCS interest, KEPConnection kepConnection) {

  /*    System.out.println("start do expose");
        STSet fragment;


        Enumeration<TXSemanticTag> interestEnum = newsinterest.getSubTags();


        while(interestEnum.nextElement()!=null) {
            TXSemanticTag topic = interestEnum.nextElement();
            Interest mutualInterest = SharkCSAlgebra.contextualize( topic, interest, getFP());

            SharkCS storedInterest = interestEnum
// mutual interest? Interest mutualInterest = SharkCSAlgebra.contextualize( storedInterest, interest, fps);
            if(mutualInterest != null) { kepConnection.expose(mutualInterest); }


            /*
            Iterator<SharkCS> interestIter = storedInterests.getInterests();
while(interestIter.hasNext()) { SharkCS storedInterest = interestIter.next();
// mutual interest? Interest mutualInterest = SharkCSAlgebra.contextualize( storedInterest, interest, fps);
if(mutualInterest != null) { kepConnection.expose(mutualInterest); }
}

        }
*/
    }
}
