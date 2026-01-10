package engine.core;

import engine.input.Input;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Abstract base class for all games using the Turtle Engine.
 * 
 * Provides core functionality including:
 * <ul>
 *   <li>Window management and rendering</li>
 *   <li>Game loop at 60 FPS</li>
 *   <li>Input handling (keyboard and mouse)</li>
 *   <li>Turtle graphics commands (movement, drawing, filling)</li>
 * </ul>
 * 
 * Subclasses must implement the abstract methods: {@link #start()}, {@link #draw()}, and {@link #input(Input)}.
 */
public abstract class CoreGame extends Canvas {

    /** Flag indicating whether the game loop is currently running. */
    private boolean isRunning = false;
    
    /** The main game window frame. */
    private JFrame window;
    
    /** The back buffer used for double buffering to reduce flickering. */
    private BufferedImage backBuffer;   
    
    /** The Graphics2D pen used for drawing operations. */
    private Graphics2D pen;             
    
    /** The input system managing keyboard and mouse events. */
    private Input inputSystem;

    // Turtle State
    /** The current x-coordinate of the turtle. */
    private double turtleX = 0;
    
    /** The current y-coordinate of the turtle. */
    private double turtleY = 0;
    
    /** Flag indicating whether the pen is down (drawing) or up (not drawing). */
    private boolean isPenDown = true;

    /**
     * Creates a new CoreGame instance.
     * Subclasses should use this as their default constructor.
     */
    public CoreGame() { }

    // --- Abstract Methods ---
    /**
     * Called once when the game starts, before the game loop begins.
     * Use this to initialize game state.
     */
    public abstract void start();
    
    /**
     * Called every frame to render graphics.
     * Use turtle commands to draw on the canvas.
     */
    public abstract void draw();
    
    /**
     * Called every frame to handle user input.
     * 
     * @param input the input system providing keyboard and mouse state
     */
    public abstract void input(Input input);

    // --- Engine Initialization ---
    /**
     * Initializes and starts the game engine.
     * Sets up the window, input system, and game loop.
     * 
     * This method should be called from the game loader or main entry point.
     */
    public void startEngine() {
        System.setProperty("sun.java2d.opengl", "false"); 
        
        window = new JFrame("Turtle Engine");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.setLayout(new BorderLayout());
        
        this.setPreferredSize(new Dimension(800, 600));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.setIgnoreRepaint(true); 

        window.add(this, BorderLayout.CENTER);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        this.createBufferStrategy(2);

        backBuffer = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        pen = backBuffer.createGraphics();
        pen.setStroke(new BasicStroke(3));
        
        pen.setColor(Color.BLACK);
        pen.fillRect(0, 0, 800, 600);
        pen.setColor(Color.WHITE);

        inputSystem = new Input();
        setupInputListeners();

        isRunning = true;
        start();
        new Thread(this::gameLoop).start();
    }

    @Override
    public void paint(Graphics g) { }
    @Override
    public void update(Graphics g) { }

    private void gameLoop() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1000000000.0 / 60.0; 

        while (isRunning) {
            long now = System.nanoTime();
            if (now - lastTime >= nsPerTick) {
                lastTime = now;
                try {
                    input(inputSystem);
                    draw(); 
                    renderToScreen();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try { Thread.sleep(2); } catch (InterruptedException e) {}
        }
    }

    private void renderToScreen() {
        BufferStrategy bs = this.getBufferStrategy();
        if (bs == null) return;
        Graphics g = bs.getDrawGraphics();
        g.drawImage(backBuffer, 0, 0, 800, 600, null);
        g.dispose();
        bs.show(); 
        Toolkit.getDefaultToolkit().sync();
    }

    // --- Helpers ---
    /**
     * Gets the width of the screen in pixels.
     * 
     * @return the screen width (800 pixels)
     */
    public int getScreenWidth() { return 800; }
    
    /**
     * Gets the height of the screen in pixels.
     * 
     * @return the screen height (600 pixels)
     */
    public int getScreenHeight() { return 600; }
    
    private int toScreenX(double x) { return (int)(x + 400); }
    private int toScreenY(double y) { return (int)(y + 300); }

    private void setupInputListeners() {
        this.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) { inputSystem.setKey(e.getKeyCode(), true); }
            public void keyReleased(KeyEvent e) { inputSystem.setKey(e.getKeyCode(), false); }
        });
        MouseAdapter ma = new MouseAdapter() {
            public void mouseMoved(MouseEvent e) { inputSystem.setMouse(e.getX() - 400, e.getY() - 300); }
            public void mouseDragged(MouseEvent e) { 
                inputSystem.setMouse(e.getX() - 400, e.getY() - 300); 
            }
            public void mousePressed(MouseEvent e) { 
                requestFocusInWindow();
                // Map Mouse Click to Key Code 1000 (Arbitrary internal ID)
                inputSystem.setKey(1000, true); 
            }
            public void mouseReleased(MouseEvent e) { 
                inputSystem.setKey(1000, false); 
            }
        };
        this.addMouseMotionListener(ma);
        this.addMouseListener(ma);
    }

    // --- Turtle Commands ---
    /**
     * Puts the pen down so that movement draws lines.
     */
    protected void penDown() { isPenDown = true; }
    
    /**
     * Lifts the pen up so that movement does not draw lines.
     */
    protected void penUp() { isPenDown = false; }
    
    /**
     * Sets the current drawing color.
     * 
     * @param c the color to use for subsequent drawing operations
     */
    protected void color(Color c) { pen.setColor(c); }

    /**
     * Moves the turtle to an absolute position on the canvas.
     * If the pen is down, draws a line from the current position to the new position.
     * 
     * @param x the x-coordinate of the target position
     * @param y the y-coordinate of the target position
     */
    protected void moveTo(double x, double y) {
        if (isPenDown) {
            pen.drawLine(toScreenX(turtleX), toScreenY(turtleY), toScreenX(x), toScreenY(y));
        }
        turtleX = x;
        turtleY = y;
    }

    /**
     * Moves the turtle by a relative offset.
     * If the pen is down, draws a line from the current position to the new position.
     * 
     * @param dx the horizontal offset
     * @param dy the vertical offset
     */
    protected void move(double dx, double dy) {
        moveTo(turtleX + dx, turtleY + dy);
    }
    
    /**
     * Draws a square centered at the current turtle position.
     * 
     * @param size the width and height of the square
     * @param filled if true, fills the square with the current color; if false, draws the outline
     */
    protected void drawSquare(double size, boolean filled) {
        int s = (int) size;
        int x = toScreenX(turtleX) - (s / 2);
        int y = toScreenY(turtleY) - (s / 2);

        if (filled) {
            pen.fillRect(x, y, s, s);
        } else {
            pen.drawRect(x, y, s, s);
        }
    }

    /**
     * Clears the entire canvas by filling it with black.
     * The current drawing color is preserved.
     */
    protected void clear() {
        Color old = pen.getColor();
        pen.setColor(Color.BLACK);
        pen.fillRect(0, 0, 800, 600);
        pen.setColor(old);
    }
    
    /**
     * Performs a flood fill (paint bucket) operation starting from the turtle's current position.
     * Fills all connected pixels of the same color with the current pen color.
     */
    protected void fill() {
        int w = 800;
        int h = 600;
        int startX = toScreenX(turtleX);
        int startY = toScreenY(turtleY);

        if (startX < 0 || startX >= w || startY < 0 || startY >= h) return;

        int targetColor = backBuffer.getRGB(startX, startY);
        int replacementColor = pen.getColor().getRGB();

        if (targetColor == replacementColor) return;

        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(startX, startY));

        // Basic BFS Flood Fill
        while (!queue.isEmpty()) {
            Point p = queue.poll();
            int x = p.x; 
            int y = p.y;

            if (x < 0 || x >= w || y < 0 || y >= h) continue;

            if (backBuffer.getRGB(x, y) == targetColor) {
                backBuffer.setRGB(x, y, replacementColor);
                queue.add(new Point(x + 1, y));
                queue.add(new Point(x - 1, y));
                queue.add(new Point(x, y + 1));
                queue.add(new Point(x, y - 1));
            }
        }
    }
}
