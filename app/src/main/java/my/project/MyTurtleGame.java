package my.project;

import engine.annot.Game;
import engine.core.CoreGame;
import engine.input.Input;
import java.awt.Color;
import my.project.utils.Image;

@Game(name = "Turtle Paint")
public class MyTurtleGame extends CoreGame {
    private int t = 0;
    // (getWidth() / 2) * -1
    // (getHeight() / 2) * -1
    public void drawImage(String path, int xi, int yi) {
        int startPosX = xi;
        int startPosY = yi;
        int origin = startPosX;
        Color[][] imageMap;
        try {
            imageMap = Image.loadPixelMap("/wood.png");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        int width = imageMap.length;
        int height = imageMap[0].length;
        penUp();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int drawX = origin + x * 10;
                int drawY = startPosY + y * 10;

                penUp();
                moveTo(drawX, drawY);

                penDown();
                color(imageMap[x][y]);
                drawSquare(10, true);
            }
        }
    }

    @Override
    public void start() {
        

    }

    @Override
    public void input(Input input) {

    }

    @Override
    public void draw() {
        t += 1;

        clear();
        drawImage("/wood.png", ((getWidth() / 2) * -1) + (t/10), (getHeight() / 2) * -1);
    }
}