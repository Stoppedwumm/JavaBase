package engine.input;

import java.awt.event.KeyEvent;

public class Input {
    private final boolean[] keys = new boolean[1024];
    private int mouseX, mouseY;
    private boolean mouseClicked;

    public void setKey(int keyCode, boolean pressed) {
        if (keyCode >= 0 && keyCode < keys.length) {
            keys[keyCode] = pressed;
        }
    }

    public boolean isKey(int keyCode) {
        return keys[keyCode];
    }
    
    // Helpers for common keys
    public boolean isUp() { return keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W]; }
    public boolean isDown() { return keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S]; }
    public boolean isLeft() { return keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A]; }
    public boolean isRight() { return keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D]; }
    public boolean isSpace() { return keys[KeyEvent.VK_SPACE]; }

    public void setMouse(int x, int y) { this.mouseX = x; this.mouseY = y; }
    public int getMouseX() { return mouseX; }
    public int getMouseY() { return mouseY; }
}