package org.ThreeDotsSierpinski;

import javax.swing.*;

public class App {
    public static final String DOT_MOVER = "Dot Mover";
    public static final String DOT_MOVER_DOTS = "Dot Mover - Dots: ";
    public static final int DELAY = 1;
    public static final String RANDOM_VALUE_STRING = " (Random Value: ";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(DOT_MOVER); // Window title "Dot Mover"
            DiceRoller diceRoller = new DiceRoller();
            DotMover dotMover = new DotMover(diceRoller);
            frame.add(dotMover);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Установка окна в полноэкранный режим
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

            Timer timer = new Timer(DELAY, e -> {
                dotMover.moveDot();
                int randomValue = diceRoller.rollDice(); // Get a random number value from DiceRoller
                frame.setTitle(DOT_MOVER_DOTS + dotMover.getDotCounter() + RANDOM_VALUE_STRING + randomValue + ")"); // Update the window title with the dot counter readings and the value of the random number
            });
            timer.start();

            frame.setVisible(true);
        });
    }

}
