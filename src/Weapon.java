import java.awt.*;

public class Weapon
{
    private String name;
    private int damage;
    private int fireRate;
    private int magSize;
    private int currentMag;
    private static final int MAX_AMMO = 999;
    private int totalAmmo;
    private boolean isUnlimitedAmmo;
    private boolean fired;
    private double fireCooldown = 0.0;
    private double flashLength = 0.5;
    private double flashTime = 0.0;
    private Image sprite;
    private Image[] muzzleFlash;
    private int flashFrame;
    private int flashFrames;
    private GameEngine.AudioClip fireSound;

    public Weapon(String name, int damage, int fireRate, int magSize, int initialTotalAmmo, boolean isUnlimitedAmmo,
                  Image sprite, GameEngine.AudioClip fireSound, Image fsprite, int fFrames)
    {
        this.name = name;
        this.damage = damage;
        this.fireRate = fireRate;
        this.magSize = magSize;
        this.isUnlimitedAmmo = isUnlimitedAmmo;
        if (isUnlimitedAmmo)
        {
            this.currentMag = magSize;
            this.totalAmmo = -1; // Inidicate it is unlimited
        }
        else
        {
            this.currentMag = magSize;
            this.totalAmmo = initialTotalAmmo;
        }
        this.sprite = sprite;
        this.flashFrames = fFrames;
        this.muzzleFlash = new Image[flashFrames];
        for (int i = 0; i < flashFrames; i ++) {
            muzzleFlash[i] = GameEngine.subImage(fsprite,  128  * i, 0, 128, 128);
        }
        this.fireSound = fireSound;
    }

    public void update(double dt)
    {
        if (fireCooldown > 0.0)
        {
            fireCooldown -= dt;
        }
        if (fired) {
            flashTime += dt;
            flashFrame = (int)Math.floor(flashTime / 0.1);
            if (flashFrame >= flashFrames) {
                flashFrame = 0;
                flashTime = 0;
                fired = false;
            }
        }
    }

    public boolean tryFire()
    {
        if (fireCooldown <= 0 && (isUnlimitedAmmo || currentMag > 0))
        {
            if (!isUnlimitedAmmo)
            {
                currentMag--;
            }
            fireCooldown = 1.0 / fireRate;
            fired = true;// Reset cooldown based on fire rate
            return true;
        }
        return false;
    }

    public void reload()
    {
        if (isUnlimitedAmmo)
        {
            return;
        }

        int ammoNeeded = magSize - currentMag;
        if (totalAmmo >= ammoNeeded)
        {
            currentMag += ammoNeeded;
            totalAmmo -= ammoNeeded;
        }
        else
        {
            currentMag += totalAmmo;
            totalAmmo = 0;
        }
    }

    public void addAmmo(int ammo)
    {
        if (!isUnlimitedAmmo)
        {
            totalAmmo = Math.min(totalAmmo + ammo, MAX_AMMO);
        }
    }

    public String getName()
    {
        return name;
    }
    public int getDamage()
    {
        return damage;
    }
    public void setFired(boolean f) {
        fired = f;
    }

    public Image getFlashFrame() {
        return  muzzleFlash[flashFrame];
    }

    public boolean getFired() {
        return fired;
    }

    public int getCurrentMagAmmo()
    {
        return currentMag;
    }
    public int getTotalAmmo()
    {
        return totalAmmo;
    }
    public boolean isUnlimitedAmmo()
    {
        return isUnlimitedAmmo;
    }
    public Image getSprite()
    {
        return sprite;
    }
    public GameEngine.AudioClip getFireSound()
    {
        return fireSound;
    }
}
