# Project Alderaan: Design Document

## Story
You are aboard the Imperial Starship Alderaan. A plague has swept aboard the ship, turning the crew into mindless zombies. Everyone is infected, except you. You need to get to a life pod, and get off this ship… but the zombies have a different plan for you.

## Core Gameplay (Minimum)

### Pseudo-3D Environment
- Rendered in first-person view using raycasting
- Walls, ceiling, and floor are rendered to simulate an indoor spaceship environment.

### Programmable Maps
- Programmable map for devs to create levels
- Walls will have some form of sci-fi texture

### Player
- Has health
- Health is reduced when taking damage from enemies
- When the Player reaches zero health, this will trigger player Game Over
- Health is increased when walking into a health pack

### Player Weapon
- Starts with a laser pistol (unlimited ammo, no reloading).

### Player Controls
- Forwards, backwards, and strafing sideways, using WASD
- First person view moving left and right, using mouse movements
- Left mouse button fires the player’s weapon

### Enemies

- **Ranged Zombies (Stormtroopers)**
  - Zombies do not move
  - Zombies attempt to shoot player
  - Shots have travel time, allowing the player to dodge.

- **Melee Zombie**
  - Zombies walk towards the player
  - Zombies deal melee damage only to the player
  
- **Boss Zombie**
  - One per level, guarding the end zone
  - Higher Health (x times greater than regular zombies)

### Items
- Health Packs
  - Restores health by a portion of max health (e.g 1/4 x max health) when player walks into them

## Game States

- Main Menu:
    - Options: Start New Game, How to Play, Quit.

- Gameplay Screen:
    - Displays health (bottom left), score (bottom right), and weapon (bottom center).

- Game Over Screen:
    - Options: Start New Game, Main Menu.

- Next Level Screen:
    - Displays “Level Complete!” and transitions to the next level.

- Victory Screen:
    - Displays “You Escaped!” and final score.

- How to Play Screen:
    - Explains controls and objectives.


