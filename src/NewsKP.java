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
import java.util.Iterator;
import java.util.Vector;

//ToDo: Interests should be with direction

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
     * Initialises the Taxonomy to discribe a Source Interest News and subtopics (for example Sports)
     *
     * @throws SharkException
     */
    private void initTaxonomy() throws SharkException {
        //ToDo: Anpassen von Owner und Peer bzw der Fragmentations Parameters

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

    public TXSemanticTag getNewsTopic() {

        return newsinterest;
    }

    /**
     * adds a Newsfeed to the Database
     *
     * @param message The News to be included
     * @throws SharkException
     * @throws IOException
     */
    public void addNews(String message) throws SharkException, IOException {
        addNews(message, newsinterest);
    }

    public void addNews(String message, TXSemanticTag newsTag) throws SharkException, IOException {
        // create context point for new message

        // first create coordinates

        ContextCoordinates cc = kb.createContextCoordinates(
                newsTag, // its a newsfeed
                this.owner, // originator is owner of engine
                this.owner, // peer is owner as well
                null, // talking to anybody
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

    private SemanticTag getnewsTag() {

        return newsinterest;
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

    /**
     * does nothing - we handle anything by exchanging knowledge
     * could be integrated when we use categories for Newsfeeds
     *
     * @param interest
     * @param kepConnection
     */
    @Override
    protected void doExpose(SharkCS interest, KEPConnection kepConnection) {

        //ToDo: Follow subtags like Sport -> Fussball
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
                        sendKnowledge(mutualInterest, kepConnection);
                    } catch (SharkSecurityException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        } catch (SharkKBException e) {
            e.printStackTrace();
        }



    }

    public void sendInteresst(PeerSemanticTag remotePeer) throws SharkKBException, SharkSecurityException, IOException {

        Iterator<SharkCS> InterestIterator =  kb.interests();
        for(;InterestIterator.hasNext();){
            Interest inter = (Interest) InterestIterator.next();
            sendInterest(inter, remotePeer);
        }

    }

    /**
     * Add a new Topic to the Newstopics for example Sport
     *
     * @param topic      Topic of the News
     * @param desciption the Newsstring itself
     * @throws SharkKBException
     */
    public void addNewsTopic(String topic, String desciption) throws SharkKBException {
        addNewsTopic(topic, desciption, newsinterest);
    }

    public void addNewsTopic(String topic, String description, TXSemanticTag supertopic) throws SharkKBException {
        Taxonomy tx = kb.getTopicsAsTaxonomy();

        /**Create new Topic as Taxanomie*/
        TXSemanticTag newtopic = tx.createTXSemanticTag(topic, description);

        /**Move new topic under the source Topic (News)*/
        newtopic.move(supertopic);

        Interest newInterest = kb.createInterest(kb.createContextCoordinates(
                newtopic,
                null, // originator is owner of engine
                null, // peer is owner as well
                null, // talking to anybody
                //    InMemoSharkKB.createInMemoTimeSemanticTag(System.currentTimeMillis(), 24 * 60 * 60 * 1000), // time
                null, // time
                null,   //place irrelevant
                SharkCS.DIRECTION_INOUT // Exchange News (in an out)
        ));
        kb.addInterest(newInterest);
        System.out.println(L.stSet2String(tx));

    }

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

    private void sendKnowledge(Interest mutualInterest, KEPConnection destination) throws SharkKBException, SharkSecurityException, IOException {
        Taxonomy tx = kb.getTopicsAsTaxonomy();
        Enumeration<SemanticTag> txenum = tx.tags();
        Enumeration<SemanticTag> interestenum = mutualInterest.getTopics().tags();

        TXSemanticTag txtag = null;
        outerloop:
        for(;interestenum.hasMoreElements();){
            String interesttopic = interestenum.nextElement().getName();

            for(;txenum.hasMoreElements();){
                txtag = (TXSemanticTag) txenum.nextElement();
                String txtopic = txtag.getName();
                if(interesttopic.equals(txtopic)){
                  break outerloop;

                }
            }
        }
        if(txtag != null) {
            ContextPoint stkb = getCPfromTopic(txtag);
            if (stkb != null) {
                sendCP(stkb);
            }
            TXSemanticTag supertag = txtag;

            //ToDo: Find a way to get all subtags - rekursion
            do{
                Enumeration<TXSemanticTag> subtagenum = supertag.getSubTags();
                for (; subtagenum.hasMoreElements(); ) {
                    TXSemanticTag stinterest = subtagenum.nextElement();
                    stkb = getCPfromTopic(stinterest);
                    if (stkb != null) {
                        sendCP(stkb);
                    }
                }
            }while(supertag.getSubTags() != null);
        }
    }

/*

        if (cpEnum != null) {
            do {
                ContextPoint cpout = cpEnum.nextElement();
                STSet context = mutualInterest.getTopics().contextualize(cpout.getContextCoordinates().getTopics());

                Enumeration<SemanticTag> enumtag = mutualInterest.getTopics().tags();
                if(!context.isEmpty()){
                    Knowledge k = InMemoSharkKB.createInMemoKnowledge();
                    k.addContextPoint(cpout);
                    this.sendKnowledge(k, InMemoSharkKB.createInMemoPeerSemanticTag("Bob",
                            "http://www.sharksystem.net/bob.html",
                            "tcp://localhost:7071"));
                }

            } while (cpEnum.hasMoreElements());
        }
*/

    private void sendCP(ContextPoint cp) throws SharkSecurityException, IOException, SharkKBException {
        Knowledge k = InMemoSharkKB.createInMemoKnowledge();
        k.addContextPoint(cp);
        this.sendKnowledge(k, InMemoSharkKB.createInMemoPeerSemanticTag("Bob",
                "http://www.sharksystem.net/bob.html",
                "tcp://localhost:7071"));
    }

    private void getSubTags(TXSemanticTag root){

    }
}


