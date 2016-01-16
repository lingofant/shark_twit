import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import net.sharkfw.kep.SharkProtocolNotSupportedException;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.system.SharkException;
import net.sharkfw.system.SharkSecurityException;

import java.io.IOException;
import java.util.Vector;

public class Controller {

    @FXML
    public Label lable_info;

    @FXML
    public Button btd_send;

    @FXML
    public Button btd_delold;

    @FXML
    public Button btd_addnews;

    @FXML
    public ListView List_News;

    @FXML
    public TextField txtbox_newnews;

    @FXML
    public TextField txtbox_topicdir;

    @FXML
    public TextField txtbox_topicuri;

    @FXML
    public TextField txtbox_topic;

    @FXML
    public TextField txtbox_topicsuper;

    @FXML
    public ChoiceBox choice_identity;


    PeerSemanticTag other;
    NewsPeerTCP me;


    public void delete_old_news(ActionEvent actionEvent){
            try {
            me.newsKP.deleteold();
        } catch (SharkException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void refresh(ActionEvent actionEvent){
        List_News.getItems().clear();
        Vector news =  new Vector();
        try {
            news = me.newsKP.getNews();
        } catch (SharkKBException e) {
            e.printStackTrace();
        }
        for(int i = 0; i<news.size();i++){
            List_News.getItems().add(news.get(i));

        }

    }

    public void add_news(ActionEvent actionEvent) {
        try {
            me.newsKP.addNews(txtbox_newnews.getText(), me.newsKP.getTopicasSemanticTag(txtbox_topic.getText()));
        } catch (SharkException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createIdentity(ActionEvent actionEvent) throws SharkProtocolNotSupportedException, IOException {
        String identity = (String) choice_identity.getValue();

        if(identity.equals("Bob")){
            choice_identity.setDisable(true);

                me = new Bob("Bob",
                        "http://www.sharksystem.net/bob.html",
                        "tcp://localhost:7071",
                        7071
                );


            other = InMemoSharkKB.createInMemoPeerSemanticTag("Alice",
                    "http://www.sharksystem.net/alice.html",
                    "tcp://localhost:7070");
        }
        else{
            choice_identity.setDisable(true);
             me = new Alice("Alice",
                    "http://www.sharksystem.net/alice.html",
                    "tcp://localhost:7070",
                    7070
            );
            other = InMemoSharkKB.createInMemoPeerSemanticTag("Bob",
                    "http://www.sharksystem.net/bob.html",
                    "tcp://localhost:7071");

        }

    }

    public void add_topic(ActionEvent actionEvent) throws SharkKBException {

        if(txtbox_topicdir.getText().trim().isEmpty()){
            if(txtbox_topicsuper.getText().trim().isEmpty()){
                me.newsKP.addNewsTopic(txtbox_topic.getText(), txtbox_topicuri.getText());
            }
            else{

            }
        }
        if(txtbox_topicsuper.getText().trim().isEmpty()){

        }
        else{
            me.newsKP.addNewsTopic(txtbox_topic.getText(), txtbox_topicuri.getText(), me.newsKP.getTopicasSemanticTag(txtbox_topicsuper.getText()), Integer.parseInt(txtbox_topicdir.getText()));
        }
    }

    public void send_interest(ActionEvent actionEvent) {
        try {
            me.newsKP.sendInteresst(other);
        } catch (SharkKBException e) {
            e.printStackTrace();
        } catch (SharkSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send_allinterest(ActionEvent actionEvent)  {
        try {
            me.newsKP.sendAllIntersesst(other);
        } catch (SharkKBException e) {
            e.printStackTrace();
        } catch (SharkSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
