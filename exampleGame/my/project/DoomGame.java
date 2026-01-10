package my.project;

import engine.annot.Game;
import engine.core.CoreGame;
import engine.input.Input;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

@Game(name = "Turtle Doom")
public class DoomGame extends CoreGame {

    // --- Constants ---
    private final int RES_SCALE = 4; // Higher = Blockier/Faster (4 = 200 rays)
    private int RENDER_W;
    private int RENDER_H;
    
    // --- State ---
    private enum State { LOADING, PLAYING }
    private State state = State.LOADING;
    private String loadingStatus = "Initializing...";

    // --- Assets (Fetched via HTTP) ---
    private BufferedImage gunSprite;
    private Color[][] gunPixelMap;
    private int gunW, gunH;
    private boolean assetsLoaded = false;

    // --- World Map (Accessed as [y][x] / [row][col]) ---
    private final int[][] worldMap = {
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        {1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,1,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,1,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,1},
        {1,0,1,0,0,0,0,0,0,1,1,1,1,0,0,0,0,0,0,1},
        {1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
    };

    // --- Player / Camera ---
    private double posX = 3.5, posY = 3.5;  // Player Position
    private double dirX = -1, dirY = 0;     // Direction Vector
    private double planeX = 0, planeY = 0.66; // Camera Plane (FOV)
    
    // Input Handling
    private boolean left, right, forward, back;

    @Override
    public void start() {
        RENDER_W = getScreenWidth() / RES_SCALE;
        RENDER_H = getScreenHeight();

        // Start Background Asset Loader
        new Thread(this::loadAssets).start();
    }

    private void loadAssets() {
        try {
            loadingStatus = "Connecting to HTTP Source...";
            
            // Public domain sprite URL
            URL gunUrl = new URL("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/master-ball.png"); 
            
            BufferedImage rawImg = ImageIO.read(gunUrl);
            
            // Resize for the game HUD
            gunW = 128;
            gunH = 128;
            gunSprite = new BufferedImage(gunW, gunH, BufferedImage.TYPE_INT_ARGB);
            Graphics g = gunSprite.getGraphics();
            g.drawImage(rawImg, 0, 0, gunW, gunH, null);
            g.dispose();

            // Convert to fast color array
            gunPixelMap = new Color[gunW][gunH];
            for(int x=0; x<gunW; x++){
                for(int y=0; y<gunH; y++){
                    int argb = gunSprite.getRGB(x, y);
                    if ((argb >> 24) != 0) { // Check Alpha
                        gunPixelMap[x][y] = new Color(argb);
                    } else {
                        gunPixelMap[x][y] = null; // Transparent
                    }
                }
            }

            loadingStatus = "Assets Downloaded.";
            Thread.sleep(500); 
            assetsLoaded = true;
            state = State.PLAYING;

        } catch (Exception e) {
            e.printStackTrace();
            loadingStatus = "Download Failed. Using Procedural Assets.";
            generateProceduralGun();
            assetsLoaded = true;
            state = State.PLAYING;
        }
    }

    private void generateProceduralGun() {
        gunW = 60; gunH = 100;
        gunPixelMap = new Color[gunW][gunH];
        for(int x=0; x<gunW; x++) {
            for(int y=0; y<gunH; y++) {
                gunPixelMap[x][y] = Color.DARK_GRAY;
            }
        }
    }

    @Override
    public void input(Input input) {
        if (state != State.PLAYING) return;

        double moveSpeed = 0.08;
        double rotSpeed = 0.05;

        // Rotation
        if (input.isLeft()) {
            double oldDirX = dirX;
            dirX = dirX * Math.cos(rotSpeed) - dirY * Math.sin(rotSpeed);
            dirY = oldDirX * Math.sin(rotSpeed) + dirY * Math.cos(rotSpeed);
            double oldPlaneX = planeX;
            planeX = planeX * Math.cos(rotSpeed) - planeY * Math.sin(rotSpeed);
            planeY = oldPlaneX * Math.sin(rotSpeed) + planeY * Math.cos(rotSpeed);
        }
        if (input.isRight()) {
            double oldDirX = dirX;
            dirX = dirX * Math.cos(-rotSpeed) - dirY * Math.sin(-rotSpeed);
            dirY = oldDirX * Math.sin(-rotSpeed) + dirY * Math.cos(-rotSpeed);
            double oldPlaneX = planeX;
            planeX = planeX * Math.cos(-rotSpeed) - planeY * Math.sin(-rotSpeed);
            planeY = oldPlaneX * Math.sin(-rotSpeed) + planeY * Math.cos(-rotSpeed);
        }

        // Movement with Collision Detection (Fixed indices [y][x])
        if (input.isUp() || input.isKey(87)) { // W
            int nextX = (int)(posX + dirX * moveSpeed);
            int nextY = (int)(posY + dirY * moveSpeed);
            if(worldMap[(int)posY][nextX] == 0) posX += dirX * moveSpeed;
            if(worldMap[nextY][(int)posX] == 0) posY += dirY * moveSpeed;
        }
        if (input.isDown() || input.isKey(83)) { // S
            int nextX = (int)(posX - dirX * moveSpeed);
            int nextY = (int)(posY - dirY * moveSpeed);
            if(worldMap[(int)posY][nextX] == 0) posX -= dirX * moveSpeed;
            if(worldMap[nextY][(int)posX] == 0) posY -= dirY * moveSpeed;
        }
    }

    @Override
    public void draw() {
        clear();

        if (state == State.LOADING) {
            drawLoadingScreen();
            return;
        }

        // 1. Raycasting
        for (int x = 0; x < RENDER_W; x++) {
            
            double cameraX = 2 * x / (double)RENDER_W - 1; 
            double rayDirX = dirX + planeX * cameraX;
            double rayDirY = dirY + planeY * cameraX;

            int mapX = (int) posX;
            int mapY = (int) posY;

            double sideDistX;
            double sideDistY;

            double deltaDistX = (rayDirX == 0) ? 1e30 : Math.abs(1 / rayDirX);
            double deltaDistY = (rayDirY == 0) ? 1e30 : Math.abs(1 / rayDirY);

            double perpWallDist;
            int stepX;
            int stepY;
            int hit = 0; 
            int side = 0; 

            if (rayDirX < 0) {
                stepX = -1;
                sideDistX = (posX - mapX) * deltaDistX;
            } else {
                stepX = 1;
                sideDistX = (mapX + 1.0 - posX) * deltaDistX;
            }
            if (rayDirY < 0) {
                stepY = -1;
                sideDistY = (posY - mapY) * deltaDistY;
            } else {
                stepY = 1;
                sideDistY = (mapY + 1.0 - posY) * deltaDistY;
            }

            // DDA
            while (hit == 0) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = 0;
                } else {
                    sideDistY += deltaDistY;
                    mapY += stepY;
                    side = 1;
                }
                
                // Bounds Check & Wall Check (Fixed indices [y][x])
                if (mapX < 0 || mapX >= 20 || mapY < 0 || mapY >= 14) {
                    hit = 1;
                } else {
                    if (worldMap[mapY][mapX] > 0) hit = 1;
                }
            }

            if (side == 0) perpWallDist = (sideDistX - deltaDistX);
            else           perpWallDist = (sideDistY - deltaDistY);

            int lineHeight = (int) (RENDER_H / perpWallDist);

            int drawStart = -lineHeight / 2 + RENDER_H / 2;
            if (drawStart < 0) drawStart = 0;
            int drawEnd = lineHeight / 2 + RENDER_H / 2;
            if (drawEnd >= RENDER_H) drawEnd = RENDER_H - 1;

            // Draw Wall
            Color wallColor;
            if (side == 1) wallColor = new Color(150, 0, 0); 
            else           wallColor = new Color(255, 0, 0); 

            int brightness = (int)(255 - (perpWallDist * 20));
            if (brightness < 30) brightness = 30;
            if (brightness > 255) brightness = 255;
            
            if (side == 1) wallColor = new Color(brightness/2, 0, 0);
            else           wallColor = new Color(brightness, 0, 0);

            int screenX = x * RES_SCALE;
            
            // Draw Vertical Strip
            for(int w=0; w<RES_SCALE; w++) {
                penUp();
                moveTo((screenX + w) - 400, (drawStart - 300));
                penDown();
                color(wallColor);
                moveTo((screenX + w) - 400, (drawEnd - 300));
            }
            
            // Floor
            penUp();
            moveTo((screenX) - 400, (drawEnd - 300));
            penDown();
            color(Color.DARK_GRAY);
            moveTo((screenX) - 400, 300);
        }

