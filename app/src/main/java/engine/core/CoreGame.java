package engine.core;

import engine.input.Input;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;

public abstract class CoreGame extends Canvas {

    // ============================================================
    // WINDOW / LOOP
    // ============================================================
    private Frame window;
    private boolean running;

    private BufferStrategy bufferStrategy;
    protected Graphics2D pen;

    private final Input inputSystem = new Input();

    // ============================================================
    // TURTLE STATE
    // ============================================================
    private double turtleX, turtleY;
    private boolean penDown = true;

    // ============================================================
    // ABSTRACT API
    // ============================================================
    public abstract void start();
    public abstract void draw();
    public abstract void input(Input input);

    // ============================================================
    // ENGINE START
    // ============================================================
    public void startEngine() {

        // macOS safety flags
        System.setProperty("sun.awt.noerasebackground", "true");
        System.setProperty("apple.awt.inputMethodEnabled", "false");

        window = new Frame("Turtle Engine");
        window.setLayout(new BorderLayout());
        window.setResizable(false);
        window.setIgnoreRepaint(true);

        setPreferredSize(new Dimension(800, 600));
        setIgnoreRepaint(true);
        setFocusable(true);

        window.add(this, BorderLayout.CENTER);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        setupInputListeners();
        requestFocus();

        createBufferStrategy(3);
        bufferStrategy = getBufferStrategy();

        running = true;
        start();

        new Thread(this::gameLoop, "GameLoop").start();
    }

    // ============================================================
    // GAME LOOP
    // ============================================================
    private void gameLoop() {
        final double nsPerFrame = 1_000_000_000.0 / 60.0;
        long last = System.nanoTime();

        while (running) {
            long now = System.nanoTime();
            if (now - last >= nsPerFrame) {
                last = now;

                input(inputSystem);
                render();
            }

            try { Thread.sleep(1); } catch (InterruptedException ignored) {}
        }
    }

    // ============================================================
    // RENDER (SINGLE PATH, NO FLICKER)
    // ============================================================
    private void render() {
        do {
            do {
                pen = (Graphics2D) bufferStrategy.getDrawGraphics();
                try {
                    // Turn on Anti-Aliasing for smooth "Neon" lines
                    pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    pen.setColor(Color.BLACK);
                    pen.fillRect(0, 0, getWidth(), getHeight());
                    pen.setColor(Color.WHITE);

                    draw(); // GAME RENDERS HERE

                } finally {
                    pen.dispose();
                }
            } while (bufferStrategy.contentsRestored());

            bufferStrategy.show();

        } while (bufferStrategy.contentsLost());
    }

    // ============================================================
    // INPUT
    // ============================================================
    private void setupInputListeners() {
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) { inputSystem.setKey(e.getKeyCode(), true); }
            public void keyReleased(KeyEvent e) { inputSystem.setKey(e.getKeyCode(), false); }
        });

        MouseAdapter ma = new MouseAdapter() {
            public void mouseMoved(MouseEvent e) { inputSystem.setMouse(e.getX() - 400, e.getY() - 300); }
            public void mouseDragged(MouseEvent e) { mouseMoved(e); }
            public void mousePressed(MouseEvent e) { inputSystem.setKey(1000, true); }
            public void mouseReleased(MouseEvent e) { inputSystem.setKey(1000, false); }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    // ============================================================
    // TURTLE API & EXTENDED DRAWING
    // ============================================================
    protected void penDown() { penDown = true; }
    protected void penUp() { penDown = false; }
    protected void color(Color c) { pen.setColor(c); }

    protected void moveTo(double x, double y) {
        int sx = toScreenX(turtleX);
        int sy = toScreenY(turtleY);
        int ex = toScreenX(x);
        int ey = toScreenY(y);

        if (penDown) pen.drawLine(sx, sy, ex, ey);

        turtleX = x;
        turtleY = y;
    }

    protected void move(double dx, double dy) {
        moveTo(turtleX + dx, turtleY + dy);
    }

    /**
     * FIX: Added drawSquare method required by NeonDodger
     */
    protected void drawSquare(int size, boolean filled) {
        int x = toScreenX(turtleX) - size / 2;
        int y = toScreenY(turtleY) - size / 2;
        if (filled) {
            pen.fillRect(x, y, size, size);
        } else {
            pen.drawRect(x, y, size, size);
        }
    }

    /**
     * Helper for drawing scores and UI
     */
    protected void drawText(String text, double x, double y) {
        pen.drawString(text, toScreenX(x), toScreenY(y));
    }

    /**
     * Changes font size for the UI
     */
    protected void setFontSize(int size) {
        pen.setFont(new Font("Arial", Font.BOLD, size));
    }

    /**
     * Changes line thickness (important for Neon effect)
     */
    protected void setThickness(float width) {
        pen.setStroke(new BasicStroke(width));
    }

    protected void clear() {
        Color old = pen.getColor();
        pen.setColor(Color.BLACK);
        pen.fillRect(0, 0, getWidth(), getHeight());
        pen.setColor(old);
    }

    private int toScreenX(double x) { return (int) (x + getWidth() / 2); }
    private int toScreenY(double y) { return (int) (y + getHeight() / 2); }
}
