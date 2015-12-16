import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import net.sharkfw.kep.SharkProtocolNotSupportedException;
import net.sharkfw.knowledgeBase.PeerSemanticTag;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.inmemory.InMemoSharkKB;
import net.sharkfw.system.SharkException;

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
    public ChoiceBox choice_identity;


    PeerSemanticTag other;
    NewsPeerTCP me;


    public void send_news(ActionEvent actionEvent) {
        try {
            me.newsKP.sendNews(other);
        } catch (SharkException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
            me.newsKP.addNews(txtbox_newnews.getText());
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
}
