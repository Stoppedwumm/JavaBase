package engine.input;

import java.awt.event.KeyEvent;

/**
 * Manages keyboard and mouse input state for the game.
 * 
 * Tracks which keys are currently pressed and the current mouse position.
 * Provides convenience methods for common directional and action keys.
 */
public class Input {
    /** Array tracking the pressed state of all keyboard keys (1024 possible key codes). */
    private final boolean[] keys = new boolean[1024];
    
    /** The current x-coordinate of the mouse cursor. */
    private int mouseX;
    
    /** The current y-coordinate of the mouse cursor. */
    private int mouseY;

    /**
     * Creates a new Input instance.
     */
    public Input() { }

    /**
     * Sets the state of a key.
     * 
     * @param keyCode the key code (from {@link KeyEvent})
     * @param pressed true if the key is pressed, false if released
     */
    public void setKey(int keyCode, boolean pressed) {
        if (keyCode >= 0 && keyCode < keys.length) {
            keys[keyCode] = pressed;
        }
    }

    /**
     * Checks if a specific key is currently pressed.
     * 
     * @param keyCode the key code (from {@link KeyEvent})
     * @return true if the key is pressed, false otherwise
     */
    public boolean isKey(int keyCode) {
        if (keyCode < 0 || keyCode >= keys.length) {
            return false;
        }
        return keys[keyCode];
    }
    
    /**
     * Checks if the up arrow or 'W' key is pressed.
     * 
     * @return true if either key is pressed
     */
    public boolean isUp() { return keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W]; }
    
    /**
     * Checks if the down arrow or 'S' key is pressed.
     * 
     * @return true if either key is pressed
     */
    public boolean isDown() { return keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S]; }
    
    /**
     * Checks if the left arrow or 'A' key is pressed.
     * 
     * @return true if either key is pressed
     */
    public boolean isLeft() { return keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A]; }
    
    /**
     * Checks if the right arrow or 'D' key is pressed.
     * 
     * @return true if either key is pressed
     */
    public boolean isRight() { return keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D]; }
    
    /**
     * Checks if the space bar is pressed.
     * 
     * @return true if the space bar is pressed
     */
    public boolean isSpace() { return keys[KeyEvent.VK_SPACE]; }

    /**
     * Sets the current mouse position.
     * Called internally by the input system when the mouse moves.
     * 
     * @param x the x-coordinate of the mouse
     * @param y the y-coordinate of the mouse
     */
    public void setMouse(int x, int y) { this.mouseX = x; this.mouseY = y; }
    
    /**
     * Gets the current x-coordinate of the mouse.
     * 
     * @return the mouse x-coordinate
     */
    public int getMouseX() { return mouseX; }
    
    /**
     * Gets the current y-coordinate of the mouse.
     * 
     * @return the mouse y-coordinate
     */
    public int getMouseY() { return mouseY; }
}
