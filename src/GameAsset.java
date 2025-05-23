import java.awt.Image;

public class GameAsset {

    //declare variables for game assets
    private final int WALLPIXELSIZE = 128; //already pre determined by wall width
    private Image imageTestWall;
    private Image [] imageStripsTestWall;

    //constructor
    public GameAsset() {
        //initliase game asset variables
        this.imageTestWall = GameEngine.loadImage("assets/visual/TestWall.png");
        imageStripsTestWall = new Image[WALLPIXELSIZE];
        for (int i = 0; i < WALLPIXELSIZE; i++) {
            imageStripsTestWall[i] = GameEngine.subImage(imageTestWall, i, 0, 1, WALLPIXELSIZE);
        }

    }

    //get one strip of the wall texture based on the width value
    //the width value should be derived from local coords of where on a wall block a ray hits
    public Image getImageStripsTestWall(int x) {
        return imageStripsTestWall[x];
    }

    
    
}
