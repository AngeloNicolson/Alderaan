import java.awt.*;

public class Weapon {
    private String name;
    private int damage;
    private int fireRate;
    private int magSize;
    private int currentMag;
    private static final int MAX_AMMO = 999;
    private int totalAmmo;
    private boolean isUnlimitedAmmo;
    private double fireCooldown = 0.0;
    private Image sprite;
    private GameEngine.AudioClip fireSound;

    public Weapon(String name, int damage, int fireRate, int magSize, int initialTotalAmmo,
                  boolean isUnlimitedAmmo, Image sprite, GameEngine.AudioClip fireSound) {
        this.name = name;
        this.damage = damage;
        this.fireRate = fireRate;
        this.magSize = magSize;
        this.isUnlimitedAmmo = isUnlimitedAmmo;
        if(isUnlimitedAmmo){
            this.currentMag = magSize;
            this.totalAmmo = -1; //Inidicate it is unlimited
        }else{
            this.currentMag = magSize;
            this.totalAmmo = initialTotalAmmo;
        }
        this.sprite = sprite;
        this.fireSound = fireSound;
    }

    public void update(double dt){
        if(fireCooldown > 0.0){
            fireCooldown -= dt;
        }
    }

    public boolean tryFire() {
        if (fireCooldown <= 0 && (isUnlimitedAmmo || currentMag > 0)) {
            if (!isUnlimitedAmmo) {
                currentMag--;
            }
            fireCooldown = 1.0 / fireRate; // Reset cooldown based on fire rate
            return true;
        }
        return false;
    }

    public void reload(){
        if(isUnlimitedAmmo){
            return;
        }

        int ammoNeeded = magSize - currentMag;
        if(totalAmmo >= ammoNeeded){
            currentMag += ammoNeeded;
            totalAmmo -= ammoNeeded;
        }else{
            currentMag += totalAmmo;
            totalAmmo = 0;
        }
    }


    public void addAmmo(int ammo){
        if (!isUnlimitedAmmo) {
            totalAmmo = Math.min(totalAmmo + ammo, MAX_AMMO);
        }
    }



    public String getName() {return name;}
    public int getDamage() {return damage;}
    public int getCurrentMagAmmo(){return currentMag;}
    public int getTotalAmmo(){return totalAmmo;}
    public boolean isUnlimitedAmmo() {return isUnlimitedAmmo;}
    public Image getSprite() { return sprite; }
    public GameEngine.AudioClip getFireSound() { return fireSound; }

}
