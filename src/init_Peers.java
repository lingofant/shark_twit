import net.sharkfw.kep.SharkProtocolNotSupportedException;

import java.io.IOException;

/**
 * Created by timol on 21.01.2016.
 */
public class init_Peers {

    Alice alice;
    Bob bob;

    private static init_Peers ourInstance = new init_Peers();

    public static init_Peers getInstance() {
        return ourInstance;
    }

    private init_Peers() {
        try {
            alice = new Alice("Alice",
                    "http://www.sharksystem.net/alice.html",
                    "tcp://localhost:7070",
                    7070
            );


            bob = new Bob("Bob",
                    "http://www.sharksystem.net/bob.html",
                    "tcp://localhost:7071",
                    7071
            );

        } catch (SharkProtocolNotSupportedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bob getBob(){
        return bob;
    }

    public Alice getAlice(){
        return alice;
    }

}
