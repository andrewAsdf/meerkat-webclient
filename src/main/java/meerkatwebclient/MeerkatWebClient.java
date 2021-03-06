package meerkatwebclient;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.biotools.meerkat.Action;
import com.biotools.meerkat.Card;
import com.biotools.meerkat.GameInfo;
import com.biotools.meerkat.Hand;
import com.biotools.meerkat.PlayerInfo;
import com.biotools.meerkat.util.Preferences;


public class MeerkatWebClient implements com.biotools.meerkat.Player {
    private GameInfo gameInfo;
    private Preferences prefs;
    private MeerkatWebServerConnection server;

    private int seat;


    public void init(Preferences playerPrefs) { //this is called by PokerAcademy
        this.prefs = playerPrefs;
        String serverAddress = prefs.getPreference("SERVER_ADDRESS", "http://localhost:9000/");
        server = new MeerkatWebServerConnection(serverAddress);
    }


    public JPanel getSettingsPanel() {
        JPanel panel = new JPanel();

        panel.add(new JLabel("Bot server address:"));

        String serverAddress = prefs.getPreference("SERVER_ADDRESS", "http://localhost:9000/");
        final JTextField addressTextField = new JTextField(serverAddress);

        addressTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                prefs.setPreference("SERVER_ADDRESS", addressTextField.getText());
                server = new MeerkatWebServerConnection(addressTextField.getText());
            }
        });

        panel.add(addressTextField);

        return panel;
    }


    public void holeCards(Card c1, Card c2, int seat) {
        server.sendHoleCards(c1.toString(), c2.toString(), seat);
        this.seat = seat;
    }


    public Action getAction() {
        String actionString = server.getActionTypeString();
        double raiseAmount = gameInfo.getMinRaise ();

        if(actionString.equals ("raise") && !gameInfo.isFixedLimit()) {
            double parsedAmount = Integer.parseInt(server.getRaiseAmountString());
            if (parsedAmount > gameInfo.getMinRaise ()) {
                raiseAmount = parsedAmount;
            }//PokerAcademy doesn't check whether raise amount is valid
        }

        double toCall = gameInfo.getAmountToCall(seat);

        return ActionConverter.stringToAction(actionString, toCall, raiseAmount);
    }


    public void actionEvent(int pos, Action action) {
        double amount = action.getAmount();

        String actionString = ActionConverter.toString(action);

        if (!actionString.equals("invalid")) {
            server.sendActionEvent (pos, actionString, amount);
        }
    }


    public void gameOverEvent() {
        server.sendGameOver();
    }


    public void gameStartEvent(GameInfo gameInfo) {
        this.gameInfo = gameInfo;
        ArrayList<MeerkatWebServerConnection.Player> players
            = new ArrayList<MeerkatWebServerConnection.Player>();

        for(int i = 0; i < 10; i++) {

            if(!gameInfo.inGame(i)) {
                continue;
            }

            PlayerInfo player  = gameInfo.getPlayer(i);

            String playerName  = player.getName();
            double playerStack = player.getBankRollAtStartOfHand();

            players.add(new MeerkatWebServerConnection.Player(playerName, playerStack, i));
        }

        double bigBlind = gameInfo.getBigBlindSize();

        server.sendNewGame(players, gameInfo.getButtonSeat(), bigBlind);
    }


    public void showdownEvent(int seat, Card c1, Card c2) {
        server.addShowdownEvent (c1.toString(), c2.toString(), seat);
    }


    private String stageNumberToString (int stageNumber) {
        switch (stageNumber) {
            case 1: return "flop";
            case 2: return "turn";
            case 3: return "river";
            default: return "invalid";
        }
    }

    
    public void stageEvent(int stage) {
        Hand board = gameInfo.getBoard();

        if (board.size () > 0) {
            ArrayList<String> boardCards = new ArrayList<String>();

            for(String card : board.toString().split(" ")) {
                boardCards.add(card);
            }

            String stageString = stageNumberToString(stage);

            server.sendBoardCards(stageString, boardCards);
        }
    }


    public void winEvent(int pos, double amount, String handName) {
        server.addWinEvent (pos, amount);
    }


    public void dealHoleCardsEvent() {

    }


    public void gameStateChanged() {

    }
}
