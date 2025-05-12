/*
* RayCasterClean.java
*
* Joshua Sim : 22004867
*/

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class RayCasterClean extends GameEngine {

    // -------------------------------------------
    //PSVM Main goes here
    // -------------------------------------------
    public static void main(String[] args) {
        //simply creates and runs the instance of a SnakeGame
        createGame(new RayCasterClean(), 60);
    }

    // -------------------------------------------
    // Declare and Initialise all inner classes
    // -------------------------------------------

    //class for a PlayerObject
    public class PlayerObject  {
        //declare fields for object
        double mRadius; //radius of object
        double posX, posY; //coordinate position of the object
        double speed; //speed of the object
        double angle; //angle of the object

        //declare fields for ray
        private final int numOfRays = (int) tileSquareSize; //max number of rays, if tile size is 64, that's the number of rays
        int ray, depthOfField; //ray is the index of the ray
        double rayAngleNow; //ray angle currently
        //there are two intermediate rays, horizontal checking rays and vertical checking rays
        double rayHX, rayHY, offsetHX, offsetHY, distH; //horizontal checking ray variables
        double rayVX, rayVY, offsetVX, offsetVY, distV; //vertical checking ray variables
        double rayX[] = new double[numOfRays]; //array of final ray's X and Y end coordinates
        double rayY[] = new double[numOfRays];
        double distT[] = new double[numOfRays]; //array of final ray's length
        double rayAngle[] = new double[numOfRays]; //array of final ray's angle
        boolean surfaceLight[] = new boolean[numOfRays]; //indicator of north south, or east west facing
        //these could all be made in it's own object really.

        //constructor for object
        public PlayerObject(){
            this.init();
        }

        //init of object (if there is a need to reset the object)
        public void init(){

            //initialise object size
            mRadius = 4;

            //initalise coordinates
            posX = 96;
            posY = 416;

            //initialise angle
            angle = 0;

            //initialise object speed and velocity
            speed = 64;

        }

        //update method of object
        public void update(double dt){

            //key controls
            if (keyLeft) {
                angle -= 5; //decrease the angle
                if (angle < 0) { //reset negative angles to within the 360 range
                    angle = 360;
                }
            }
            if (keyRight) {
                angle += 5; //increase the angle
                if (angle > 359.9999) { //reset over 360 angles to within the 360 range
                    angle = 0;
                }
            }
            if (keyUp) {
                //move the player object forward, the way it's facing
                posX += cos(angle) * speed * dt;
                posY += sin(angle) * speed * dt;
            }
            if (keyDown) {
                //move the player object backward, away from where it's facing
                posX -= cos(angle) * speed * dt;
                posY -= sin(angle) * speed * dt;
            }
            //note: 'origin' point is set facing dead right, like in a textbook

            //--Ray Checking--
            rayAngleNow = angle - (numOfRays/2); //set first ray angle to left of player angle
            for (ray = 0; ray < numOfRays; ray++){ //loop thru all rays to check

                //offset the negative angles back to 0 - 360 range
                if (rayAngleNow < 0) {
                    rayAngleNow += 360;
                }
                //offset the more than 360 angles back to 0 - 360 range
                if (rayAngleNow > 360) {
                    rayAngleNow -= 360;
                }
                //save angle
                rayAngle[ray] = rayAngleNow;

                //--Checking Horizontal Lines--
                distH = 100_000; //initalise the horizontal ray distance to a high number
                depthOfField=0; //set depth of field, i.e. number of times to check
                double aTan = -1 / tan(rayAngleNow); // get the arcTan of the ray angle
                //if ray looking up
                if(rayAngleNow > 180){
                    rayHY = ((int) (posY/tileSquareSize)) * tileSquareSize; //get the closest gridline above, use (int) type casting to cut off division remainder
                    rayHX = ((posY - rayHY) * aTan) + posX; //use Y distance and arcTan to get ray X, and then transmute it to player X position
                    offsetHY = -tileSquareSize; //offset of Y is just the negative tile length
                    offsetHX = -offsetHY*aTan; //offset of X is using Y distance and arcTan to get X
                }
                //if ray looking down
                if (rayAngleNow < 180) {
                    rayHY = (((int) (posY/tileSquareSize)) * tileSquareSize) + tileSquareSize; //get the closest gridline below, use (int) type casting to cut off division remainder, then add one more tile length
                    rayHX = ((posY - rayHY) * aTan) + posX; //use Y distance and arcTan to get ray X, and then transmute it to player X position
                    offsetHY = tileSquareSize; //offset of Y is just the tile length
                    offsetHX = -offsetHY*aTan; //offset of X is using Y distance and arcTan to get X
                }
                //if ray is looking straight left
                if (rayAngleNow == 180) {
                    rayHX = 0; //set the ray to left edge
                    rayHY = posY; //set to same Y as the player
                    depthOfField = mapRowY;
                }
                //if ray is looking straight right
                if (rayAngleNow == 0) {
                    rayHX = width(); //set the ray to right edge
                    rayHY = posY; //set to same Y as the player
                    depthOfField = mapRowY;
                }

                //collision check with wall
                while (depthOfField < mapRowY) {
                    boolean collisionCheck = false; //flag for collision check with wall
                    for (WallBlock wallBlock : wallBlocks) { //loop to check ray point with all walls
                        collisionCheck = collisionPoint(wallBlock.getPosX(), wallBlock.getPosY(), wallBlock.getWidth(), wallBlock.getHeight(), rayHX, rayHY); //collision check per wall
                        if (collisionCheck) { //if there is a collision...
                            break; //...break out of all wall loop
                        }
                    }
                    if (collisionCheck) { //if there is a collision...
                        //depthOfField = mapRowY;
                        break; //...break out of the checking loop
                    } else { //...else, add the offset to the coordinates, and the test again
                        rayHX += offsetHX;
                        rayHY += offsetHY;
                        depthOfField++; //add the depth of field check and test again
                    }
                }
                distH = distance(posX, posY, rayHX, rayHY); //set true distance of horizontal ray
                //finish checking horzontal rays

                //--Checking Vertical Lines--
                distV = 100_000; //initalise the vertical ray distance to a high number
                depthOfField=0; //set depth of field, i.e. number of times to check
                double nTan = -tan(rayAngleNow); // get the negative Tan of the ray angle
                //if ray looking left
                if(rayAngleNow > 90 && rayAngleNow < 270){
                    rayVX = ((int) (posX/tileSquareSize)) * tileSquareSize; //get the closest gridline left, use (int) type casting to cut off division remainder
                    rayVY = ((posX - rayVX) * nTan) + posY; //use X distance and nTan to get ray Y, and then transmute it to player Y position
                    offsetVX = -tileSquareSize; //offset of X is just the negative tile length
                    offsetVY = -offsetVX*nTan; //offset of Y is using X distance and nTan to get Y
                }
                //if ray looking right
                if (rayAngleNow < 90 || rayAngleNow > 270) {
                    rayVX = (((int) (posX/tileSquareSize)) * tileSquareSize) + tileSquareSize; //get the closest gridline left, use (int) type casting to cut off division remainder, then add one more tile length
                    rayVY = ((posX - rayVX) * nTan) + posY; //use X distance and nTan to get ray Y, and then transmute it to player Y position
                    offsetVX = tileSquareSize; //offset of X is just the tile length
                    offsetVY = -offsetVX*nTan; //offset of Y is using X distance and arcTan to get Y
                }
                //if ray is looking straight up
                if (rayAngleNow == 270) {
                    rayVX = posX; //set to same X as the player
                    rayVY = 0; //set to top edge
                    depthOfField = mapColX;
                }
                //if ray is looking straight down
                if (rayAngleNow == 90) {
                    rayVX = posX; //set to same X as the player
                    rayVY = height(); //set to bottom edge
                    depthOfField = mapColX;
                }
                //collision check with wall
                while (depthOfField < mapColX) {
                    boolean collisionCheck = false; //flag for collision check with wall
                    for (WallBlock wallBlock : wallBlocks) { //loop to check ray point with all walls
                        collisionCheck = collisionPoint(wallBlock.getPosX(), wallBlock.getPosY(), wallBlock.getWidth(), wallBlock.getHeight(), rayVX, rayVY); //collision check per wall
                        if (collisionCheck) { //if there is a collision...
                            break; //...break out of all wall loop
                        }
                    }
                    if (collisionCheck) { //if there is a collision...
                        //depthOfField = mapColX;
                        break; //...break out of the checking loop
                    } else { //...else, add the offset to the coordinates, and the test again
                        rayVX += offsetVX;
                        rayVY += offsetVY;
                        depthOfField++; //add the depth of field check and test again
                        //System.out.println("DEBUG: depthOfField: " + depthOfField);
                    }
                }
                //System.out.println("DEBUG: Angle: "+ angle + " depthOfField: " + depthOfField +" rayVX: "+ rayVX + " rayVY: "+ rayVY);
                distV = distance(posX, posY, rayVX, rayVY); //set true distance of vertical ray
                //finish checking vertical rays

                //check both rays and use the shortest for the final ray, plus input ray info
                if (distH < distV) {
                    rayX[ray] = rayHX;
                    rayY[ray] = rayHY;
                    distT[ray] = distH;
                    surfaceLight[ray] = false;
                } else if (distV < distH) {
                    rayX[ray] = rayVX;
                    rayY[ray] = rayVY;
                    distT[ray] = distV;
                    surfaceLight[ray] = true;
                }
                // increament ray angle for the next ray
                rayAngleNow++;

            } //finish looking thru all rays

        }

        //draw method for object
        public void draw(){
            //==Draw the player object==
            // Save the current transform
            saveCurrentTransform();
            // translate to the position of the object
            translate(posX, posY);
            // Rotate the drawing context around the angle of the object
            rotate(angle);
            //change the colour to white
            changeColor(white);
            // Draw the object
            //drawImage(imageCircular, -mRadius, -mRadius, mRadius*2, mRadius*2);
            drawSolidCircle(0, 0, mRadius);//draws a circle
            drawLine(0, 0, 8, 0);
            // Restore last transform to undo the rotate and translate transforms
            restoreLastTransform();

            //==Draw the rays==
            //initialise the vertical line height on the right side display
            double lineH = 320; //defaulted the max line height to be 320 at the moment
            //initialise the right side vertical line X coord as the left most point on the right side
            //(+4 because the vertical lines are set to 8 thickness for now)
            double lineX = width() / 2 + 4;

            //draw ray line
            for (ray = 0; ray < numOfRays; ray++) { //for each ray
                //change colour to red
                changeColor(red);
                //draw the ray from player to closest collided wall
                drawLine(posX, posY, rayX[ray], rayY[ray]);
                //optional: calculation to remove fisheye effect
                //this re-calcuates the rays not directly ahead of the player
                //it uses cosine to re-ratio the line as if it was directly in front of player
                distT[ray] = distT[ray] * cos(rayAngle[ray] - angle);
                //based on ray length, workout the length of vertical line on the rightside display
                lineH = (tileSquareSize * 320) / distT[ray];
                //limit the max height of the vertical lines on the right side
                if (lineH > 320) {
                    lineH = 320;
                }
                //change the colour of the surface based on whether it's facing NS facing wall or EW facing wall
                if (surfaceLight[ray]) { //if it's a north south facing wall...
                    changeColor(new Color(255,0,0)); //...make it s strong red
                } else { //else it's a east west facing wall...
                    changeColor(new Color(200, 0,0)); //...make it a pale red
                }
                //draw the line on the rightside display
                drawLine(lineX, height()/2 - lineH/2, lineX, height()/2 + lineH/2, 8);
                //move the line origin to the right for the next ray
                lineX +=8;
            }

        }

    }

    //class for a wall
    public class WallBlock  {
        //declare fields for object
        double mWidth, mHeight; //width and height of object
        double posX, posY; //coordinate position of the object

        //constructor for object
        public WallBlock(){
            this.init();
        }

        //init of object (if there is a need to reset the object)
        public void init(){

            //initalise coordinates
            posX = 0;
            posY = 0;

        }

        //update method of object
        public void update(double dt){

        }

        public double getWidth() {
            return mWidth;
        }

        public void setWidth(double mWidth) {
            this.mWidth = mWidth;
        }

        public double getHeight() {
            return mHeight;
        }

        public void setHeight(double mHeight) {
            this.mHeight = mHeight;
        }

        public double getPosX() {
            return posX;
        }

        public void setPosX(double posX) {
            this.posX = posX;
        }

        public double getPosY() {
            return posY;
        }

        public void setPosY(double posY) {
            this.posY = posY;
        }

        //draw method for object
        public void draw(){
            //change the colour to white
            changeColor(white);
            //draws a circle with colour above
            drawSolidRectangle(posX, posY, mWidth-1, mHeight-1);
        }

    }

    // -------------------------------------------
    // Declare all fields/variables
    // -------------------------------------------

    // declare fields for the game state
    private final int WINDOWWIDTH = 1024; //pre-set the width and height
    private final int WINDOWHEIGHT = 512;

    //map
    private int mapColX;
    private int mapRowY;
    private double tileSquareSize;
    private int[][] map01 =
            {
                    {1,1,1,1,1,1,1,1},
                    {1,0,1,0,0,0,0,1},
                    {1,0,1,0,0,0,0,1},
                    {1,0,1,0,0,0,0,1},
                    {1,0,0,0,0,0,0,1},
                    {1,0,0,0,0,1,0,1},
                    {1,0,0,0,0,0,0,1},
                    {1,1,1,1,1,1,1,1},
            }; //change this to change the map

    // declare objects
    private int numWallBlocks;
    private ArrayList<WallBlock> wallBlocks;
    private WallBlock sampleWallBlock;
    private PlayerObject playerObject;

    // declare boolean flags
    private boolean isGameOver;

    // declare fields for input
    private boolean keyUp, keyRight, keyDown, keyLeft; // directional key inputs

    // -------------------------------------------
    // initialise the game and the fields needed
    // -------------------------------------------

    // key method to initalise everything before the game starts proper
    @Override
    public void init() {
        // set the size of the game board
        setWindowSize(WINDOWWIDTH, WINDOWHEIGHT);

        // initialise map
        mapColX = 8;
        mapRowY = 8;
        tileSquareSize = 64;

        wallBlocks = new ArrayList<>();
        numWallBlocks = 0;

        // initialise objects
        //map objects
        for (int iY = 0; iY < mapRowY; iY++) {
            for (int jX = 0; jX < mapColX; jX++) {
                if (map01[iY][jX] == 1) {
                    sampleWallBlock = new WallBlock();
                    sampleWallBlock.setPosX(jX*tileSquareSize);
                    sampleWallBlock.setPosY(iY*tileSquareSize);
                    sampleWallBlock.setWidth(tileSquareSize);
                    sampleWallBlock.setHeight(tileSquareSize);
                    wallBlocks.add(sampleWallBlock);
                    numWallBlocks++;
                }
            }
        }
        //player object
        playerObject = new PlayerObject();

        // initialise the input fields:
        keyUp = false; //all the keys are not pressed to start with
        keyRight = false;
        keyDown = false;
        keyLeft = false;
    }

    // -------------------------------------------
    // update the game
    // -------------------------------------------

    // key method to update the game
    @Override
    public void update(double dt) {

        //if game is not over...
        if (!isGameOver) {
            //...then update game

            // update objects
            // for (WallBlock wallBlock : wallBlocks) {
            //     wallBlock.update(dt);
            // }
            playerObject.update(dt);


        }

    }

    // -------------------------------------------
    // draw the game
    // -------------------------------------------

    // key method to draw things in the window
    @Override
    public void paintComponent() {
        // Clear the background to selected color
        changeBackgroundColor(black);
        clearBackground(width(), height());

        // draw objects
        for (WallBlock wallBlock : wallBlocks) {
            wallBlock.draw();
        }
        playerObject.draw();

    }

    // -------------------------------------------
    // supplementary methods
    // -------------------------------------------

    //basic retangle to point collision detection
    //Only works with axis aligned rects (no rotation)
    public boolean collisionPoint(double aX, double aY, double aWidth, double aHeight, double bX, double bY){
        return (bX >= aX //pointX is more than rect left side
                && bX <= aX + aWidth //pointX is less than rect right side
                && bY >= aY //pointY is more than rect top side
                && bY <= aY + aHeight); //pointY is less than rect bottom
        //If all are true, means point is inside the rectangle
    }



    // -------------------------------------------
    // user input functions
    // -------------------------------------------

    // -- Keyboard Inputs --

    // Method for when key is pressed
    public void keyPressed(KeyEvent e) {
        // The user pressed left arrow key...
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            // ...then record input
            keyLeft = true;
        }
        // The user pressed right arrow key
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            keyRight = true;
        }
        // The user pressed up arrow key
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            keyUp = true;
        }
        // The user pressed down arrow key
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            keyDown = true;
        }
    }

    // Method for when key is released
    public void keyReleased(KeyEvent e) {
        // The user releases left arrow key
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            //...then record lack of input
            keyLeft = false;
        }
        // The user releases right arrow key
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            keyRight = false;
        }
        // The user releases up arrow key
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            keyUp = false;
        }
        // The user releases down arrow key
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            keyDown = false;
        }
    }

}
