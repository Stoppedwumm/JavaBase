package engine.core;

import engine.input.Input;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

public abstract class CoreGame extends Canvas {

    private boolean isRunning = false;
    private JFrame window;
    private BufferedImage backBuffer;   
    private Graphics2D pen;             
    private Input inputSystem;

    // Turtle State
    private double turtleX = 0;
    private double turtleY = 0;
    private boolean isPenDown = true;

    // --- Abstract Methods ---
    public abstract void start();
    public abstract void draw();
    public abstract void input(Input input);

    // --- Engine Initialization ---
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
        pen.setStroke(new BasicStroke(3)); // Thick lines
        
        // Initial Clear
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
    public int getScreenWidth() { return 800; }
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
    protected void penDown() { isPenDown = true; }
    protected void penUp() { isPenDown = false; }
    protected void color(Color c) { pen.setColor(c); }

    protected void moveTo(double x, double y) {
        if (isPenDown) {
            pen.drawLine(toScreenX(turtleX), toScreenY(turtleY), toScreenX(x), toScreenY(y));
        }
        turtleX = x;
        turtleY = y;
    }

    protected void move(double dx, double dy) {
        moveTo(turtleX + dx, turtleY + dy);
    }
    
    /**
     * Draws a square centered at the current turtle position.
     * @param size The width and height of the square.
     * @param filled If true, fills the square with the current color. If false, draws the outline.
     */
    protected void drawSquare(double size, boolean filled) {
        int s = (int) size;
        // Calculate the top-left corner so the square is centered on the turtle
        int x = toScreenX(turtleX) - (s / 2);
        int y = toScreenY(turtleY) - (s / 2);

        if (filled) {
            pen.fillRect(x, y, s, s);
        } else {
            pen.drawRect(x, y, s, s);
        }
    }

    protected void clear() {
        Color old = pen.getColor();
        pen.setColor(Color.BLACK);
        pen.fillRect(0, 0, 800, 600);
        pen.setColor(old);
    }
    
    // --- Flood Fill (Paint Bucket) ---
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