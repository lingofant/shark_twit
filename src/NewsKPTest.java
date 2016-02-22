import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.TXSemanticTag;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

/**
 * Created by timol on 21.01.2016.
 */
public class NewsKPTest {

    Alice alice;
    Bob bob;
    PeerSemanticTag alicepeer;

    @Before
    public void setUp() throws Exception {

        init_Peers init = init_Peers.getInstance();
        alice = init.getAlice();
        bob = init.getBob();

        alice.newsKP.addNewsTopic("Sport", "www.sport.de");
        alice.newsKP.addNewsTopic("Fussball", "www.bundlesliga.de", alice.newsKP.getTopicasSemanticTag("Sport"), 2);
        alice.newsKP.addNewsTopic("Politik", "www.politik.de", alice.newsKP.getTopicasSemanticTag("News"), 0);




        alice.newsKP.addNews("Supernews");
        alice.newsKP.addNews("Sportnews", alice.newsKP.getTopicasSemanticTag("Sport"));
        alice.newsKP.addNews("Politiknews", alice.newsKP.getTopicasSemanticTag("Politik"));
        alice.newsKP.addNews("Fussballnews", alice.newsKP.getTopicasSemanticTag("Fussball"));

        bob.newsKP.addNewsTopic("Sport", "www.sport.de");
        bob.newsKP.addNewsTopic("Fussball", "www.bundlesliga.de", alice.newsKP.getTopicasSemanticTag("Sport"), 2);
        bob.newsKP.addNewsTopic("Politik", "www.politik.de", alice.newsKP.getTopicasSemanticTag("News"), 2);

        alicepeer = InMemoSharkKB.createInMemoPeerSemanticTag("Alice",
                "http://www.sharksystem.net/alice.html",
                "tcp://localhost:7070");

        alice.newsKP.setDurationtime(10);
        alice.newsKP.addNews("Durationtest");


    }

    @Test
    public void testAddNewsTopic() throws Exception {
/*Add news Topics
*  1. Under SuperTopic
*  2. Under Subtopic
*  1. Direction 0 under Supertopic
* */


        TXSemanticTag interests = alice.newsKP.getTopicasSemanticTag("News");
        Enumeration<TXSemanticTag> interenum = interests.getSubTags();

        while(interenum.hasMoreElements()){
            TXSemanticTag inter = interenum.nextElement();
            String topic = inter.getName();
            Assert.assertTrue(topic.equals("Sport") || topic.equals("Politik"));
            if(topic.equals("Politik"))
                Assert.assertNull(inter.getSubTags());
            else{
                Assert.assertNotNull(inter.getSubTags());
            }

        }

    }

    @Test
    public void testAddNews() throws Exception {
        /* Add news
        1. Under Supertopic
        2. Under Subtopic
        3. Under Topic with Direction 0
        4. Test - get News
         */


        Vector allnews = alice.newsKP.getNews();
        Assert.assertTrue(allnews.contains("Supernews"));
        Assert.assertTrue(allnews.contains("Sportnews"));
        Assert.assertTrue(allnews.contains("Fussballnews"));
        Assert.assertTrue(allnews.contains("Politiknews"));
        Assert.assertFalse(allnews.contains("foo"));
    }



    @Test
    public void testSendInteresst() throws Exception {
        /*
        Send interest
        Subtopic, Subsubtopic should be exchanged
         */

        bob.newsKP.sendInteresst(alicepeer);
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Vector allnews = bob.newsKP.getNews();
        Assert.assertFalse(allnews.contains("Supernews"));
        Assert.assertTrue(allnews.contains("Sportnews"));
        Assert.assertTrue(allnews.contains("Fussballnews"));
        Assert.assertFalse(allnews.contains("Politiknews"));
        Assert.assertFalse(allnews.contains("foo"));

    }

    @Test
    public void testSendAllIntersesst() throws Exception {
        /*
        All should be exchanged but not direction 0
         */

        try {
            TimeUnit.SECONDS.sleep(15);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        bob.newsKP.sendAllIntersesst(alicepeer);
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        Vector allnews = bob.newsKP.getNews();
        Assert.assertTrue(allnews.contains("Supernews"));
        Assert.assertTrue(allnews.contains("Sportnews"));
        Assert.assertTrue(allnews.contains("Fussballnews"));
        Assert.assertFalse(allnews.contains("Politiknews"));
        Assert.assertFalse(allnews.contains("foo"));

    }


    @Test
    public void testDeleteold() throws Exception {

        /*
        Delete old news
         */

        Vector news = alice.newsKP.getNews();
        Assert.assertTrue(news.contains("Durationtest"));
        try {
            TimeUnit.SECONDS.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        alice.newsKP.deleteold();
        news = alice.newsKP.getNews();
        Assert.assertFalse(news.contains("Durationtest"));

    }

}