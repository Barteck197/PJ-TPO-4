import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JOptionPane;

import javax.naming.InitialContext;

import javax.jms.Topic;
import javax.jms.Session;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.TopicSession;
import javax.jms.JMSException;
import javax.jms.TopicPublisher;
import javax.jms.MessageListener;
import javax.jms.TopicSubscriber;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;

public class Chat {
    String _block = "";
    TopicSubscriber subscriber;

    JFrame frame = new JFrame("Chat");
    JButton send = new JButton("Send");
    JMenuBar menu = new JMenuBar();
    JMenuItem block = new JMenuItem("Block...");
    JMenuItem clear = new JMenuItem("Clear");
    JTextArea incoming = new JTextArea();
    JTextArea outgoing = new JTextArea();

    public static void main(String[] args) throws Exception {
        Chat chat = new Chat();
    }

    public Chat() throws Exception {
        InitialContext ctx = new InitialContext();
        TopicConnectionFactory factory = (TopicConnectionFactory)
                ctx.lookup("topic/connectionFactory");
        final Topic topic = (Topic) ctx.lookup("topic/topic0");
        TopicConnection conn = factory.createTopicConnection();
        final TopicSession session = conn.createTopicSession(false,
                Session.AUTO_ACKNOWLEDGE);
        final TopicPublisher publisher = session.createPublisher(topic);
        subscriber = session.createSubscriber(topic);

        incoming.setEditable(false);

        menu.add(block);
        menu.add(clear);

        block.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                String user = (String) JOptionPane.showInputDialog(frame,
                        "Block user:", "Select user name",
                        JOptionPane.QUESTION_MESSAGE, null, null, _block);
                if (user == null || _block.equals(user)) return;
                _block = user;
                try {
                    subscriber.close();
                    String sel = "sender <> '" + _block + "'";
                    subscriber = session.createSubscriber(topic, sel, false);
                    setMessageListener();
                    incoming.append("blocking " + _block);
                } catch (JMSException ex) {
                    ex.printStackTrace();
                }
            }
        });

        clear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                incoming.setText("");
            }
        });

        setMessageListener();

        conn.start();

        send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String msg = outgoing.getText();
                if (msg.length() > 0) {
                    try {
                        String sender = System.getProperty("user.name");
                        TextMessage m = session.createTextMessage(msg);
                        m.setStringProperty("sender", sender);
                        publisher.publish(m);
                        outgoing.setText("");
                    } catch (JMSException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(outgoing);
        panel.add(send, BorderLayout.EAST);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                incoming, panel);
        split.setOneTouchExpandable(true);
        split.setDividerLocation(205);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(split);
        frame.getContentPane().add(menu, BorderLayout.NORTH);
        frame.setSize(new Dimension(400, 300));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public void setMessageListener() throws JMSException {
        subscriber.setMessageListener(new MessageListener() {
            public void onMessage(Message m) {
                try {
                    TextMessage msg = (TextMessage) m;
                    String sender = msg.getStringProperty("sender");
                    incoming.append(sender + "> " + msg.getText() + "\n");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}