        drawGun();
        drawMinimap();
    }

    private void drawLoadingScreen() {
        drawText("TURTLE DOOM", -150, -50, 5, Color.RED);
        drawText(loadingStatus.toUpperCase(), -200, 50, 3, Color.WHITE);
        
        penUp(); moveTo(0, 0); penDown(); color(Color.WHITE);
        drawSquare(20 + (System.currentTimeMillis() % 10), false);
    }

    private void drawGun() {
        if (!assetsLoaded || gunPixelMap == null) return;
        
        int bobX = (int)(Math.sin(System.currentTimeMillis() / 200.0) * 10);
        int bobY = (int)(Math.abs(Math.sin(System.currentTimeMillis() / 200.0)) * 10);
        
        // Check input directly for bobbing state
        // (Simplified check, assumes movement if any keys pressed)
        // Ideally pass "isMoving" flag from input
        
        int startX = -gunW / 2 + bobX;
        int startY = 300 - gunH - 10 + bobY;

        for(int x=0; x<gunW; x+=2) { 
            for(int y=0; y<gunH; y+=2) {
                Color c = gunPixelMap[x][y];
                if (c != null) {
                    penUp();
                    moveTo(startX + x, startY + y);
                    penDown();
                    color(c);
                    drawSquare(3, true);
                }
            }
        }
    }

    private void drawMinimap() {
        int mapScale = 5;
        int offX = -380;
        int offY = -280;

        // Draw Walls (Fixed indices [y][x])
        for(int x=0; x<20; x++) {
            for(int y=0; y<14; y++) {
                if (worldMap[y][x] == 1) { // Swapped indices here
                    penUp();
                    moveTo(offX + x*mapScale, offY + y*mapScale);
                    penDown();
                    color(Color.GRAY);
                    drawSquare(mapScale, true);
                }
            }
        }

        // Draw Player
        penUp();
        moveTo(offX + posX*mapScale, offY + posY*mapScale);
        penDown();
        color(Color.GREEN);
        drawSquare(3, true);
    }

    public void drawText(String text, int x, int y, int scale, Color color) {
        if (text == null) return;
        text = text.toUpperCase();
        int cursorX = x;
        int spacing = scale;
        for (char c : text.toCharArray()) {
            boolean[][] map = getBitmap(c);
            for (int r = 0; r < map.length; r++) {
                for (int cIdx = 0; cIdx < map[0].length; cIdx++) {
                    if (map[r][cIdx]) {
                        penUp(); moveTo(cursorX + cIdx*scale, y + r*scale);
                        penDown(); this.color(color); drawSquare(scale, true);
                    }
                }
            }
            cursorX += (map[0].length * scale) + spacing;
        }
    }
    
    private boolean[][] getBitmap(char c) {
        boolean T=true, F=false;
        switch(c) {
            case 'T': return new boolean[][]{{T,T,T},{F,T,F},{F,T,F},{F,T,F},{F,T,F}};
            case 'U': return new boolean[][]{{T,F,T},{T,F,T},{T,F,T},{T,F,T},{T,T,T}};
            case 'R': return new boolean[][]{{T,T,T},{T,F,T},{T,T,T},{T,F,T},{T,F,T}};
            case 'L': return new boolean[][]{{T,F,F},{T,F,F},{T,F,F},{T,F,F},{T,T,T}};
            case 'E': return new boolean[][]{{T,T,T},{T,F,F},{T,T,T},{T,F,F},{T,T,T}};
            case 'D': return new boolean[][]{{T,T,F},{T,F,T},{T,F,T},{T,F,T},{T,T,F}};
            case 'O': return new boolean[][]{{F,T,F},{T,F,T},{T,F,T},{T,F,T},{F,T,F}};
            case 'M': return new boolean[][]{{T,F,T},{T,T,T},{T,F,T},{T,F,T},{T,F,T}};
            case ' ': return new boolean[][]{{F,F,F},{F,F,F},{F,F,F},{F,F,F},{F,F,F}};
            case '.': return new boolean[][]{{F},{F},{F},{F},{T}};
            default: return new boolean[][]{{T,T,T},{T,F,T},{T,F,T},{T,F,T},{T,T,T}};
        }
    }
}