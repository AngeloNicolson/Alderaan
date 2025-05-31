import java.awt.Image;

public class GameAsset
{

    // declare variables for game assets
    private final int WALLPIXELSIZE = 128; // already pre determined by wall width
    private Image imageScifiWall;
    private Image[][] imageStripsScifiWall;
    private Image lazerPistol;
    private Image lazerRifle;
    private Image lazerRiflePickup;
    private Image lazerShotgun;
    private Image lazerShotgunPickup;
    private Image healthItemSprite;
    private Image escapePodSprite;

    // constructor
    public GameAsset()
    {
        // initliase game asset variables
        imageScifiWall = GameEngine.loadImage("assets/visual/ScifiWall.png");
        imageStripsScifiWall = new Image[5][WALLPIXELSIZE];
        for (int wallType = 0; wallType < 5; wallType++)
        {
            for (int x = 0; x < WALLPIXELSIZE; x++)
            {
                imageStripsScifiWall[wallType][x] =
                    GameEngine.subImage(imageScifiWall, wallType * WALLPIXELSIZE + x, 0, 1, WALLPIXELSIZE);
            }
        }
        lazerPistol = GameEngine.loadImage("assets/visual/LazerPistol.png");
        lazerRifle = GameEngine.loadImage("assets/visual/LazerRifle.png");
        lazerRiflePickup = GameEngine.loadImage("assets/visual/LazerRifleSide.png");
        lazerShotgunPickup = GameEngine.loadImage("assets/visual/LazerShotGunSide.png");
        lazerShotgun = GameEngine.loadImage("assets/visual/LazerShotGun.png");
        healthItemSprite = GameEngine.loadImage("assets/visual/HealthPack.png");
        escapePodSprite = GameEngine.loadImage("assets/visual/shell.png");
    }

    // get one strip of the wall texture based on the width value
    // the width value should be derived from local coords of where on a wall block a ray hits
    // also accounts for the wallType, which is 1 to 5
    public Image getImageStripsScifiWall(int wallType, int x)
    {
        wallType--; // map goes from 1 to 5, but walls are from 0 to 4 in array
        if (wallType > 4)
        {
            wallType = 1;
        }
        return imageStripsScifiWall[wallType][x];
    }

    public int getWALLPIXELSIZE()
    {
        return WALLPIXELSIZE;
    }

    public Image getLazerPistol()
    {
        return lazerPistol;
    }
    public Image getLazerRiflePickup()
    {
        return lazerRiflePickup;
    }
    public Image getLazerRifle()
    {
        return lazerRifle;
    }
    public Image getLazerShotgun()
    {
        return lazerShotgun;
    }
    public Image getLazerShotgunPickup()
    {
        return lazerShotgunPickup;
    }
    public Image getHealthItemSprite()
    {
        return healthItemSprite;
    }
    public Image getEscapePodSprite() {
        return escapePodSprite;
    }
}
